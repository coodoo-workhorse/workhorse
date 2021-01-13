package io.coodoo.workhorse.jobengine.boundary;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.coodoo.workhorse.api.DTO.GroupInfo;
import io.coodoo.workhorse.api.DTO.JobEngineInfo;
import io.coodoo.workhorse.api.DTO.JobExecutionInfo;
import io.coodoo.workhorse.config.boundary.JobEngineConfigService;
import io.coodoo.workhorse.config.entity.JobEngineConfig;
import io.coodoo.workhorse.jobengine.control.JobEngine;
import io.coodoo.workhorse.jobengine.control.JobEngineController;
import io.coodoo.workhorse.jobengine.control.JobExecutionBuffer;
import io.coodoo.workhorse.jobengine.control.JobScheduler;
import io.coodoo.workhorse.jobengine.control.events.RestartTheJobEngine;
import io.coodoo.workhorse.jobengine.entity.Job;
import io.coodoo.workhorse.jobengine.entity.JobExecution;
import io.coodoo.workhorse.jobengine.entity.JobExecutionStatus;
import io.coodoo.workhorse.jobengine.entity.JobStatus;
import io.coodoo.workhorse.log.control.JobEngineLogControl;
import io.coodoo.workhorse.storage.PersistenceManager;
import io.coodoo.workhorse.storage.persistenceInterface.PersistenceTyp;
import io.coodoo.workhorse.util.CronExpression;

@ApplicationScoped
public class JobEngineService {

    private static final Logger log = Logger.getLogger(JobEngineService.class);

    @Inject
    JobEngine jobEngine;

    @Inject
    JobExecutionBuffer jobExecutionQueue;

    @Inject
    PersistenceManager persistenceManager;

    @Inject
    JobEngineLogControl jobEngineLogControl;

    @Inject
    JobEngineConfig jobEngineConfig;

    @Inject
    JobEngineController jobEngineController;

    @Inject
    JobEngineConfigService jobEngineConfigService;

    @Inject
    JobScheduler jobScheduler;

    /**
     * Start the Job Engine with a peristence
     * 
     * @param persistenceTyp           Type of the persistence to use
     * @param persistenceConfiguration Config data to connect the persistence
     */
    public void startTheEngine(PersistenceTyp persistenceTyp, Object persistenceConfiguration) {

        persistenceManager.initializePersistence(persistenceTyp, persistenceConfiguration);
        jobEngineConfigService.initializeJobEngineConfig();
        jobEngineController.loadJobWorkers();
        jobExecutionQueue.initializeBuffer();
        jobEngine.start();
        jobScheduler.startScheduler();

    }

    /**
     * Start the Job Engine with given config and persistence params
     * 
     * @param persistenceTyp           Typ of the choosen persistence
     * @param persistenceConfiguration config parameter for the choosen persistence
     * @param timeZone                 ZoneId Object time zone for LocalDateTime
     *                                 instance creation. Default is UTC
     * @param jobQueuePollerInterval   Job queue poller interval in seconds
     * @param jobQueuePusherPoll       poll interval to use when using Pushable
     *                                 persistence
     * @param jobQueueMax              Max amount of executions to load into the
     *                                 memory queue per job
     * @param jobQueueMin              Min amount of executions in memory queue
     *                                 before the poller gets to add more
     */
    public void startTheEngine(PersistenceTyp persistenceTyp, Object persistenceConfiguration, String timeZone,
            int jobQueuePollerInterval, int jobQueuePusherPoll, Long jobQueueMax, int jobQueueMin) {

        persistenceManager.initializePersistence(persistenceTyp, persistenceConfiguration);

        jobEngineConfigService.initializeJobEngineConfig(timeZone, jobQueuePollerInterval, jobQueuePusherPoll,
                jobQueueMax, jobQueueMin, persistenceTyp);

        jobEngineController.loadJobWorkers();
        jobExecutionQueue.initializeBuffer();
        jobEngine.start();
        jobScheduler.startScheduler();
    }

