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
import io.coodoo.workhorse.core.entity.ExecutionFailStatus;
import io.coodoo.workhorse.core.entity.ExecutionStatus;
import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.core.entity.JobStatus;
import io.coodoo.workhorse.persistence.PersistenceManager;
import io.coodoo.workhorse.persistence.interfaces.ExecutionPersistence;
import io.coodoo.workhorse.persistence.interfaces.JobPersistence;
import io.coodoo.workhorse.persistence.interfaces.qualifier.ExecutionQualifier;
import io.coodoo.workhorse.persistence.interfaces.qualifier.JobQualifier;
import io.coodoo.workhorse.util.WorkhorseUtil;

/**
 * @author coodoo GmbH (coodoo.io)
 */
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
    WorkhorseController workhorseController;

    @Inject
    Event<Job> jobThreadManager;

    @Inject
    Event<JobErrorEvent> jobErrorEvent;

    @Inject
    Event<AllExecutionsDoneEvent> allExecutionsDoneEvent;

    protected ScheduledExecutorService scheduledExecutorService;

    /**
     * Stores the poll scheduler object. There is always one instance of this scheduler if the engine is active.
     */
    protected ScheduledFuture<?> scheduledFuture;

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
        // Check if the engine is already started. If so stop first and start again.
        if (scheduledFuture != null) {
            stop();
        }

        if (executionPersistence.isPusherAvailable()) {
            scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(this::poll, 0, StaticConfig.BUFFER_PUSH_FALL_BACK_POLL_INTERVAL, TimeUnit.SECONDS);

            log.trace("Job queue pusher started. Backup poller started with a {} seconds interval", StaticConfig.BUFFER_PUSH_FALL_BACK_POLL_INTERVAL);

        } else {
            scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(this::poll, 0, StaticConfig.BUFFER_POLL_INTERVAL, TimeUnit.SECONDS);
            log.trace("Job queue poller started with a {} seconds interval", StaticConfig.BUFFER_POLL_INTERVAL);
        }

    }

    /**
     * poll the Execution with an given intervall
     */
    void poll() {
        for (Job job : jobPersistence.getAllByStatus(JobStatus.ACTIVE)) {

            if (executionBuffer.getNumberOfExecution(job.getId()) < StaticConfig.BUFFER_MIN) {
                List<Execution> executions = executionPersistence.pollNextExecutions(job.getId(), StaticConfig.BUFFER_MAX);
                for (Execution execution : executions) {
                    if (execution == null) {
                        continue;
                    }
                    executionDistributor(execution);
                }
            }
            log.trace("Number of job execution in buffer: {}", executionBuffer.getNumberOfExecution(job.getId()));
        }

    }

    /**
     * Receive the notification about new persisted execution
     * 
     * @param newExecutionEvent describes the newly persisted execution
     */
    public void push(@ObservesAsync NewExecutionEvent newExecutionEvent) {
        if (!executionPersistence.isPusherAvailable() || scheduledFuture == null) {
            return;
        }
        log.trace("New Execution pushed: {}", newExecutionEvent);
        if (executionBuffer.getNumberOfExecution(newExecutionEvent.jobId) < StaticConfig.BUFFER_MAX) {
            Execution execution = executionPersistence.getById(newExecutionEvent.jobId, newExecutionEvent.executionId);
            if (execution == null) {
                log.error("No execution found for executionId: {} ", newExecutionEvent.executionId);
                return;
            }
            if (execution.getStatus() == ExecutionStatus.PLANNED) {
                long delayInSeconds = ChronoUnit.SECONDS.between(WorkhorseUtil.timestamp(), execution.getPlannedFor());
                scheduledExecutorService.schedule(() -> {
                    executionDistributor(execution);
                }, delayInSeconds, TimeUnit.SECONDS);
                log.trace("Execution {} will be process in {} seconds", execution.getId(), delayInSeconds);
            } else {
                executionDistributor(execution);
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
     * Add an execution in the executionBuffer of the correspondent job
     * 
     * @param execution Execution to map with a executionBuffer
     * @return true if the mapping was successful and false otherwise
     */
    public boolean executionDistributor(Execution execution) {

        if (execution.getChainId() != null && !execution.getId().equals(execution.getChainId())) {
            // Only the head of a chainExecution may integrate the executionBuffer
            return false;
        }

        switch (execution.getStatus()) {
            // Execution in status PLANNED have to be updated to QUEUED before being added
            // to the buffer.
            case PLANNED:
                workhorseController.updateExecutionStatus(execution.getJobId(), execution.getId(), ExecutionStatus.QUEUED);
            case QUEUED:
                // If the execution is 'expired' these don't have to be processed.
                if (execution.getExpiresAt() != null && execution.getExpiresAt().isBefore(WorkhorseUtil.timestamp())) {
                    workhorseController.setExecutionStatusToFailed(execution, ExecutionFailStatus.EXPIRED);
                    return false;
                }
                break;
            default:
                // Only QUEUED and PLANNED executions are permitted!
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

            if (execution.isPriority()) {
                executionBuffer.addPriorityExecution(jobId, execution.getId());
            } else {
                executionBuffer.addExecution(jobId, execution.getId());
            }

            log.trace("New Execution: {} in Queue. Number of Executions in Queue: {} ", execution, numberOfExecutions);
        } finally {
            lock.unlock();
        }

        log.trace("Numbers of running's jobThreads: {}", executionBuffer.getRunningJobThreadCounts(jobId));
        if (executionBuffer.getJobThreadCounts(jobId) > executionBuffer.getRunningJobThreadCounts(jobId)) {
            // lock = executionQueue.getLock(jobId);
            try {
                lock.lock();
                for (int i = executionBuffer.getRunningJobThreadCounts(jobId); i < executionBuffer.getJobThreadCounts(jobId); i++) {
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
            jobPersistence.update(job);

            executionBuffer.cancelProcess(job);

            jobErrorEvent.fire(new JobErrorEvent(exception, ErrorType.JOB_THREAD_ERROR.getMessage(), job.getId(), JobStatus.ERROR));
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
                            TimeUnit.MILLISECONDS.toSeconds(durationMillis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(durationMillis)));
            final String message = "Duration of all " + job.getName() + " job executions: " + durationText;

            log.trace(message + durationMillis);

            executionBuffer.removeJobStartTimes(job.getId());
        }
    }

}
