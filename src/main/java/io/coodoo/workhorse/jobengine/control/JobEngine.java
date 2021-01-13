package io.coodoo.workhorse.jobengine.control;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.coodoo.workhorse.config.entity.JobEngineConfig;
import io.coodoo.workhorse.jobengine.control.events.AllJobExecutionsDoneJobEvent;
import io.coodoo.workhorse.jobengine.control.events.JobErrorEvent;
import io.coodoo.workhorse.jobengine.control.events.NewJobExecutionEvent;
import io.coodoo.workhorse.jobengine.entity.Job;
import io.coodoo.workhorse.jobengine.entity.JobEngineErrorType;
import io.coodoo.workhorse.jobengine.entity.JobExecution;
import io.coodoo.workhorse.jobengine.entity.JobExecutionStatus;
import io.coodoo.workhorse.jobengine.entity.JobStatus;
import io.coodoo.workhorse.persistence.PersistenceManager;
import io.coodoo.workhorse.persistence.interfaces.JobExecutionPersistence;
import io.coodoo.workhorse.persistence.interfaces.JobPersistence;
import io.coodoo.workhorse.persistence.interfaces.qualifiers.JobDAO;
import io.coodoo.workhorse.persistence.interfaces.qualifiers.JobExecutionDAO;

@ApplicationScoped
public class JobEngine {

    private static final Logger log = Logger.getLogger(JobEngine.class);

    @Inject
    @JobDAO
    JobPersistence jobPersistence;

    @Inject
    @JobExecutionDAO
    JobExecutionPersistence jobExecutionPersistence;

    @Inject
    PersistenceManager persistenceManager;


    @Inject
    JobExecutionBuffer jobExecutionBuffer;

    @Inject
    JobEngineConfig jobEngineConfig;

    @Inject
    Event<Job> jobThreadManager;

    @Inject
    Event<JobErrorEvent> jobErrorEvent;

    @Inject
    Event<AllJobExecutionsDoneJobEvent> allJobExecutionDoneEvent;

    private ScheduledExecutorService scheduledExecutorService;

    private ScheduledFuture<?> scheduledFuture;

