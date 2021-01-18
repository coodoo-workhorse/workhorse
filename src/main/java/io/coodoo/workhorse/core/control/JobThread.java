package io.coodoo.workhorse.core.control;

import java.time.LocalDateTime;
import java.util.concurrent.locks.ReentrantLock;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.coodoo.workhorse.config.entity.WorkhorseConfig;
import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.core.control.event.AllJobExecutionsDoneEvent;
import io.coodoo.workhorse.core.entity.Execution;
import io.coodoo.workhorse.core.entity.ExecutionStatus;
import io.coodoo.workhorse.persistence.interfaces.ExecutionPersistence;
import io.coodoo.workhorse.persistence.interfaces.JobPersistence;
import io.coodoo.workhorse.persistence.interfaces.qualifier.JobQualifier;
import io.coodoo.workhorse.persistence.interfaces.qualifier.ExecutionQualifier;

@Dependent
public class JobThread {

    @Inject
    WorkhorseConfig jobEngineConfig;

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
    Event<AllJobExecutionsDoneEvent> allJobExecutionDoneEvent;

    private boolean stopMe;
    private Execution runningJobExecution;
    private Thread thread;

    private static final Logger log = Logger.getLogger(JobThread.class);

    public Long execute(@ObservesAsync Job job) throws Exception {

        long t1 = System.currentTimeMillis();
        thread = Thread.currentThread();
        log.info("start of the Thread: " + thread);

        Long jobId = job.getId();

        executionBuffer.addJobThreads(jobId, this);

        final BaseJobWorker workerInstance = workhorseController.getJobWorker(job);

        while (true) {
            if (this.stopMe) {
                break;
            }

            Execution jobExecution = pollNextExecutionfromBuffer(job);

            if (jobExecution == null) {
                allJobExecutionDoneEvent.fire(new AllJobExecutionsDoneEvent(job));
                break;
            }

            int minMillisPerExecution = 0;
            if (job.getMaxPerMinute() > 0 && job.getMaxPerMinute() <= 60000) {
                minMillisPerExecution = 60000 / job.getMaxPerMinute();
            }

            jobExecutionLoop: while (true) {

                executionBuffer.addRunningJobExecution(jobId, jobExecution.getId());

                long millisAtStart = System.currentTimeMillis();

                log.info("On Running Job Execution:" + runningJobExecution);

                try {

                    updateExecutionStatus(jobExecution, ExecutionStatus.RUNNING, jobEngineConfig.timestamp(),
                            null, null);

                    // Land of Witch !!
                    workerInstance.doWork(jobExecution);

                    long duration = System.currentTimeMillis() - millisAtStart;

                    if (duration < minMillisPerExecution) {
                        // this execution was to fast and must wait to not exceed the limit of
                        // executions per minute
                        Thread.sleep(minMillisPerExecution - duration);
                    }

                    String jobExecutionLog = workerInstance.getLog();

                    updateExecutionStatus(jobExecution, ExecutionStatus.FINISHED, jobEngineConfig.timestamp(),
                            Long.valueOf(duration), jobExecutionLog);

                    log.info("JobExecution " + jobExecution.getId() + ", duration: " + jobExecution.getDuration()
                            + " was successfull");
                    executionBuffer.removeRunningJobExecution(jobId, jobExecution.getId());

                    workerInstance.onFinished(jobExecution.getId());

                    if (executionPersistence.isBatchFinished(jobId, jobExecution.getBatchId())) {
                        workerInstance.onFinishedBatch(jobExecution.getBatchId(), jobExecution.getId());
                    }

                    Execution nextInChain = handleChainedJobExecution(jobId, jobExecution, workerInstance);
                    if (nextInChain != null) {
                        jobExecution = nextInChain;
                        runningJobExecution = jobExecution;
                        log.info("This execution, Id: " + jobExecution.getId() + " of the chain "
                                + nextInChain.getChainId() + " will be process as next.");
                        continue jobExecutionLoop;
                    }

                    break jobExecutionLoop;
                } catch (Exception e) {
                    executionBuffer.removeRunningJobExecution(jobId, jobExecution.getId());
                    long duration = System.currentTimeMillis() - millisAtStart;

                    String jobExecutionLog = workerInstance.getLog();

                    // create a new Job Execution to retry this fail.
                    jobExecution = workhorseController.handleFailedJobExecution(job, jobExecution.getId(), e, duration,
                            workerInstance, jobExecutionLog);

                    if (jobExecution == null) {
                        break jobExecutionLoop; // Do not retry
                    }

                    runningJobExecution = jobExecution;

                    log.info("JobExecution " + jobExecution.getJobId() + " failed. It will be retry in "
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
        Execution jobExecution = null;
        Long jobId = job.getId();

        // only the poll-function have to be thread-safe.
        ReentrantLock lock = executionBuffer.getLock(jobId);
        try {
            lock.lock();

            // Get the next Job Execution Id
            Long jobExecutionId = executionBuffer.pollPriorityJobExecutionQueue(jobId);
            if (jobExecutionId != null) {
                // Get the correspondent JobExecution
                jobExecution = executionPersistence.getById(jobId, jobExecutionId);
            }

            // If they are no priority JobExecution, get a normal one.
            if (jobExecution == null) {
                jobExecutionId = executionBuffer.pollJobExecutionQueue(jobId);

                if (jobExecutionId != null) {
                    jobExecution = executionPersistence.getById(jobId, jobExecutionId);

                }
            }

            // If they are no more JobExecution, finish the JobThread
            if (jobExecution == null) {
                log.infof("No more executions for the Job: " + job + " to execute");

                executionBuffer.removeJobThread(jobId, this);
                executionBuffer.removeRunningJobThreadCounts(jobId);
                if (executionBuffer.getJobThreads(jobId).isEmpty()) {
                    log.info("All job executions done for job " + job);
                }

            }

            runningJobExecution = jobExecution;

        } finally {
            lock.unlock();
        }

        return jobExecution;
    }

    private Execution handleChainedJobExecution(Long jobId, Execution jobExecution,
            BaseJobWorker workerInstance) {
        Long chainId = jobExecution.getChainId();
        Execution nextInChain = null;
        if (chainId != null) {
            nextInChain = executionPersistence.getNextQueuedJobExecutionInChain(jobId, chainId, jobExecution);
            if (nextInChain == null) {
                workerInstance.onFinishedChain(chainId, jobExecution.getId());
            }
        }

        return nextInChain;
    }

    /**
     * Set JobExecution on RUNNING status
     * 
     * @param jobExecution
     * @param timeStamp
     */
    public void updateExecutionStatusToRunning(Execution jobExecution, LocalDateTime timeStamp) {
        updateExecutionStatus(jobExecution, ExecutionStatus.RUNNING, timeStamp, null, null);
    }

    public void updateExecutionStatus(Execution execution, ExecutionStatus executionStatus,
            LocalDateTime timeStamp, Long duration, String jobExecutionLog) {
        execution.setStatus(executionStatus);
        execution.setLog(jobExecutionLog);

        if (executionStatus.equals(ExecutionStatus.RUNNING)) {
            execution.setStartedAt(timeStamp);
        } else if (executionStatus.equals(ExecutionStatus.FINISHED)
                || executionStatus.equals(ExecutionStatus.FAILED)) {
            execution.setDuration(duration);
            execution.setEndedAt(timeStamp);
        }

        executionPersistence.update(execution.getJobId(), execution.getId(), execution);
    }

    public void stop() {
        this.stopMe = true;
    }

    public Execution getActiveExecution() {
        return runningJobExecution;
    }

    @Override
    public String toString() {
        return "JobThread [runningJobExecution=" + runningJobExecution + ", stopMe=" + stopMe + ", thread=" + thread
                + "]";
    }

}