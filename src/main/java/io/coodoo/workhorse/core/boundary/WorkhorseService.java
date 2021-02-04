package io.coodoo.workhorse.core.boundary;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.coodoo.workhorse.api.DTO.ExecutionInfo;
import io.coodoo.workhorse.api.DTO.GroupInfo;
import io.coodoo.workhorse.core.control.ExecutionBuffer;
import io.coodoo.workhorse.core.control.JobScheduler;
import io.coodoo.workhorse.core.control.Workhorse;
import io.coodoo.workhorse.core.control.WorkhorseConfigService;
import io.coodoo.workhorse.core.control.WorkhorseController;
import io.coodoo.workhorse.core.control.event.RestartWorkhorseEvent;
import io.coodoo.workhorse.core.entity.Execution;
import io.coodoo.workhorse.core.entity.ExecutionStatus;
import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.core.entity.JobStatus;
import io.coodoo.workhorse.core.entity.WorkhorseConfig;
import io.coodoo.workhorse.core.entity.WorkhorseInfo;
import io.coodoo.workhorse.persistence.PersistenceManager;
import io.coodoo.workhorse.persistence.interfaces.PersistenceTyp;
import io.coodoo.workhorse.util.CronExpression;

/**
 * Central Workhorse API-Service
 * 
 * @author coodoo GmbH (coodoo.io)
 */
@ApplicationScoped
public class WorkhorseService {

    private static final Logger log = Logger.getLogger(WorkhorseService.class);

    @Inject
    Workhorse workhorse;

    @Inject
    ExecutionBuffer executionBuffer;

    @Inject
    PersistenceManager persistenceManager;

    @Inject
    WorkhorseLogService workhorseLogService;

    @Inject
    WorkhorseController workhorseController;

    @Inject
    WorkhorseConfigService workhorseConfigService;

    @Inject
    WorkhorseConfig workhorseConfig;

    @Inject
    JobScheduler jobScheduler;

    /**
     * Start the Job Engine with a peristence
     * 
     * @param persistenceTyp Type of the persistence to use
     * @param persistenceConfiguration Config data to connect the persistence
     */
    @Deprecated
    public void startTheEngine(PersistenceTyp persistenceTyp, Object persistenceConfiguration) {

        persistenceManager.initializePersistence(persistenceTyp, persistenceConfiguration);
        workhorseConfigService.initializeConfig();
        workhorseController.loadWorkers();
        executionBuffer.initializeBuffer();
        workhorse.start();
        jobScheduler.startScheduler();

    }

    /**
     * Start the Job Engine with given config and persistence params
     * 
     * @param persistenceTyp Typ of the choosen persistence
     * @param persistenceConfiguration config parameter for the choosen persistence
     * @param timeZone ZoneId Object time zone for LocalDateTime instance creation. Default is UTC
     * @param jobQueuePollerInterval Job queue poller interval in seconds
     * @param jobQueuePusherPoll poll interval to use when using Pushable persistence
     * @param jobQueueMax Max amount of executions to load into the memory queue per job
     * @param jobQueueMin Min amount of executions in memory queue before the poller gets to add more
     */
    @Deprecated
    public void startTheEngine(PersistenceTyp persistenceTyp, Object persistenceConfiguration, String timeZone, int jobQueuePollerInterval,
                    int jobQueuePusherPoll, Long jobQueueMax, int jobQueueMin) {

        persistenceManager.initializePersistence(persistenceTyp, persistenceConfiguration);

        workhorseConfigService.initializeConfig(timeZone, jobQueuePollerInterval, jobQueuePusherPoll, jobQueueMax, jobQueueMin, persistenceTyp);

        workhorseController.loadWorkers();
        executionBuffer.initializeBuffer();
        workhorse.start();
        jobScheduler.startScheduler();
    }

    /**
     * Restart the Workhorse on an event
     * 
     * @param restartWorkhorseEvent
     */
    @Deprecated
    public void startTheEngine(@ObservesAsync RestartWorkhorseEvent restartWorkhorseEvent) {

        workhorse.stop();
        jobScheduler.stopScheduler();
        executionBuffer.destroyQueue();
        persistenceManager.destroyStorage();

        log.info("The job engine will be restart.");

        WorkhorseConfig config = restartWorkhorseEvent.getWorkhorseConfig();
        startTheEngine(config.getPersistenceTyp(), restartWorkhorseEvent.getPersistenceParams(), config.getTimeZone(), config.getJobQueuePollerInterval(),
                        config.getJobQueuePusherPoll(), config.getJobQueueMax(), config.getJobQueueMin());
        log.info("End of the restart of the job engine.");
    }

