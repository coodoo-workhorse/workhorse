package io.coodoo.workhorse.core.control;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.coodoo.workhorse.core.boundary.ExecutionContext;
import io.coodoo.workhorse.core.control.event.AllExecutionsDoneEvent;
import io.coodoo.workhorse.core.entity.Execution;
import io.coodoo.workhorse.core.entity.ExecutionStatus;
import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.persistence.interfaces.ExecutionPersistence;
import io.coodoo.workhorse.persistence.interfaces.JobPersistence;
import io.coodoo.workhorse.persistence.interfaces.qualifier.ExecutionQualifier;
import io.coodoo.workhorse.persistence.interfaces.qualifier.JobQualifier;
import io.coodoo.workhorse.util.WorkhorseUtil;

/**
 * Class that executes all executions of the a given job.
 * 
 * @author coodoo GmbH (coodoo.io)
 */
@Dependent
public class JobThread {

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

    private boolean stopMe;
    private Execution runningExecution;
    private Thread thread;

    private static final Logger log = LoggerFactory.getLogger(JobThread.class);

    public Long execute(@ObservesAsync Job job) throws Exception {

        long t1 = System.currentTimeMillis();
        thread = Thread.currentThread();
        log.trace("start of the Thread: {}", thread);

        Long jobId = job.getId();

        executionBuffer.addJobThreads(jobId, this);

        final BaseWorker workerInstance = workhorseController.getWorker(job);

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
            if (job.getMaxPerMinute() > 0 && job.getMaxPerMinute() <= 60000) {
                minMillisPerExecution = 60000 / job.getMaxPerMinute();
            }

            executionLoop: while (true) {

                executionBuffer.addRunningExecution(jobId, execution.getId());

                long millisAtStart = System.currentTimeMillis();

                ExecutionContext executionContext = workerInstance.getExecutionContext();

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

                    String executionLog = executionContext.getLog();

                    execution.setLog(executionLog);
                    executionPersistence.update(execution);
                    workhorseController.setExecutionStatusToFinished(execution);

                    log.trace("Execution {}, duration: {} was successfull", execution.getId(), execution.getDuration());
                    executionBuffer.removeRunningExecution(jobId, execution.getId());

                    workerInstance.onFinished(execution.getId());

                    if (executionPersistence.isBatchFinished(jobId, execution.getBatchId())) {
                        workerInstance.onFinishedBatch(execution.getBatchId(), execution.getId());
                    }

                    Execution nextInChain = handleChainedExecution(jobId, execution, workerInstance);
                    if (nextInChain != null) {
                        execution = nextInChain;
                        runningExecution = execution;
                        log.trace("This execution, Id: {} of the chain {} will be process as next.", execution.getId(), nextInChain.getChainId());
                        continue executionLoop;
                    }

                    break executionLoop;
                } catch (Exception e) {
                    executionBuffer.removeRunningExecution(jobId, execution.getId());
                    long duration = System.currentTimeMillis() - millisAtStart;

                    String executionLog = executionContext.getLog();

                    // create a new Job Execution to retry this fail.
                    execution = workhorseController.handleFailedExecution(job, execution.getId(), e, duration, workerInstance, executionLog);

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

    private Execution handleChainedExecution(Long jobId, Execution execution, BaseWorker workerInstance) {
        Long chainId = execution.getChainId();
        Execution nextInChain = null;
        if (chainId != null) {
            nextInChain = executionPersistence.getNextQueuedExecutionInChain(jobId, chainId, execution);
            if (nextInChain == null) {
                workerInstance.onFinishedChain(chainId, execution.getId());
            }
        }

        return nextInChain;
    }

    public void stop() {
        this.stopMe = true;
    }

    public Execution getActiveExecution() {
        return runningExecution;
    }

    @Override
    public String toString() {
        return "JobThread [runningExecution=" + runningExecution + ", stopMe=" + stopMe + ", thread=" + thread + "]";
    }

}
