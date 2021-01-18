package io.coodoo.workhorse.core.boundary;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.coodoo.workhorse.api.DTO.ExecutionInfo;
import io.coodoo.workhorse.api.DTO.GroupInfo;
import io.coodoo.workhorse.config.boundary.WorkhorseConfigService;
import io.coodoo.workhorse.config.entity.WorkhorseConfig;
import io.coodoo.workhorse.core.control.ExecutionBuffer;
import io.coodoo.workhorse.core.control.JobScheduler;
import io.coodoo.workhorse.core.control.Workhorse;
import io.coodoo.workhorse.core.control.WorkhorseController;
import io.coodoo.workhorse.core.control.event.RestartWorkhorseEvent;
import io.coodoo.workhorse.core.entity.Execution;
import io.coodoo.workhorse.core.entity.ExecutionStatus;
import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.core.entity.JobStatus;
import io.coodoo.workhorse.core.entity.WorkhorseInfo;
import io.coodoo.workhorse.persistence.PersistenceManager;
import io.coodoo.workhorse.persistence.interfaces.PersistenceTyp;
import io.coodoo.workhorse.util.CronExpression;

@ApplicationScoped
public class WorkhorseService {

    private static final Logger log = Logger.getLogger(WorkhorseService.class);

    @Inject
    Workhorse workhorse;

    @Inject
    ExecutionBuffer jobExecutionQueue;

    @Inject
    PersistenceManager persistenceManager;

    @Inject
    WorkhorseLogService workhorseLogService;

    @Inject
    WorkhorseConfig jobEngineConfig;

    @Inject
    WorkhorseController workhorseController;

    @Inject
    WorkhorseConfigService jobEngineConfigService;

    @Inject
    JobScheduler jobScheduler;