    /**
     * Start the Job Engine
     */
    @Deprecated
    public void startTheEngine() {

        startTheEngine(workhorseConfig.getPersistenceTyp(), null);
    }

    /**
     * Stop the Workhorse
     */
    @Deprecated
    public void stopTheEngine() {
        workhorse.stop();
        for (Job job : getAllScheduledJobs()) {
            jobScheduler.stop(job);
            executionBuffer.cancelProcess(job);
        }

        executionBuffer.destroyQueue();
    }

    public void start() {

        if (workhorse.isRunning()) {
            return;
        }
        // TODO oder
        if (workhorse.isRunning()) {
            stopTheEngine();
        }
        // TODO start
    }

    public void stop() {

        if (!workhorse.isRunning()) {
            return;
        }
        // TODO stopp
    }

    public WorkhorseConfig getWorkhorseConfig() {
        // TODO
        return null;
    }

    public void updateWorkhorseConfig(WorkhorseConfig workhorseConfig) {
        // TODO
    }

    /**
     * Retrieves all jobs
     * 
     * @return List of Job
     */
    public List<Job> getAllJobs() {
        return workhorseController.getAllJobs();
    }

    /**
     * 
     */
    public WorkhorseInfo getWorkhorseInfo(Long jobId) {
        return executionBuffer.getInfo(jobId);
    }

    /**
     * Check if the job engine is running
     * 
     * @return boolean
     */
    public boolean isRunning() {
        return workhorse.isRunning();
    }

    /**
     * Retrieves a {@link Execution} by Id
     */
    public Execution getExecutionById(Long jobId, Long executionId) {
        return workhorseController.getExecutionById(jobId, executionId);
    }

    /**
     * Retrieves a Job by Id
     * 
     * @return Job
     */
    public Job getJobById(Long jobId) {
        return workhorseController.getJobById(jobId);
    }

    /**
     * Update a job
     * 
     * @return Job
     */
    public Job updateJob(Long jobId, String name, String description, String workerClassName, String schedule, JobStatus status, int threads,
                    Integer maxPerMinute, int failRetries, int retryDelay, int daysUntilCleanUp, boolean uniqueInQueue) {

        Job job = getJobById(jobId);

        jobScheduler.stop(job);
        // workhorse.stop(); maybe we don t need. To proove
        executionBuffer.cancelProcess(job);

        workhorseController.updateJob(jobId, name, description, workerClassName, schedule, status, threads, maxPerMinute, failRetries, retryDelay,
                        daysUntilCleanUp, uniqueInQueue);

        executionBuffer.initializeBuffer(job);
        // workhorse.start();
        jobScheduler.start(job);
        return job;

    }

    /**
     * Update a {@link Execution}
     */
    public Execution createExecution(Long jobId, String parameters, Boolean priority, LocalDateTime maturity, Long batchId, Long chainId,
                    Long chainedPreviousExecutionId, boolean uniqueInQueue) {
        return workhorseController.createExecution(jobId, parameters, priority, maturity, batchId, chainId, chainedPreviousExecutionId, uniqueInQueue);
    }

    public Execution updateExecution(Long jobId, Long executionId, ExecutionStatus status, String parameters, boolean priority, LocalDateTime maturity,
                    int fails) {

        Execution execution = getExecutionById(jobId, executionId);

        if (ExecutionStatus.QUEUED == execution.getStatus() && ExecutionStatus.QUEUED != status) {
            executionBuffer.removeFromBuffer(execution);
        }
        execution.setStatus(status);
        execution.setParameters(parameters);
        execution.setPriority(priority);
        execution.setMaturity(maturity);
        execution.setFailRetry(fails);
        log.info("Execution updated: " + execution);

        workhorseController.updateExecution(jobId, executionId, execution);
        return execution;
    }

    public void deleteExecution(Long jobId, Long executionId) {

        Execution execution = getExecutionById(jobId, executionId);
        if (execution == null) {
            log.info("Execution does not exist: " + execution);
            return;
        }
        if (execution.getStatus() == ExecutionStatus.QUEUED) {
            executionBuffer.removeFromBuffer(execution);
        }
        log.info("Execution removed: " + execution);

        workhorseController.deleteExecution(jobId, executionId);
    }

    public void activateJob(Long jobId) {

        Job job = getJobById(jobId);
        if (job == null) {
            throw new RuntimeException("Job not found");
        }
        log.info("Activate job " + job.getName());
        job.setStatus(JobStatus.ACTIVE);
        workhorseController.update(job.getId(), job);
        if (job.getSchedule() != null && !job.getSchedule().isEmpty()) {
            jobScheduler.start(job);
        }
    }

