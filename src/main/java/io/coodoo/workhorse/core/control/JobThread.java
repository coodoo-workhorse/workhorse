package io.coodoo.workhorse.core.control;

import java.time.LocalDateTime;
import java.util.concurrent.locks.ReentrantLock;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.coodoo.workhorse.core.control.event.AllExecutionsDoneEvent;
import io.coodoo.workhorse.core.entity.Execution;
import io.coodoo.workhorse.core.entity.ExecutionStatus;
import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.core.entity.WorkhorseConfig;
import io.coodoo.workhorse.persistence.interfaces.ExecutionPersistence;
import io.coodoo.workhorse.persistence.interfaces.JobPersistence;
import io.coodoo.workhorse.persistence.interfaces.qualifier.ExecutionQualifier;
import io.coodoo.workhorse.persistence.interfaces.qualifier.JobQualifier;

@Dependent
public class JobThread {

    @Inject
    WorkhorseConfig workhorseConfig;

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
        log.info("start of the Thread: " + thread);

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

                log.info("On Running Job Execution:" + runningExecution);

                try {

                    updateExecutionStatus(execution, ExecutionStatus.RUNNING, workhorseConfig.timestamp(), null, null);

                    // Land of Witch !!
                    workerInstance.doWork(execution);

                    long duration = System.currentTimeMillis() - millisAtStart;

                    if (duration < minMillisPerExecution) {
                        // this execution was to fast and must wait to not exceed the limit of
                        // executions per minute
                        Thread.sleep(minMillisPerExecution - duration);
                    }

                    String executionLog = workerInstance.getLog();

                    updateExecutionStatus(execution, ExecutionStatus.FINISHED, workhorseConfig.timestamp(),
                            Long.valueOf(duration), executionLog);

                    log.info("Execution " + execution.getId() + ", duration: " + execution.getDuration()
                            + " was successfull");
                    executionBuffer.removeRunningExecution(jobId, execution.getId());

                    workerInstance.onFinished(execution.getId());

                    if (executionPersistence.isBatchFinished(jobId, execution.getBatchId())) {
                        workerInstance.onFinishedBatch(execution.getBatchId(), execution.getId());
                    }

                    Execution nextInChain = handleChainedExecution(jobId, execution, workerInstance);
                    if (nextInChain != null) {
                        execution = nextInChain;
                        runningExecution = execution;
                        log.info("This execution, Id: " + execution.getId() + " of the chain "
                                + nextInChain.getChainId() + " will be process as next.");
                        continue executionLoop;
                    }

                    break executionLoop;
                } catch (Exception e) {
                    executionBuffer.removeRunningExecution(jobId, execution.getId());
                    long duration = System.currentTimeMillis() - millisAtStart;

                    String executionLog = workerInstance.getLog();

                    // create a new Job Execution to retry this fail.
                    execution = workhorseController.handleFailedExecution(job, execution.getId(), e, duration,
                            workerInstance, executionLog);

                    if (execution == null) {
                        break executionLoop; // Do not retry
                    }

                    runningExecution = execution;

                    log.info("Execution " + execution.getJobId() + " failed. It will be retry in "
                            + job.getRetryDelay() / 1000 + " seconds. ");

                    Thread.sleep(job.getRetryDelay());
                }
            }
        }

        long t2 = System.currentTimeMillis();
        log.info("End of the Thread in " + (t2 - t1) + " milli .");

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
                log.info("No more executions for the Job: " + job + " to execute");

                executionBuffer.removeJobThread(jobId, this);
                executionBuffer.removeRunningJobThreadCounts(jobId);
                if (executionBuffer.getJobThreads(jobId).isEmpty()) {
                    log.info("All job executions done for job " + job);
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

    /**
     * Set Execution on RUNNING status
     * 
     * @param execution
     * @param timeStamp
     */
    public void updateExecutionStatusToRunning(Execution execution, LocalDateTime timeStamp) {
        updateExecutionStatus(execution, ExecutionStatus.RUNNING, timeStamp, null, null);
    }

    public void updateExecutionStatus(Execution execution, ExecutionStatus executionStatus, LocalDateTime timeStamp,
            Long duration, String executionLog) {
        execution.setStatus(executionStatus);
        execution.setLog(executionLog);

        switch (executionStatus) {
            case RUNNING:
                execution.setStartedAt(timeStamp);
                break;
            case FINISHED:
            case FAILED:
                execution.setDuration(duration);
                execution.setEndedAt(timeStamp);
                break;
            default:
                break;
        }
        executionPersistence.update(execution.getJobId(), execution.getId(), execution);
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