    @PostConstruct
    public void init() {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Start the Poller/Pusher of the JobEngine
     * 
     * @param usePusher May the pusher to be use instead of the poller ?
     */
    public void start() {
        if (scheduledFuture != null) {
            stop();
        }
        
        if (jobExecutionPersistence.isPusherAvailable()) {
            scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(this::poll, 0,
                    jobEngineConfig.getJobQueuePusherPoll(), TimeUnit.SECONDS);

            log.info(
                    "Job queue pusher started with a " + jobEngineConfig.getJobQueuePusherPoll() + " seconds interval");

        } else {
            scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(this::poll, 0,
                    jobEngineConfig.getJobQueuePollerInterval(), TimeUnit.SECONDS);
            log.info("Job queue poller started with a " + jobEngineConfig.getJobQueuePollerInterval()
                    + " seconds interval");
        }

    }

    /**
     * poll the JobExecution with an given intervall
     */
    void poll() {
        for (Job job : jobPersistence.getAllByStatus(JobStatus.ACTIVE)) {
            int executedJobExecution = 0;

            if (jobExecutionBuffer.getNumberOfJobExecution(job.getId()) < jobEngineConfig.getJobQueueMin()) {
                List<JobExecution> jobExecutions = jobExecutionPersistence.pollNextJobExecutions(job.getId(),
                        jobEngineConfig.getJobQueueMax());
                for (JobExecution jobExecution : jobExecutions) {
                    if (jobExecution == null) {
                        continue;
                    }
                    if (jobExecutionDistributor(jobExecution)) {
                        executedJobExecution++;
                        // log.info("JobExecution add" + executedJobExecution);
                    }
                }
            }
            log.info("Number of job execution in buffer:" + jobExecutionBuffer.getNumberOfJobExecution(job.getId()));
        }

    }

    /**
     * recieve the notification about new persisted job execution
     * 
     * @param newjobExecutionEvent
     */
    public void push(@ObservesAsync NewJobExecutionEvent newjobExecutionEvent) {
        if (!jobExecutionPersistence.isPusherAvailable() || scheduledFuture == null) {
            return;
        }
        log.info("New Job Execution pushed: " + newjobExecutionEvent);
        if (jobExecutionBuffer.getNumberOfJobExecution(newjobExecutionEvent.jobId) < jobEngineConfig.getJobQueueMax()) {
            JobExecution jobExecution = jobExecutionPersistence.getById(newjobExecutionEvent.jobId,
                    newjobExecutionEvent.jobExecutionId);
            if (jobExecution != null) {
                if (jobExecution.getMaturity() != null) {
                    long delayInSeconds = ChronoUnit.SECONDS.between(jobEngineConfig.timestamp(),
                            jobExecution.getMaturity());

                    log.info("Job Execution : " + jobExecution + " will be process in " + delayInSeconds + " seconds");
                    scheduledExecutorService.schedule(() -> {
                        jobExecutionDistributor(jobExecution);
                    }, delayInSeconds, TimeUnit.SECONDS);
                    // Only the head of a chainJobExecution may integrate the jobExecutionBuffer
                } else if (jobExecution.getChainedPreviousExecutionId() == null) {
                    jobExecutionDistributor(jobExecution);
                }
            } else {
                log.error("No job execution found for jobExecutionId: " + newjobExecutionEvent.jobId);
            }
        }
    }

    /**
     * Stop the poller/pusher process
     */
    public void stop() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            scheduledFuture = null;

            if (jobExecutionPersistence.isPusherAvailable()) {
                log.info("Job queue pusher stopped");
            } else {
                log.info("Job queue poller stopped");
            }

        } else {
            if (jobExecutionPersistence.isPusherAvailable()) {
                log.info("Job queue pusher cann't be stopped because it's not currently running!");
            } else {
                log.info("Job queue poller cann't be stopped because it's not currently running!");
            }
        }
    }

    /**
     * Check if the job engine is running
     * 
     * @return boolean
     */
    public boolean isRunning() {
        return scheduledFuture != null;
    }

    /**
     * add a job execution in the jobExecutionQueue of the correspondent job
     * 
     * @param jobExecution Job Execution to map with a jobExecutionQueue
     * @return true if the mapping was successful and false otherwise
     */
    public boolean jobExecutionDistributor(JobExecution jobExecution) {

        if (jobExecution == null || jobExecution.getStatus() != JobExecutionStatus.QUEUED) {
            return false;
        }

        Long jobId = jobExecution.getJobId();

        if (!jobExecutionBuffer.canTheJobExecutionBeAdd(jobId, jobExecution.getId())) {
            return false;
        }

        final int nbrOfJobExecutions = jobExecutionBuffer.getNumberOfJobExecution(jobId);

        if (nbrOfJobExecutions == 0) {
            jobExecutionBuffer.addJobStartTimes(jobId, System.currentTimeMillis());
        }

        final ReentrantLock lock = jobExecutionBuffer.getLock(jobId);
        try {
            lock.lock();

            if (Boolean.TRUE.equals(jobExecution.getPriority())) {
                jobExecutionBuffer.addPriorityJobExecution(jobId, jobExecution.getId());
            } else {
                jobExecutionBuffer.addJobExecution(jobId, jobExecution.getId());
            }

            log.infof("New JobExecution: " + jobExecution + " in Queue. Number of Executions in Queue: "
                    + nbrOfJobExecutions);
        } finally {
            lock.unlock();
        }

        log.info(jobExecutionBuffer.getRunningJobThreadCounts(jobId));
        if (jobExecutionBuffer.getJobThreadCounts(jobId) > jobExecutionBuffer.getRunningJobThreadCounts(jobId)) {
            // lock = jobExecutionQueue.getLock(jobId);
            try {
                lock.lock();
                for (int i = jobExecutionBuffer.getRunningJobThreadCounts(jobId); i < jobExecutionBuffer
                        .getJobThreadCounts(jobId); i++) {
                    startJobThread(jobId);
                }
            } finally {
                lock.unlock();
            }
        }

        return true;

    }

    /**
     * Start a thread to process the job execution of the given Job.
     * 
     * @param jobId Id of the job, which job execution have to be process.
     */
    private void startJobThread(Long jobId) {
        Job job = jobPersistence.get(jobId);

        if (!JobStatus.ACTIVE.equals(job.getStatus())) {
              log.error("The job " + job + " is not ACTIVE ");
              return;
        }

        CompletionStage<Job> completion = jobThreadManager.fireAsync(job);

        // Increment the number of created JobThread for the Job with the Id jobId
        jobExecutionBuffer.addRunningJobThreadCounts(jobId);

        jobExecutionBuffer.addCompletionStage(jobId, completion);

        // handle the fail of a JobThread
        completion.exceptionally(exception -> {
            log.error("Error in job thread - Process gets cancelled", exception);
            job.setStatus(JobStatus.ERROR);
            jobPersistence.update(jobId, job);

            jobExecutionBuffer.cancelProcess(job);

            jobErrorEvent.fire(new JobErrorEvent(exception, JobEngineErrorType.JOB_THREAD_ERROR.getMessage(), job.getId(), JobStatus.ERROR ));
            return job;
        });

        completion.thenApply(fn -> {
            jobExecutionBuffer.removeCompletionStage(jobId, completion);
            log.info("Thread is really over: " + fn);
            return this;
        });

    }

    public void allJobExecutionDoneEvent(@Observes AllJobExecutionsDoneJobEvent event) {
        final Job job = event.getJob();

        Long startTime = jobExecutionBuffer.getJobStartTimes(job.getId());
        if (startTime != null) {
            long durationMillis = System.currentTimeMillis() - startTime;

            final String durationText = String.format("%d min, %d sec", TimeUnit.MILLISECONDS.toMinutes(durationMillis),
                    TimeUnit.MILLISECONDS.toSeconds(durationMillis)
                            - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(durationMillis)));
            final String message = "Duration of all " + job.getName() + " job executions: " + durationText;

            log.info(message + durationMillis);

            jobExecutionBuffer.removeJobStartTimes(job.getId());
        }
    }

}