    public void deactivateJob(Long jobId) {
        Job job = getJobById(jobId);
        if (job == null) {
            throw new RuntimeException("Job not found");
        }
        log.info("Deactivate job " + job.getName());
        job.setStatus(JobStatus.INACTIVE);
        workhorseController.update(job.getId(), job);
        if (job.getSchedule() != null && !job.getSchedule().isEmpty()) {
            jobScheduler.stop(job);
            executionBuffer.cancelProcess(job);
        }
    }

    public GroupInfo getExecutionBatchInfo(Long jobId, Long batchId) {

        List<Execution> batchExecutions = workhorseController.getBatch(jobId, batchId);

        List<ExecutionInfo> batchInfo =
                        batchExecutions.stream()
                                        .map(execution -> new ExecutionInfo(execution.getId(), execution.getStatus(), execution.getStartedAt(),
                                                        execution.getEndedAt(), execution.getDuration(), execution.getFailRetryExecutionId()))
                                        .collect(Collectors.toList());

        return new GroupInfo(batchId, batchInfo);
    }

    public List<Execution> getExecutionBatch(Long jobId, Long batchId) {
        return workhorseController.getBatch(jobId, batchId);
    }

    public GroupInfo getExecutionChainInfo(Long jobId, Long chainId) {
        List<Execution> chainExecutions = workhorseController.getchain(jobId, chainId);
        List<ExecutionInfo> batchInfo =
                        chainExecutions.stream()
                                        .map(execution -> new ExecutionInfo(execution.getId(), execution.getStatus(), execution.getStartedAt(),
                                                        execution.getEndedAt(), execution.getDuration(), execution.getFailRetryExecutionId()))
                                        .collect(Collectors.toList());
        return new GroupInfo(chainId, batchInfo);
    }

    public List<Execution> getExecutionChain(Long jobId, Long chainId) {
        return workhorseController.getchain(jobId, chainId);
    }

    public List<LocalDateTime> getNextScheduledTimes(String schedule, int times, LocalDateTime startTime) {
        List<LocalDateTime> nextScheduledTimes = new ArrayList<>();
        if (schedule == null) {
            return nextScheduledTimes;
        }

        CronExpression cronExpression = new CronExpression(schedule);
        LocalDateTime nextScheduledTime = startTime != null ? startTime : workhorseConfig.timestamp();

        for (int i = 0; i < times; i++) {
            nextScheduledTime = cronExpression.nextTimeAfter(nextScheduledTime);
            nextScheduledTimes.add(nextScheduledTime);
        }
        return nextScheduledTimes;
    }

    public List<Job> getAllScheduledJobs() {
        return workhorseController.getAllScheduledJobs();
    }

    /**
     * Get the execution times defined by {@link Job#getSchedule()}
     * 
     * @param schedule CRON Expression
     * @param startTime start time for this request (if <tt>null</tt> then current time is used)
     * @param endTime end time for this request (if <tt>null</tt> then current time plus 1 day is used)
     * @return List of {@link LocalDateTime} representing the execution times of a scheduled job between the <tt>startTime</tt> and <tt>endTime</tt>
     */
    public List<LocalDateTime> getScheduledTimes(String schedule, LocalDateTime startTime, LocalDateTime endTime) {

        List<LocalDateTime> scheduledTimes = new ArrayList<>();
        if (schedule == null) {
            return scheduledTimes;
        }

        CronExpression cronExpression = new CronExpression(schedule);
        LocalDateTime scheduledTime = startTime != null ? startTime : workhorseConfig.timestamp();
        LocalDateTime endOfTimes = endTime != null ? endTime : scheduledTime.plusDays(1);

        while (scheduledTime.isBefore(endOfTimes)) {
            scheduledTime = cronExpression.nextTimeAfter(scheduledTime);
            scheduledTimes.add(scheduledTime);
        }
        return scheduledTimes;
    }

    public List<Execution> getExecutions(Long jobId) {
        return workhorseController.getExecutions(jobId);
    }

    public boolean stopScheduledJob(Long jobId) {
        Job job = workhorseController.getJobById(jobId);
        if (job != null && job.getSchedule() != null && !job.getSchedule().isEmpty()) {
            jobScheduler.stop(job);
            return true;
        }

        throw new RuntimeException("No Job for JobId found");
    }

    public void triggerScheduledExecutionCreation(Job job) throws ClassNotFoundException {
        workhorseController.triggerScheduledExecutionCreation(job);
    }

}