    /**
     * Start the Job Engine with a peristence
     * 
     * @param persistenceTyp Type of the persistence to use
     * @param persistenceConfiguration Config data to connect the persistence
     */
    public void startTheEngine(PersistenceTyp persistenceTyp, Object persistenceConfiguration) {

        persistenceManager.initializePersistence(persistenceTyp, persistenceConfiguration);
        jobEngineConfigService.initializeJobEngineConfig();
        workhorseController.loadJobWorkers();
        jobExecutionQueue.initializeBuffer();
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
    public void startTheEngine(PersistenceTyp persistenceTyp, Object persistenceConfiguration, String timeZone, int jobQueuePollerInterval,
                    int jobQueuePusherPoll, Long jobQueueMax, int jobQueueMin) {

        persistenceManager.initializePersistence(persistenceTyp, persistenceConfiguration);

        jobEngineConfigService.initializeJobEngineConfig(timeZone, jobQueuePollerInterval, jobQueuePusherPoll, jobQueueMax, jobQueueMin, persistenceTyp);

        workhorseController.loadJobWorkers();
        jobExecutionQueue.initializeBuffer();
        workhorse.start();
        jobScheduler.startScheduler();
    }

    /**
     * Restart the Workhorse on an event
     * 
     * @param restartPayload
     */
    public void startTheEngine(@ObservesAsync RestartWorkhorseEvent restartPayload) {

        workhorse.stop();
        jobScheduler.stopScheduler();
        jobExecutionQueue.destroyQueue();
        persistenceManager.destroyStorage();

        log.info("The job engine will be restart.");
        startTheEngine(restartPayload.getJobEngine().getPersistenceTyp(), restartPayload.getPersistenceParams(), restartPayload.getJobEngine().getTimeZone(),
                        restartPayload.getJobEngine().getJobQueuePollerInterval(), restartPayload.getJobEngine().getJobQueuePusherPoll(),
                        restartPayload.getJobEngine().getJobQueueMax(), restartPayload.getJobEngine().getJobQueueMin());
        log.info("End of the restart of the job engine.");
    }

    /**
     * Start the Job Engine
     */
    public void startTheEngine() {
        startTheEngine(jobEngineConfig.getPersistenceTyp(), null);
    }

    /**
     * Stop the Workhorse
     */
    public void stopTheEngine() {
        workhorse.stop();
        for (Job job : getAllScheduledJobs()) {
            jobScheduler.stop(job);
            jobExecutionQueue.cancelProcess(job);
        }

        jobExecutionQueue.destroyQueue();
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
    public WorkhorseInfo getJobEngineInfo(Long jobId) {
        return jobExecutionQueue.getInfo(jobId);
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
     * Retrieves a JobExecution by Id
     * 
     * @return JobExecution
     */
    public Execution getJobExecutionById(Long jobId, Long jobExecutionId) {
        return workhorseController.getJobExecutionById(jobId, jobExecutionId);
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
        jobExecutionQueue.cancelProcess(job);

        workhorseController.updateJob(jobId, name, description, workerClassName, schedule, status, threads, maxPerMinute, failRetries, retryDelay,
                        daysUntilCleanUp, uniqueInQueue);

        jobExecutionQueue.initializeBuffer(job);
        // workhorse.start();
        jobScheduler.start(job);
        return job;

    }

    /**
     * Update a JobExecution
     * 
     * @return JobExecution
     */
    public Execution createJobExecution(Long jobId, String parameters, Boolean priority, LocalDateTime maturity, Long batchId, Long chainId,
                    Long chainedPreviousExecutionId, boolean uniqueInQueue) {
        return workhorseController.createJobExecution(jobId, parameters, priority, maturity, batchId, chainId, chainedPreviousExecutionId, uniqueInQueue);
    }

    public Execution updateJobExecution(Long jobId, Long jobExecutionId, ExecutionStatus status, String parameters, boolean priority, LocalDateTime maturity,
                    int fails) {

        Execution jobExecution = getJobExecutionById(jobId, jobExecutionId);

        if (ExecutionStatus.QUEUED == jobExecution.getStatus() && ExecutionStatus.QUEUED != status) {
            jobExecutionQueue.removeFromBuffer(jobExecution);
        }
        jobExecution.setStatus(status);
        jobExecution.setParameters(parameters);
        jobExecution.setPriority(priority);
        jobExecution.setMaturity(maturity);
        jobExecution.setFailRetry(fails);
        log.info("JobExecution updated: " + jobExecution);

        workhorseController.updateJobExecution(jobId, jobExecutionId, jobExecution);
        return jobExecution;
    }

    public void deleteJobExecution(Long jobId, Long jobExecutionId) {

        Execution jobExecution = getJobExecutionById(jobId, jobExecutionId);

        if (jobExecution == null) {
            log.info("JobExecution does not exist: " + jobExecution);
            return;
        }

        if (Objects.equals(ExecutionStatus.QUEUED, jobExecution.getStatus())) {
            jobExecutionQueue.removeFromBuffer(jobExecution);
        }

        log.info("JobExecution removed: " + jobExecution);

        workhorseController.deleteJobExecution(jobId, jobExecutionId);

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
            jobExecutionQueue.cancelProcess(job);
        }
    }

    public GroupInfo getJobExecutionBatchInfo(Long jobId, Long batchId) {

        List<Execution> batchExecutions = workhorseController.getBatch(jobId, batchId);

        List<ExecutionInfo> batchInfo = batchExecutions.stream()
                        .map(JobExecution -> new ExecutionInfo(JobExecution.getId(), JobExecution.getStatus(), JobExecution.getStartedAt(),
                                        JobExecution.getEndedAt(), JobExecution.getDuration(), JobExecution.getFailRetryJobExecutionId()))
                        .collect(Collectors.toList());

        return new GroupInfo(batchId, batchInfo);
    }

    public List<Execution> getJobExecutionBatch(Long jobId, Long batchId) {
        return workhorseController.getBatch(jobId, batchId);
    }

    public GroupInfo getJobExecutionChainInfo(Long jobId, Long chainId) {
        List<Execution> chainExecutions = workhorseController.getchain(jobId, chainId);
        List<ExecutionInfo> batchInfo = chainExecutions.stream()
                        .map(JobExecution -> new ExecutionInfo(JobExecution.getId(), JobExecution.getStatus(), JobExecution.getStartedAt(),
                                        JobExecution.getEndedAt(), JobExecution.getDuration(), JobExecution.getFailRetryJobExecutionId()))
                        .collect(Collectors.toList());
        return new GroupInfo(chainId, batchInfo);
    }

    public List<Execution> getJobExecutionChain(Long jobId, Long chainId) {
        return workhorseController.getchain(jobId, chainId);
    }

    public List<LocalDateTime> getNextScheduledTimes(String schedule, int times, LocalDateTime startTime) {
        List<LocalDateTime> nextScheduledTimes = new ArrayList<>();
        if (schedule == null) {
            return nextScheduledTimes;
        }

        CronExpression cronExpression = new CronExpression(schedule);
        LocalDateTime nextScheduledTime = startTime != null ? startTime : jobEngineConfig.timestamp();

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
        LocalDateTime scheduledTime = startTime != null ? startTime : jobEngineConfig.timestamp();
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

    public void triggerScheduledJobExecutionCreation(Job job) throws ClassNotFoundException {
        workhorseController.triggerScheduledJobExecutionCreation(job);
    }

}
