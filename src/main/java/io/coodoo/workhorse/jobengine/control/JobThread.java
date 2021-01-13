package io.coodoo.workhorse.jobengine.control;

import java.time.LocalDateTime;
import java.util.concurrent.locks.ReentrantLock;

import javax.enterprise.event.Event;
import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.coodoo.workhorse.config.entity.JobEngineConfig;
import io.coodoo.workhorse.jobengine.control.events.AllJobExecutionsDoneJobEvent;
import io.coodoo.workhorse.jobengine.entity.Job;
import io.coodoo.workhorse.jobengine.entity.JobExecution;
import io.coodoo.workhorse.jobengine.entity.JobExecutionStatus;
import io.coodoo.workhorse.persistence.interfaces.JobExecutionPersistence;
import io.coodoo.workhorse.persistence.interfaces.JobPersistence;
import io.coodoo.workhorse.persistence.interfaces.qualifiers.JobDAO;
import io.coodoo.workhorse.persistence.interfaces.qualifiers.JobExecutionDAO;

public class JobThread {

    @Inject
    JobEngineConfig jobEngineConfig;

    @Inject
    @JobExecutionDAO
    JobExecutionPersistence jobExecutionPersistence;

    @Inject
    @JobDAO
    JobPersistence jobPersistence;

    @Inject
    JobEngineController jobEngineController;

    @Inject
    JobExecutionBuffer jobExecutionBuffer;

    @Inject
    Event<AllJobExecutionsDoneJobEvent> allJobExecutionDoneEvent;

    private boolean stopMe;
    private JobExecution runningJobExecution;
    private Thread thread;

    private static final Logger log = Logger.getLogger(JobThread.class);