    /**
     * Restart the JobEngine on an event
     * 
     * @param restartPayload
     */
    public void startTheEngine(@ObservesAsync RestartTheJobEngine restartPayload) {

        jobEngine.stop();
        jobScheduler.stopScheduler();
        jobExecutionQueue.destroyQueue();
        persistenceManager.destroyStorage();

        log.info("The job engine will be restart.");
        startTheEngine(restartPayload.getJobEngine().getPersistenceTyp(), restartPayload.getPersistenceParams(),
                restartPayload.getJobEngine().getTimeZone(), restartPayload.getJobEngine().getJobQueuePollerInterval(),
                restartPayload.getJobEngine().getJobQueuePusherPoll(), restartPayload.getJobEngine().getJobQueueMax(),
                restartPayload.getJobEngine().getJobQueueMin());
        log.info("End of the restart of the job engine.");
    }

    /**
     * Start the Job Engine
     */
    public void startTheEngine() {
        startTheEngine(jobEngineConfig.getPersistenceTyp(), null);
    }

    /**
     * Stop the JobEngine
     */
    public void stopTheEngine() {
        jobEngine.stop();
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
        return jobEngineController.getAllJobs();
    }

    /**
     * 
     */
    public JobEngineInfo getJobEngineInfo(Long jobId) {
        return jobExecutionQueue.getInfo(jobId);
    }

    /**
     * Check if the job engine is running
     * 
     * @return boolean
     */
    public boolean isRunning() {
        return jobEngine.isRunning();
    }

    /**
     * Retrieves a JobExecution by Id
     * 
     * @return JobExecution
     */
    public JobExecution getJobExecutionById(Long jobId, Long jobExecutionId) {
        return jobEngineController.getJobExecutionById(jobId, jobExecutionId);
    }

    /**
     * Retrieves a Job by Id
     * 
     * @return Job
     */
    public Job getJobById(Long jobId) {
        return jobEngineController.getJobById(jobId);
    }

    /**
     * Update a job
     * 
     * @return Job
     */
    public Job updateJob(Long jobId, String name, String description, String workerClassName, String schedule,
            JobStatus status, int threads, Integer maxPerMinute, int failRetries, int retryDelay, int daysUntilCleanUp,
            boolean uniqueInQueue) {

        Job job = getJobById(jobId);

        jobScheduler.stop(job);
        // jobEngine.stop(); maybe we don t need. To proove
        jobExecutionQueue.cancelProcess(job);

        jobEngineController.updateJob(jobId, name, description, workerClassName, schedule, status, threads,
                maxPerMinute, failRetries, retryDelay, daysUntilCleanUp, uniqueInQueue);

        jobExecutionQueue.initializeBuffer(job);
        // jobEngine.start();
        jobScheduler.start(job);
        return job;

    }

    /**
     * Update a JobExecution
     * 
     * @return JobExecution
     */
    public JobExecution createJobExecution(Long jobId, String parameters, Boolean priority, LocalDateTime maturity,
            Long batchId, Long chainId, Long chainedPreviousExecutionId, boolean uniqueInQueue) {
        return jobEngineController.createJobExecution(jobId, parameters, priority, maturity, batchId, chainId,
                chainedPreviousExecutionId, uniqueInQueue);
    }

    public JobExecution updateJobExecution(Long jobId, Long jobExecutionId, JobExecutionStatus status,
            String parameters, boolean priority, LocalDateTime maturity, int fails) {

        JobExecution jobExecution = getJobExecutionById(jobId, jobExecutionId);

        if (JobExecutionStatus.QUEUED == jobExecution.getStatus() && JobExecutionStatus.QUEUED != status) {
            jobExecutionQueue.removeFromBuffer(jobExecution);
        }
        jobExecution.setStatus(status);
        jobExecution.setParameters(parameters);
        jobExecution.setPriority(priority);
        jobExecution.setMaturity(maturity);
        jobExecution.setFailRetry(fails);
        log.info("JobExecution updated: " + jobExecution);

        jobEngineController.updateJobExecution(jobId, jobExecutionId, jobExecution);
        return jobExecution;
    }

