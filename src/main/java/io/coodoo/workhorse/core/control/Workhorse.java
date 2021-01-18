package io.coodoo.workhorse.core.control;

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

import io.coodoo.workhorse.config.entity.WorkhorseConfig;
import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.core.entity.ErrorType;
import io.coodoo.workhorse.core.control.event.AllJobExecutionsDoneEvent;
import io.coodoo.workhorse.core.control.event.JobErrorEvent;
import io.coodoo.workhorse.core.control.event.NewExecutionEvent;
import io.coodoo.workhorse.core.entity.Execution;
import io.coodoo.workhorse.core.entity.ExecutionStatus;
import io.coodoo.workhorse.core.entity.JobStatus;
import io.coodoo.workhorse.persistence.PersistenceManager;
import io.coodoo.workhorse.persistence.interfaces.ExecutionPersistence;
import io.coodoo.workhorse.persistence.interfaces.JobPersistence;
import io.coodoo.workhorse.persistence.interfaces.qualifier.JobQualifier;
import io.coodoo.workhorse.persistence.interfaces.qualifier.ExecutionQualifier;

@ApplicationScoped
public class Workhorse {

    private static final Logger log = Logger.getLogger(Workhorse.class);

    @Inject
    @JobQualifier
    JobPersistence jobPersistence;

    @Inject
    @ExecutionQualifier
    ExecutionPersistence executionPersistence;

    @Inject
    PersistenceManager persistenceManager;


    @Inject
    ExecutionBuffer executionBuffer;

    @Inject
    WorkhorseConfig jobEngineConfig;

    @Inject
    Event<Job> jobThreadManager;

    @Inject
    Event<JobErrorEvent> jobErrorEvent;

    @Inject
    Event<AllJobExecutionsDoneEvent> allJobExecutionDoneEvent;

    private ScheduledExecutorService scheduledExecutorService;

    private ScheduledFuture<?> scheduledFuture;

    @PostConstruct
    public void init() {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Start the Poller/Pusher of the Workhorse
     * 
     * @param usePusher May the pusher to be use instead of the poller ?
     */
    public void start() {
        if (scheduledFuture != null) {
            stop();
        }
        
        if (executionPersistence.isPusherAvailable()) {
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

            if (executionBuffer.getNumberOfJobExecution(job.getId()) < jobEngineConfig.getJobQueueMin()) {
                List<Execution> jobExecutions = executionPersistence.pollNextJobExecutions(job.getId(),
                        jobEngineConfig.getJobQueueMax());
                for (Execution jobExecution : jobExecutions) {
                    if (jobExecution == null) {
                        continue;
                    }
                    if (jobExecutionDistributor(jobExecution)) {
                        executedJobExecution++;
                        // log.info("JobExecution add" + executedJobExecution);
                    }
                }
            }
            log.info("Number of job execution in buffer:" + executionBuffer.getNumberOfJobExecution(job.getId()));
        }

    }

    /**
     * recieve the notification about new persisted job execution
     * 
     * @param newjobExecutionEvent
     */
    public void push(@ObservesAsync NewExecutionEvent newjobExecutionEvent) {
        if (!executionPersistence.isPusherAvailable() || scheduledFuture == null) {
            return;
        }
        log.info("New Job Execution pushed: " + newjobExecutionEvent);
        if (executionBuffer.getNumberOfJobExecution(newjobExecutionEvent.jobId) < jobEngineConfig.getJobQueueMax()) {
            Execution jobExecution = executionPersistence.getById(newjobExecutionEvent.jobId,
                    newjobExecutionEvent.executionId);
            if (jobExecution != null) {
                if (jobExecution.getMaturity() != null) {
                    long delayInSeconds = ChronoUnit.SECONDS.between(jobEngineConfig.timestamp(),
                            jobExecution.getMaturity());

                    log.info("Job Execution : " + jobExecution + " will be process in " + delayInSeconds + " seconds");
                    scheduledExecutorService.schedule(() -> {
                        jobExecutionDistributor(jobExecution);
                    }, delayInSeconds, TimeUnit.SECONDS);
                    // Only the head of a chainJobExecution may integrate the executionBuffer
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

            if (executionPersistence.isPusherAvailable()) {
                log.info("Job queue pusher stopped");
            } else {
                log.info("Job queue poller stopped");
            }

        } else {
            if (executionPersistence.isPusherAvailable()) {
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
    public boolean jobExecutionDistributor(Execution jobExecution) {

        if (jobExecution == null || jobExecution.getStatus() != ExecutionStatus.QUEUED) {
            return false;
        }

        Long jobId = jobExecution.getJobId();

        if (!executionBuffer.canTheJobExecutionBeAdd(jobId, jobExecution.getId())) {
            return false;
        }

        final int nbrOfJobExecutions = executionBuffer.getNumberOfJobExecution(jobId);

        if (nbrOfJobExecutions == 0) {
            executionBuffer.addJobStartTimes(jobId, System.currentTimeMillis());
        }

        final ReentrantLock lock = executionBuffer.getLock(jobId);
        try {
            lock.lock();

            if (Boolean.TRUE.equals(jobExecution.getPriority())) {
                executionBuffer.addPriorityJobExecution(jobId, jobExecution.getId());
            } else {
                executionBuffer.addJobExecution(jobId, jobExecution.getId());
            }

            log.infof("New JobExecution: " + jobExecution + " in Queue. Number of Executions in Queue: "
                    + nbrOfJobExecutions);
        } finally {
            lock.unlock();
        }

        log.info(executionBuffer.getRunningJobThreadCounts(jobId));
        if (executionBuffer.getJobThreadCounts(jobId) > executionBuffer.getRunningJobThreadCounts(jobId)) {
            // lock = jobExecutionQueue.getLock(jobId);
            try {
                lock.lock();
                for (int i = executionBuffer.getRunningJobThreadCounts(jobId); i < executionBuffer
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
        executionBuffer.addRunningJobThreadCounts(jobId);

        executionBuffer.addCompletionStage(jobId, completion);

        // handle the fail of a JobThread
        completion.exceptionally(exception -> {
            log.error("Error in job thread - Process gets cancelled", exception);
            job.setStatus(JobStatus.ERROR);
            jobPersistence.update(jobId, job);

            executionBuffer.cancelProcess(job);

            jobErrorEvent.fire(new JobErrorEvent(exception, ErrorType.JOB_THREAD_ERROR.getMessage(), job.getId(), JobStatus.ERROR ));
            return job;
        });

        completion.thenApply(fn -> {
            executionBuffer.removeCompletionStage(jobId, completion);
            log.info("Thread is really over: " + fn);
            return this;
        });

    }

    public void allJobExecutionDoneEvent(@Observes AllJobExecutionsDoneEvent event) {
        final Job job = event.getJob();

        Long startTime = executionBuffer.getJobStartTimes(job.getId());
        if (startTime != null) {
            long durationMillis = System.currentTimeMillis() - startTime;

            final String durationText = String.format("%d min, %d sec", TimeUnit.MILLISECONDS.toMinutes(durationMillis),
                    TimeUnit.MILLISECONDS.toSeconds(durationMillis)
                            - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(durationMillis)));
            final String message = "Duration of all " + job.getName() + " job executions: " + durationText;

            log.info(message + durationMillis);

            executionBuffer.removeJobStartTimes(job.getId());
        }
    }

}