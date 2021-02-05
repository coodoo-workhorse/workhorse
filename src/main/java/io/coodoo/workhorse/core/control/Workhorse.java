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
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.coodoo.workhorse.core.control.event.AllExecutionsDoneEvent;
import io.coodoo.workhorse.core.control.event.JobErrorEvent;
import io.coodoo.workhorse.core.control.event.NewExecutionEvent;
import io.coodoo.workhorse.core.entity.ErrorType;
import io.coodoo.workhorse.core.entity.Execution;
import io.coodoo.workhorse.core.entity.ExecutionStatus;
import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.core.entity.JobStatus;
import io.coodoo.workhorse.core.entity.WorkhorseConfig;
import io.coodoo.workhorse.persistence.PersistenceManager;
import io.coodoo.workhorse.persistence.interfaces.ExecutionPersistence;
import io.coodoo.workhorse.persistence.interfaces.JobPersistence;
import io.coodoo.workhorse.persistence.interfaces.qualifier.ExecutionQualifier;
import io.coodoo.workhorse.persistence.interfaces.qualifier.JobQualifier;

@ApplicationScoped
public class Workhorse {

    private static final Logger log = LoggerFactory.getLogger(Workhorse.class);

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
    WorkhorseConfig workhorseConfig;

    @Inject
    Event<Job> jobThreadManager;

    @Inject
    Event<JobErrorEvent> jobErrorEvent;

    @Inject
    Event<AllExecutionsDoneEvent> allExecutionsDoneEvent;

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
                    workhorseConfig.getJobQueuePusherPoll(), TimeUnit.SECONDS);

            log.trace("Job queue pusher started with a {} seconds interval", workhorseConfig.getJobQueuePusherPoll());

        } else {
            scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(this::poll, 0,
                    workhorseConfig.getJobQueuePollerInterval(), TimeUnit.SECONDS);
            log.trace("Job queue poller started with a {} seconds interval",
                    workhorseConfig.getJobQueuePollerInterval());
        }

    }

    /**
     * poll the Execution with an given intervall
     */
    void poll() {
        for (Job job : jobPersistence.getAllByStatus(JobStatus.ACTIVE)) {

            if (executionBuffer.getNumberOfExecution(job.getId()) < workhorseConfig.getJobQueueMin()) {
                List<Execution> executions = executionPersistence.pollNextExecutions(job.getId(),
                        workhorseConfig.getJobQueueMax());
                for (Execution execution : executions) {
                    if (execution == null) {
                        continue;
                    }
                    if (!executionDistributor(execution)) {
                        // TODO
                    }
                }
            }
            log.trace("Number of job execution in buffer: {}", executionBuffer.getNumberOfExecution(job.getId()));
        }

    }

    /**
     * recieve the notification about new persisted job execution
     * 
     * @param newExecutionEvent
     */
    public void push(@ObservesAsync NewExecutionEvent newExecutionEvent) {
        if (!executionPersistence.isPusherAvailable() || scheduledFuture == null) {
            return;
        }
        // log.trace("New Job Execution pushed: " + newExecutionEvent);
        if (executionBuffer.getNumberOfExecution(newExecutionEvent.jobId) < workhorseConfig.getJobQueueMax()) {
            Execution execution = executionPersistence.getById(newExecutionEvent.jobId, newExecutionEvent.executionId);
            if (execution != null) {
                if (execution.getMaturity() != null) {
                    long delayInSeconds = ChronoUnit.SECONDS.between(workhorseConfig.timestamp(),
                            execution.getMaturity());

                    log.trace("Job Execution : {} will be process in {} seconds", execution, delayInSeconds);
                    scheduledExecutorService.schedule(() -> {
                        executionDistributor(execution);
                    }, delayInSeconds, TimeUnit.SECONDS);
                    // Only the head of a chainExecution may integrate the executionBuffer
                } else if (execution.getChainedPreviousExecutionId() == null) {
                    executionDistributor(execution);
                }
            } else {
                log.error("No job execution found for executionId: {} ", newExecutionEvent.jobId);
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
                log.trace("Job queue pusher stopped");
            } else {
                log.trace("Job queue poller stopped");
            }

        } else {
            if (executionPersistence.isPusherAvailable()) {
                log.trace("Job queue pusher cann't be stopped because it's not currently running!");
            } else {
                log.trace("Job queue poller cann't be stopped because it's not currently running!");
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
     * add a job execution in the executionQueue of the correspondent job
     * 
     * @param execution Job Execution to map with a executionQueue
     * @return true if the mapping was successful and false otherwise
     */
    public boolean executionDistributor(Execution execution) {

        if (execution == null || execution.getStatus() != ExecutionStatus.QUEUED) {
            return false;
        }

        Long jobId = execution.getJobId();

        if (!executionBuffer.isAddable(jobId, execution.getId())) {
            return false;
        }

        final int numberOfExecutions = executionBuffer.getNumberOfExecution(jobId);

        if (numberOfExecutions == 0) {
            executionBuffer.addJobStartTimes(jobId, System.currentTimeMillis());
        }

        final ReentrantLock lock = executionBuffer.getLock(jobId);
        try {
            lock.lock();

            if (Boolean.TRUE.equals(execution.getPriority())) {
                executionBuffer.addPriorityExecution(jobId, execution.getId());
            } else {
                executionBuffer.addExecution(jobId, execution.getId());
            }

            log.trace("New Execution: {} in Queue. Number of Executions in Queue: {} ", execution, numberOfExecutions);
        } finally {
            lock.unlock();
        }

        log.trace(" Numbers of running's jobThreads : {}", executionBuffer.getRunningJobThreadCounts(jobId));
        if (executionBuffer.getJobThreadCounts(jobId) > executionBuffer.getRunningJobThreadCounts(jobId)) {
            // lock = executionQueue.getLock(jobId);
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
            log.error("The job {} is not ACTIVE ", job);
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

            jobErrorEvent.fire(new JobErrorEvent(exception, ErrorType.JOB_THREAD_ERROR.getMessage(), job.getId(),
                    JobStatus.ERROR));
            return job;
        });

        completion.thenApply(fn -> {
            executionBuffer.removeCompletionStage(jobId, completion);
            log.trace("Thread is really over: {} ", fn);
            return this;
        });

    }

    public void allExecutionsDoneEvent(@Observes AllExecutionsDoneEvent event) {
        final Job job = event.getJob();

        Long startTime = executionBuffer.getJobStartTimes(job.getId());
        if (startTime != null) {
            long durationMillis = System.currentTimeMillis() - startTime;

            final String durationText = String.format("%d min, %d sec", TimeUnit.MILLISECONDS.toMinutes(durationMillis),
                    TimeUnit.MILLISECONDS.toSeconds(durationMillis)
                            - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(durationMillis)));
            final String message = "Duration of all " + job.getName() + " job executions: " + durationText;

            log.trace(message + durationMillis);

            executionBuffer.removeJobStartTimes(job.getId());
        }
    }

}
