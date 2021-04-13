package io.coodoo.workhorse.core.control;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Event;
import javax.enterprise.event.ObservesAsync;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.coodoo.workhorse.core.control.event.AllExecutionsDoneEvent;
import io.coodoo.workhorse.core.entity.Execution;
import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.persistence.interfaces.ExecutionPersistence;
import io.coodoo.workhorse.persistence.interfaces.JobPersistence;
import io.coodoo.workhorse.persistence.interfaces.qualifier.ExecutionQualifier;
import io.coodoo.workhorse.persistence.interfaces.qualifier.JobQualifier;

/**
 * Class that executes all executions of the a given job.
 * 
 * @author coodoo GmbH (coodoo.io)
 */
@Dependent
public class JobThread {

    @Inject
    BeanManager beanManager;

    @Inject
    @ExecutionQualifier
    ExecutionPersistence executionPersistence;

    @Inject
    @JobQualifier
    JobPersistence jobPersistence;

    @Inject
    WorkhorseController workhorseController;

    @Inject
    ExecutionBuffer executionBuffer;

    @Inject
    Event<AllExecutionsDoneEvent> allExecutionsDoneEvent;

    private Job job;
    private boolean stopMe;
    private Execution runningExecution;
    private Thread thread;
    private List<Execution> chainedExecutions = new ArrayList<>();
    private Long chainId = null;

    private static final Logger log = LoggerFactory.getLogger(JobThread.class);

    private BaseWorker getWorker(Job job) throws ClassNotFoundException {
        Set<Bean<?>> beans = beanManager.getBeans(BaseWorker.class, new AnnotationLiteral<Any>() {});
        for (Bean<?> bean : beans) {
            Class<?> workerclass = bean.getBeanClass();
            if (job.getWorkerClassName().equals(workerclass.getName())) {
                CreationalContext<?> creationalContext = beanManager.createCreationalContext(bean);
                return (BaseWorker) beanManager.getReference(bean, bean.getBeanClass(), creationalContext);
            }

        }

        log.error("No worker class {} found for {} ({})", job.getWorkerClassName(), job.getName(), job.getId());
        throw new ClassNotFoundException(job.getWorkerClassName());
    }

    public Long execute(@ObservesAsync Job job) throws Exception {

        long t1 = System.currentTimeMillis();
        thread = Thread.currentThread();
        log.trace("start of the Thread: {}", thread);

        this.job = job;
        Long jobId = job.getId();

        executionBuffer.addJobThreads(jobId, this);

        final BaseWorker workerInstance = getWorker(job);

        while (true) {
            if (this.stopMe) {
                break;
            }

            Execution execution = pollNextExecutionfromBuffer(job);

            if (execution == null) {
                allExecutionsDoneEvent.fire(new AllExecutionsDoneEvent(job));
                break;
            }

            int minMillisPerExecution = 0;
            if (job.getMaxPerMinute() != null && job.getMaxPerMinute() > 0 && job.getMaxPerMinute() <= 60000) {
                minMillisPerExecution = 60000 / job.getMaxPerMinute();
            }

            executionLoop: while (true) {

                executionBuffer.addRunningExecution(jobId, execution.getId());

                long millisAtStart = System.currentTimeMillis();

                log.trace("On Running Job Execution: {}", runningExecution);

                try {

                    workhorseController.setExecutionStatusToRunning(execution);

                    // mediterraneus
                    workerInstance.doWork(execution);

                    long duration = System.currentTimeMillis() - millisAtStart;

                    if (duration < minMillisPerExecution) {
                        // this execution was to fast and must wait to not exceed the limit of
                        // executions per minute
                        TimeUnit.MILLISECONDS.sleep(minMillisPerExecution - duration);
                    }

                    workhorseController.setExecutionStatusToFinished(execution);

                    log.trace("Execution {}, duration: {} was successfull", execution.getId(), execution.getDuration());
                    executionBuffer.removeRunningExecution(jobId, execution.getId());

                    workerInstance.onFinished(execution.getId());

                    if (executionPersistence.isBatchFinished(jobId, execution.getBatchId())) {
                        workerInstance.onFinishedBatch(execution.getBatchId(), execution.getId());
                    }

                    // Handle chained execution
                    if (execution.getChainId() != null) {
                        if (chainId == null) {
                            // chain is about to start
                            chainId = execution.getChainId();
                            chainedExecutions = executionPersistence.getChain(jobId, execution.getChainId());
                            chainedExecutions.removeIf(exe -> exe.getId().equals(chainId));
                        } else {
                            if (chainedExecutions.isEmpty()) {
                                // chain is all done
                                chainId = null;
                                workerInstance.onFinishedChain(execution.getChainId(), execution.getId());
                                break executionLoop;
                            }
                        }
                        // "remove" gets me the element...
                        execution = chainedExecutions.remove(0);
                        log.trace("This execution, Id: {} of the chain {} will be process as next.", execution.getId(), execution.getChainId());

                        runningExecution = execution;
                        continue executionLoop;
                    }

                    break executionLoop;
                } catch (Exception e) {
                    executionBuffer.removeRunningExecution(jobId, execution.getId());
                    long duration = System.currentTimeMillis() - millisAtStart;

                    // create a new Job Execution to retry this fail.
                    execution = workhorseController.handleFailedExecution(job, execution.getId(), e, duration, workerInstance);

                    if (execution == null) {
                        break executionLoop; // Do not retry
                    }

                    runningExecution = execution;

                    log.trace("Execution {} failed. It will be retry in {} seconds. ", execution.getJobId(), job.getRetryDelay() / 1000);

                    Thread.sleep(job.getRetryDelay());
                }
            }
        }

        long t2 = System.currentTimeMillis();
        log.trace("End of the Thread in {} milli .", (t2 - t1));

        return Long.valueOf(t2 - t1);
    }

    private Execution pollNextExecutionfromBuffer(Job job) {
        Execution execution = null;
        Long jobId = job.getId();

        // only the poll-function have to be thread-safe.
        ReentrantLock lock = executionBuffer.getLock(jobId);
        try {
            lock.lock();

            // Get the next Job Execution Id
            Long executionId = executionBuffer.pollPriorityExecutionQueue(jobId);
            if (executionId != null) {
                // Get the correspondent Execution
                execution = executionPersistence.getById(jobId, executionId);
            }

            // If they are no priority Execution, get a normal one.
            if (execution == null) {
                executionId = executionBuffer.pollExecutionQueue(jobId);

                if (executionId != null) {
                    execution = executionPersistence.getById(jobId, executionId);

                }
            }

            // If they are no more Execution, finish the JobThread
            if (execution == null) {
                log.trace("No more executions for the Job: {} to execute", job);

                executionBuffer.removeJobThread(jobId, this);
                executionBuffer.removeRunningJobThreadCounts(jobId);
                if (executionBuffer.getJobThreads(jobId).isEmpty()) {
                    log.trace("All job executions done for job {} ", job);
                }

            }

            runningExecution = execution;

        } finally {
            lock.unlock();
        }

        return execution;
    }

    public void stop() {
        this.stopMe = true;
    }

    public Job getJob() {
        return job;
    }

    public boolean isStopMe() {
        return stopMe;
    }

    public Execution getRunningExecution() {
        return runningExecution;
    }

    public Thread getThread() {
        return thread;
    }

    public Long getChainId() {
        return chainId;
    }

    @Override
    public String toString() {
        return "JobThread [chainId=" + chainId + ", runningExecution=" + runningExecution + ", stopMe=" + stopMe + ", thread=" + thread + "]";
    }

}