    public void deleteJobExecution(Long jobId, Long jobExecutionId) {

        JobExecution jobExecution = getJobExecutionById(jobId, jobExecutionId);

        if (jobExecution == null) {
            log.info("JobExecution does not exist: " + jobExecution);
            return;
        }

        if (Objects.equals(JobExecutionStatus.QUEUED, jobExecution.getStatus())) {
            jobExecutionQueue.removeFromBuffer(jobExecution);
        }

        log.info("JobExecution removed: " + jobExecution);

        jobEngineController.deleteJobExecution(jobId, jobExecutionId);

    }

    public void activateJob(Long jobId) {

        Job job = getJobById(jobId);
        if (job == null) {
            throw new RuntimeException("Job not found");
        }
        log.info("Activate job " + job.getName());
        job.setStatus(JobStatus.ACTIVE);
        jobEngineController.update(job.getId(), job);
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
        jobEngineController.update(job.getId(), job);
        if (job.getSchedule() != null && !job.getSchedule().isEmpty()) {
            jobScheduler.stop(job);
            jobExecutionQueue.cancelProcess(job);
        }
    }

    public GroupInfo getJobExecutionBatchInfo(Long jobId, Long batchId) {

        List<JobExecution> batchJobExecutions = jobEngineController.getBatch(jobId, batchId);

        List<JobExecutionInfo> batchInfo = batchJobExecutions.stream()
                .map(JobExecution -> new JobExecutionInfo(JobExecution.getId(), JobExecution.getStatus(),
                        JobExecution.getStartedAt(), JobExecution.getEndedAt(), JobExecution.getDuration(),
                        JobExecution.getFailRetryJobExecutionId()))
                .collect(Collectors.toList());

        return new GroupInfo(batchId, batchInfo);
    }

    public List<JobExecution> getJobExecutionBatch(Long jobId, Long batchId) {
        return jobEngineController.getBatch(jobId, batchId);
    }

    public GroupInfo getJobExecutionChainInfo(Long jobId, Long chainId) {
        List<JobExecution> chainJobExecutions = jobEngineController.getchain(jobId, chainId);
        List<JobExecutionInfo> batchInfo = chainJobExecutions.stream()
                .map(JobExecution -> new JobExecutionInfo(JobExecution.getId(), JobExecution.getStatus(),
                        JobExecution.getStartedAt(), JobExecution.getEndedAt(), JobExecution.getDuration(),
                        JobExecution.getFailRetryJobExecutionId()))
                .collect(Collectors.toList());
        return new GroupInfo(chainId, batchInfo);
    }

    public List<JobExecution> getJobExecutionChain(Long jobId, Long chainId) {
        return jobEngineController.getchain(jobId, chainId);
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
        return jobEngineController.getAllScheduledJobs();
    }

    /**
     * Get the execution times defined by {@link Job#getSchedule()}
     * 
     * @param schedule  CRON Expression
     * @param startTime start time for this request (if <tt>null</tt> then current
     *                  time is used)
     * @param endTime   end time for this request (if <tt>null</tt> then current
     *                  time plus 1 day is used)
     * @return List of {@link LocalDateTime} representing the execution times of a
     *         scheduled job between the <tt>startTime</tt> and <tt>endTime</tt>
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

    public List<JobExecution> getJobExecutions(Long jobId) {
        return jobEngineController.getJobExecutions(jobId);
    }

    public boolean stopScheduledJob(Long jobId) {
        Job job = jobEngineController.getJobById(jobId);
        if (job != null && job.getSchedule() != null && !job.getSchedule().isEmpty()) {
            jobScheduler.stop(job);
            return true;
        }

        throw new RuntimeException("No Job for JobId found");
    }

    public void triggerScheduledJobExecutionCreation(Job job) throws ClassNotFoundException {
        jobEngineController.triggerScheduledJobExecutionCreation(job);
    }

}