    public Long execute(@ObservesAsync Job job) throws Exception {

        long t1 = System.currentTimeMillis();
        thread = Thread.currentThread();
        log.info("start of the Thread: " + thread);

        Long jobId = job.getId();

        jobExecutionBuffer.addJobThreads(jobId, this);

        final BaseJobWorker workerInstance = jobEngineController.getJobWorker(job);

        while (true) {
            if (this.stopMe) {
                break;
            }

            JobExecution jobExecution = pollNextExecutionfromBuffer(job);

            if (jobExecution == null) {
                allJobExecutionDoneEvent.fire(new AllJobExecutionsDoneJobEvent(job));
                break;
            }

            int minMillisPerExecution = 0;
            if (job.getMaxPerMinute() > 0 && job.getMaxPerMinute() <= 60000) {
                minMillisPerExecution = 60000 / job.getMaxPerMinute();
            }

            jobExecutionLoop: while (true) {

                jobExecutionBuffer.addRunningJobExecution(jobId, jobExecution.getId());

                long millisAtStart = System.currentTimeMillis();

                log.info("On Running Job Execution:" + runningJobExecution);

                try {

                    updateJobExecutionStatus(jobExecution, JobExecutionStatus.RUNNING, jobEngineConfig.timestamp(),
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

                    updateJobExecutionStatus(jobExecution, JobExecutionStatus.FINISHED, jobEngineConfig.timestamp(),
                            Long.valueOf(duration), jobExecutionLog);

                    log.info("JobExecution " + jobExecution.getId() + ", duration: " + jobExecution.getDuration()
                            + " was successfull");
                    jobExecutionBuffer.removeRunningJobExecution(jobId, jobExecution.getId());

                    workerInstance.onFinished(jobExecution.getId());

                    if (jobExecutionPersistence.isBatchFinished(jobId, jobExecution.getBatchId())) {
                        workerInstance.onFinishedBatch(jobExecution.getBatchId(), jobExecution.getId());
                    }

                    JobExecution nextInChain = handleChainedJobExecution(jobId, jobExecution, workerInstance);
                    if (nextInChain != null) {
                        jobExecution = nextInChain;
                        runningJobExecution = jobExecution;
                        log.info("This execution, Id: " + jobExecution.getId() + " of the chain "
                                + nextInChain.getChainId() + " will be process as next.");
                        continue jobExecutionLoop;
                    }

                    break jobExecutionLoop;
                } catch (Exception e) {
                    jobExecutionBuffer.removeRunningJobExecution(jobId, jobExecution.getId());
                    long duration = System.currentTimeMillis() - millisAtStart;

                    String jobExecutionLog = workerInstance.getLog();

                    // create a new Job Execution to retry this fail.
                    jobExecution = jobEngineController.handleFailedJobExecution(job, jobExecution.getId(), e, duration,
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

    private JobExecution pollNextExecutionfromBuffer(Job job) {
        JobExecution jobExecution = null;
        Long jobId = job.getId();

        // only the poll-function have to be thread-safe.
        ReentrantLock lock = jobExecutionBuffer.getLock(jobId);
        try {
            lock.lock();

            // Get the next Job Execution Id
            Long jobExecutionId = jobExecutionBuffer.pollPriorityJobExecutionQueue(jobId);
            if (jobExecutionId != null) {
                // Get the correspondent JobExecution
                jobExecution = jobExecutionPersistence.getById(jobId, jobExecutionId);
            }

            // If they are no priority JobExecution, get a normal one.
            if (jobExecution == null) {
                jobExecutionId = jobExecutionBuffer.pollJobExecutionQueue(jobId);

                if (jobExecutionId != null) {
                    jobExecution = jobExecutionPersistence.getById(jobId, jobExecutionId);

                }
            }

            // If they are no more JobExecution, finish the JobThread
            if (jobExecution == null) {
                log.infof("No more executions for the Job: " + job + " to execute");

                jobExecutionBuffer.removeJobThread(jobId, this);
                jobExecutionBuffer.removeRunningJobThreadCounts(jobId);
                if (jobExecutionBuffer.getJobThreads(jobId).isEmpty()) {
                    log.info("All job executions done for job " + job);
                }

            }

            runningJobExecution = jobExecution;

        } finally {
            lock.unlock();
        }

        return jobExecution;
    }

    private JobExecution handleChainedJobExecution(Long jobId, JobExecution jobExecution,
            BaseJobWorker workerInstance) {
        Long chainId = jobExecution.getChainId();
        JobExecution nextInChain = null;
        if (chainId != null) {
            nextInChain = jobExecutionPersistence.getNextQueuedJobExecutionInChain(jobId, chainId, jobExecution);
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
    public void updateJobExecutionStatusToRunning(JobExecution jobExecution, LocalDateTime timeStamp) {
        updateJobExecutionStatus(jobExecution, JobExecutionStatus.RUNNING, timeStamp, null, null);
    }

    public void updateJobExecutionStatus(JobExecution jobExecution, JobExecutionStatus jobExecutionStatus,
            LocalDateTime timeStamp, Long duration, String jobExecutionLog) {
        jobExecution.setStatus(jobExecutionStatus);
        jobExecution.setLog(jobExecutionLog);

        if (jobExecutionStatus.equals(JobExecutionStatus.RUNNING)) {
            jobExecution.setStartedAt(timeStamp);
        } else if (jobExecutionStatus.equals(JobExecutionStatus.FINISHED)
                || jobExecutionStatus.equals(JobExecutionStatus.FAILED)) {
            jobExecution.setDuration(duration);
            jobExecution.setEndedAt(timeStamp);
        }

        jobExecutionPersistence.update(jobExecution.getJobId(), jobExecution.getId(), jobExecution);
    }

    public void stop() {
        this.stopMe = true;
    }

    public JobExecution getActiveJobExecution() {
        return runningJobExecution;
    }

    @Override
    public String toString() {
        return "JobThread [runningJobExecution=" + runningJobExecution + ", stopMe=" + stopMe + ", thread=" + thread
                + "]";
    }

}