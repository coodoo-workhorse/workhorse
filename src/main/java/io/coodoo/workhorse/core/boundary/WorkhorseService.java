package io.coodoo.workhorse.core.boundary;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.coodoo.workhorse.core.control.ExecutionBuffer;
import io.coodoo.workhorse.core.control.JobScheduler;
import io.coodoo.workhorse.core.control.JobThread;
import io.coodoo.workhorse.core.control.Workhorse;
import io.coodoo.workhorse.core.control.WorkhorseConfigController;
import io.coodoo.workhorse.core.control.WorkhorseController;
import io.coodoo.workhorse.core.entity.Execution;
import io.coodoo.workhorse.core.entity.ExecutionLog;
import io.coodoo.workhorse.core.entity.ExecutionStatus;
import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.core.entity.JobExecutionCount;
import io.coodoo.workhorse.core.entity.JobExecutionStatusSummary;
import io.coodoo.workhorse.core.entity.JobStatus;
import io.coodoo.workhorse.core.entity.JobStatusCount;
import io.coodoo.workhorse.core.entity.WorkhorseConfig;
import io.coodoo.workhorse.core.entity.WorkhorseConfigBuilder;
import io.coodoo.workhorse.core.entity.WorkhorseInfo;
import io.coodoo.workhorse.persistence.PersistenceManager;
import io.coodoo.workhorse.persistence.interfaces.listing.ListingParameters;
import io.coodoo.workhorse.persistence.interfaces.listing.ListingResult;
import io.coodoo.workhorse.persistence.memory.MemoryConfig;
import io.coodoo.workhorse.persistence.memory.MemoryConfigBuilder;
import io.coodoo.workhorse.util.CronExpression;
import io.coodoo.workhorse.util.WorkhorseUtil;

/**
 * Central Workhorse API-Service
 * 
 * @author coodoo GmbH (coodoo.io)
 */
@ApplicationScoped
public class WorkhorseService {

    private static final Logger log = LoggerFactory.getLogger(WorkhorseService.class);

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
    WorkhorseConfigController workhorseConfigController;

    @Inject
    JobScheduler jobScheduler;

    WorkhorseConfig currentWorkhorseConfig = null;

    /**
     * Initialize the persistence to use. The default persistence is Memory.
     */
    public void init() {

        currentWorkhorseConfig = new MemoryConfigBuilder().build();

        init(currentWorkhorseConfig);
    }

    /**
     * Initialize the persistence to use
     * 
     * The configuration can be built by using the builder that extends {@link WorkhorseConfigBuilder} of the chosen persisitence.
     * 
     * For example, if you want to use the default persistence {@link MemoryConfig} use the builder as follow:
     * 
     * <code>init(new MemoryConfigBuilder().build())</code>
     * 
     * @param workhorseConfig Configuration of the chosen persistence (this can only be done once and is final).
     */
    public void init(WorkhorseConfig workhorseConfig) {

        String version = WorkhorseUtil.getVersion();

        log.info("hyyyyyyhdmNmhs++/+//+/+/+//+/+//+/oNm-                ");
        log.info("dhyhdmNNdyo++//+/+////+/+//+/+//+/+omN/:/+++/-`       ");
        log.info("hmNNdyo+////+//++++++///++/+/+//+//+hMNhso+oydmh:     ");
        log.info("/++++//////+oydmmmmmNdyo+//+/+//++yNd/`       .oNd-   ");
        log.info("//+/++++++omNs:``  ``:yNms++++/++yMo`           .dN-  ");
        log.info("//+/+//++yNy.          .yNy+/+//oNh`   `::.      :Ny  ");
        log.info("+/+/+///oNy`            `hNo/+//oNs    sMMm.     .Nd` ");
        log.info("+/+/+///yM+      +mNs`   /My/+//+mm.   .oo-      +Mo  ");
        log.info("+/+/+/+/sMo      /mms`   oMs/++/+oNm:          `oNy`  ");
        log.info("//+/+///+dN/            /Nd+/+//+/+hNh+-`  ``:omm+    ");
        log.info("//+/+//+/+hNy-`      `-yNh++/+//+/+++sdmmmmmmdmN/     ");
        log.info("//+/+//+//+ohmmhsooshmmho+/+/+//+//////++++++++dN+    Workhorse");
        log.info("+/+/+//+/////++ssyyss++/+//+/+//+/+/++/+/+///++yNMo   " + version);
        log.info("//+////////////////////////+/+////////////+ohmNdshMs` ");

        currentWorkhorseConfig = workhorseConfig;
        persistenceManager.initializePersistence(workhorseConfig);
        workhorseConfigController.initializeStaticConfig(workhorseConfig);
    }

    /**
     * Start Workhorse.
     */
    public void start() {
        if (currentWorkhorseConfig == null) {
            currentWorkhorseConfig = new MemoryConfigBuilder().build();
        }
        start(currentWorkhorseConfig);
    }

    /**
     * <p>
     * Start Workhorse with the configuration of a persistence.
     * </p>
     * 
     * The configuration can be built by using the builder that extends {@link WorkhorseConfigBuilder} of the chosen persistence.
     * <p>
     * For example, if you want to use the default persistence {@link MemoryConfig} use the builder as follow:
     * <p>
     * <code>start(new MemoryConfigBuilder().build())</code>
     * 
     * @param workhorseConfig Configuration of the chosen persistence (this can only be done once and is final).
     */
    public void start(WorkhorseConfig workhorseConfig) {

        long ms = System.currentTimeMillis();

        // Check if the persistence is already initialized. If so the engine is already living but paused and should now start again.
        if (!persistenceManager.isInitialized()) {
            currentWorkhorseConfig = workhorseConfig;
            persistenceManager.initializePersistence(workhorseConfig);
            workhorseConfigController.initializeStaticConfig(workhorseConfig);
        }

        workhorseController.loadWorkers();
        executionBuffer.initialize();
        workhorse.start();
        jobScheduler.startScheduler();

        ms = System.currentTimeMillis() - ms;
        String startMessage = "Workhorse " + WorkhorseUtil.getVersion() + " (Persistence: " + workhorseConfig.getPersistenceName() + " "
                        + workhorseConfig.getPersistenceVersion() + ") started in " + ms + "ms";
        workhorseLogService.logMessage(startMessage, null, true);
    }

    /**
     * Stop Workhorse
     */
    public void stop() {

        long ms = System.currentTimeMillis();

        workhorse.stop();
        jobScheduler.stopScheduler();
        executionBuffer.clearMemoryQueue();

        ms = System.currentTimeMillis() - ms;
        String startMessage = "Workhorse stopped in " + ms + "ms";
        workhorseLogService.logMessage(startMessage, null, true);
    }

    /**
     * Retrieves the current configuration of the job engine
     * 
     * @return the current configuration of the job engine
     */
    public WorkhorseConfig getWorkhorseConfig() {
        return workhorseConfigController.getWorkhorseConfig();
    }

    /**
     * Update the configuration of the job engine
     * 
     * @param workhorseConfig the new configurations to set
     */
    public void updateWorkhorseConfig(WorkhorseConfig workhorseConfig) {
        workhorseConfigController.updateWorkhorseConfig(workhorseConfig);
    }

    /**
     * Update the configuration of the job engine
     */
    public void updateWorkhorseConfig(String timeZone, int bufferMax, int bufferMin, int bufferPollInterval, int bufferPushFallbackPollIntervall,
                    long minutesUnitlCleanup, int executionTimeout, ExecutionStatus executionTimeoutStatus, String logChange, String logTimeFormat,
                    String logInfoMarker, String logWarnMarker, String logErrorMarker) {

        WorkhorseConfig config = new MemoryConfig(timeZone, bufferMax, bufferMin, bufferPollInterval, bufferPushFallbackPollIntervall, minutesUnitlCleanup,
                        executionTimeout, executionTimeoutStatus, logChange, logTimeFormat, logInfoMarker, logWarnMarker, logErrorMarker);

        workhorseConfigController.updateWorkhorseConfig(config);
    }

    /**
     * Retrieves all jobs
     * 
     * @return List of all Job
     */
    public List<Job> getAllJobs() {
        return workhorseController.getAllJobs();
    }

    /**
     * Get the listing result of execution
     * 
     * @param listingParameters defines the listing queue. It contains optional query parameters as described above
     * @return list of execution
     */
    public ListingResult<Execution> getExecutionListing(Long jobId, ListingParameters listingParameters) {
        if (listingParameters.getSortAttribute() == null || listingParameters.getSortAttribute().isEmpty()) {
            listingParameters.setSortAttribute("-createdAt");
        }
        return workhorseController.getExecutionListing(jobId, listingParameters);
    }

    /**
     * Get the listing result of jobs
     * 
     * @param listingParameters defines the listing queue. It contains optional query parameters as described above
     * @return list of jobs
     */
    public ListingResult<Job> getJobListing(ListingParameters listingParameters) {
        return workhorseController.getJobListing(listingParameters);
    }

    /**
     * Get the count of jobs by status
     * 
     * @return JobStatusCount
     */
    public JobStatusCount getJobStatusCount() {
        return workhorseController.getJobStatusCount();
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
     * Retrieves a {@link ExecutionLog} of the correspnding executionId
     * 
     * @param jobId ID of the correspnding job
     * @param executionId ID of the correspnding execution
     * @return the log of the execution
     */
    public ExecutionLog getExecutionLog(Long jobId, Long executionId) {
        return workhorseController.getExecutionLog(jobId, executionId);

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
     * Update a {@link Job}
     * 
     * @param jobId
     * @param name the name of the job
     * @param description the description about what the job do.
     * @param workerClassName the name of the Class that the Job use as Parameter
     * @param schedule the timstamp as cron-syntax to schedule the job
     * @param status the status of the Job. <code>ACTIVE</code>, <code>NO_WORKER</code>
     * @param threads the number of thread, that can process the job
     * @param maxPerMinute the max number of execution per minute
     * @param failRetries the number of retries for a failed execution
     * @param retryDelay the duration to wait before a retry
     * @param minutesUntilCleanUp the number of minutes before delete execution of this Job
     * @param uniqueQueued if a job has the uniqueInqueue set <code>true</code>, Two or more job execution with the same parameters are not authorized
     * 
     * @return the updated job
     */
    public Job updateJob(Long jobId, String name, String description, String workerClassName, String schedule, JobStatus status, int threads,
                    Integer maxPerMinute, int failRetries, int retryDelay, int minutesUntilCleanUp, boolean uniqueQueued) {

        Job job = getJobById(jobId);

        jobScheduler.stop(job);
        executionBuffer.clearMemoryQueue(job);

        Job updatedJob = workhorseController.updateJob(jobId, name, description, workerClassName, schedule, status, threads, maxPerMinute, failRetries,
                        retryDelay, minutesUntilCleanUp, uniqueQueued);

        if (JobStatus.ACTIVE.equals(updatedJob.getStatus())) {
            startJob(updatedJob);
        }
        return updatedJob;

    }

    /**
     * Start the processing of executions of a {@link Job}
     * 
     * @param job the job to start
     */
    public void startJob(Job job) {

        executionBuffer.initialize(job);
        jobScheduler.start(job);
        workhorse.poll(job);
    }

    /**
     * Delete a job.
     * 
     * @param jobId ID of the job to delete
     */
    public void deleteJob(Long jobId) {
        Job job = getJobById(jobId);

        if (job != null) {
            jobScheduler.stop(job);
            executionBuffer.clearMemoryQueue(job);
            workhorseController.deleteJob(jobId);
        }
    }

    /**
     * @deprecated This version must be deleted as soon as the corresponding resource endpoint in the Workhorse-ui-api project is deleted.
     * 
     *             create an {@link Execution}
     * 
     * @param jobId Id of the corresponding job
     * @param parameters parameters of the execution
     * @param priority if <code>true</code> the execution will be process before other execution. Otherwise the execution will be process in order of add.
     * @param plannedFor If a plannedFor is given, the job execution will not be executed before this time.
     * @param expiresAt If expiresAt is given, the execution have to be process before this time. Otherwise the execution is cancelled.
     * @param batchId Id to refer to a group of executions to handle as a single entity.
     * @param chainId Id to refer to a group of executions to process by an order.
     * @param uniqueQueued if true than two and more executions with the sames paramters can be queued.
     * @return the created execution
     */
    @Deprecated
    public Execution createExecution(Long jobId, String parameters, Boolean priority, LocalDateTime plannedFor, LocalDateTime expiresAt, Long batchId,
                    Long chainId, boolean uniqueQueued) {

        return createExecution(jobId, parameters, priority, plannedFor, expiresAt, batchId, chainId);
    }

    /**
     * create an {@link Execution}
     * 
     * @param jobId Id of the corresponding job
     * @param parameters parameters of the execution
     * @param priority if <code>true</code> the execution will be process before other execution. Otherwise the execution will be process in order of add.
     * @param plannedFor If a plannedFor is given, the job execution will not be executed before this time.
     * @param expiresAt If expiresAt is given, the execution have to be process before this time. Otherwise the execution is cancelled.
     * @param batchId Id to refer to a group of executions to handle as a single entity.
     * @param chainId Id to refer to a group of executions to process by an order.
     * @return the created execution
     */
    public Execution createExecution(Long jobId, String parameters, Boolean priority, LocalDateTime plannedFor, LocalDateTime expiresAt, Long batchId,
                    Long chainId) {

        Job job = getJobById(jobId);
        if (job == null) {
            return null;
        }
        return workhorseController.createExecution(jobId, parameters, priority, plannedFor, expiresAt, batchId, chainId, job.isUniqueQueued());
    }

    public Execution updateExecution(Long jobId, Long executionId, ExecutionStatus status, String parameters, boolean priority, LocalDateTime plannedFor,
                    int fails) {

        Execution execution = getExecutionById(jobId, executionId);

        if (ExecutionStatus.QUEUED == execution.getStatus() && ExecutionStatus.QUEUED != status) {
            executionBuffer.removeFromBuffer(execution);
        }
        execution.setStatus(status);
        execution.setParameters(parameters);
        execution.setPriority(priority);
        execution.setPlannedFor(plannedFor);
        execution.setFailRetry(fails);
        log.info("Execution updated: " + execution);

        workhorseController.updateExecution(execution);
        return execution;
    }

    public void deleteExecution(Long jobId, Long executionId) {

        Execution execution = getExecutionById(jobId, executionId);
        if (execution == null) {
            log.info("Execution does not exist: {} ", execution);
            return;
        }
        if (execution.getStatus() == ExecutionStatus.QUEUED) {
            executionBuffer.removeFromBuffer(execution);
        }
        log.info("Execution removed: {}", execution);

        workhorseController.deleteExecution(jobId, executionId);
    }

    /**
     * You can redo an {@link Execution} in status {@link ExecutionStatus#FINISHED}, {@link ExecutionStatus#FAILED} and {@link ExecutionStatus#ABORTED}, but all
     * meta data like timestamps and logs of this execution will be gone!
     * 
     * @param executionId ID of the {@link Execution} you wish to redo
     * @return cleared out {@link Execution} in status {@link ExecutionStatus#QUEUED}
     */
    public Execution redoJobExecution(Long jobId, Long executionId) {

        Execution execution = getExecutionById(jobId, executionId);
        if (ExecutionStatus.QUEUED == execution.getStatus() || ExecutionStatus.RUNNING == execution.getStatus()) {
            log.warn("Can't redo Execution in status {}: {}", execution.getStatus(), execution);
            return execution;
        }

        log.info("Redo {} {}", execution.getStatus(), execution);

        execution.setPlannedFor(WorkhorseUtil.timestamp());
        execution.setStatus(ExecutionStatus.QUEUED);
        execution.setStartedAt(null);
        execution.setEndedAt(null);
        execution.setDuration(null);
        execution.setFailRetry(0);
        execution.setFailRetryExecutionId(null);

        workhorseController.appendExecutionLog(jobId, executionId, "The execution will be retried.");
        return workhorseController.updateExecution(execution);
    }

    /**
     * <i>Activate a job.</i><br>
     * <br>
     * The executions of this job can again be processed by the job engine
     * 
     * @param jobId ID of the job to activate
     */
    public void activateJob(Long jobId) {

        Job job = getJobById(jobId);
        if (job == null) {
            throw new RuntimeException("Job not found");
        }
        log.info("Activate job {}", job.getName());
        job.setStatus(JobStatus.ACTIVE);

        workhorseController.update(job);

        startJob(job);
    }

    /**
     * <i>Deactivate a job.</i><br>
     * <br>
     * The next executions of this job will not be processed by the job engine
     * 
     * @param jobId ID of the job to deactivate
     */
    public void deactivateJob(Long jobId) {
        Job job = getJobById(jobId);
        if (job == null) {
            throw new RuntimeException("Job not found");
        }
        log.info("Deactivate job {}", job.getName());
        job.setStatus(JobStatus.INACTIVE);
        workhorseController.update(job);
        if (job.getSchedule() != null && !job.getSchedule().isEmpty()) {
            jobScheduler.stop(job);
        }
        executionBuffer.clearMemoryQueue(job);
    }

    /**
     * Get a Job by the classname of his worker.
     * 
     * @param className
     * @return
     */
    public Job getJobByClassName(String className) {

        Job job = null;
        if (className != null && !className.isEmpty()) {
            job = workhorseController.getJobByClassName(className.trim());
        }
        return job;
    }

    public List<Execution> getExecutionBatch(Long jobId, Long batchId) {
        return workhorseController.getBatch(jobId, batchId);
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
        LocalDateTime nextScheduledTime = startTime != null ? startTime : WorkhorseUtil.timestamp();

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
     * 
     * @param schedule CRON Expression
     * @param startTime start time for this request (if <tt>null</tt> then current time is used)
     * @param endTime end time for this request (if <tt>null</tt> then current time plus 1 day is used) =======
     * @param schedule CRON Expression
     * @param startTime start time for this request (if <tt>null</tt> then current time is used)
     * @param endTime end time for this request (if <tt>null</tt> then current time plus 1 day is used) >>>>>>> master
     * @return List of {@link LocalDateTime} representing the execution times of a scheduled job between the <tt>startTime</tt> and <tt>endTime</tt>
     */
    public List<LocalDateTime> getScheduledTimes(String schedule, LocalDateTime startTime, LocalDateTime endTime) {

        List<LocalDateTime> scheduledTimes = new ArrayList<>();
        if (schedule == null) {
            return scheduledTimes;
        }

        CronExpression cronExpression = new CronExpression(schedule);
        LocalDateTime scheduledTime = startTime != null ? startTime : WorkhorseUtil.timestamp();
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

    /**
     * Get all jobs, whose executions are in the given status
     * 
     * @param status status of executions
     * @param since only executions that have been created after this timestamp have to be returned. If null, it is no more considered
     * @return list of job
     */
    public List<JobExecutionStatusSummary> getJobExecutionStatusSummaries(ExecutionStatus status, LocalDateTime since) {
        if (ExecutionStatus.FAILED.equals(status) && since == null) {
            // TODO remove this if since can be set by frontend ui
            LocalDateTime lt = LocalDateTime.now();
            since = lt.minus(24, ChronoUnit.HOURS);
        }
        return workhorseController.getJobExecutionStatusSummaries(status, since);
    }

    /**
     * Retrieves the counts of {@link Execution} by status for a specific job or for all jobs between a time interval
     * 
     * @param jobId ID of the corresponding job
     * @param from only executions that were created after this time stamp are considered. If <code>null</code>, the default value is generated by subtracting
     *        24 hours from the {@code LocalDateTime} parameter {@code to}.
     * @return {@link JobExecutionCount}
     */
    public JobExecutionCount getJobExecutionCount(Long jobId, LocalDateTime from) {
        return getJobExecutionCount(jobId, from, null);
    }

    /**
     * Retrieves the counts of {@link Execution} by status for a specific job or for all jobs between a time interval
     * 
     * @param jobId ID of the corresponding job
     * @param from only executions that were created after this time stamp are considered. If <code>null</code>, the default value is generated by subtracting
     *        24 hours from the {@code LocalDateTime} parameter {@code to}.
     * @param to only executions that were created before this time stamp are considered. If <code>null</code>, the default value is the the current date-time
     *        from the system clock.
     * @return {@link JobExecutionCount}
     */
    public JobExecutionCount getJobExecutionCount(Long jobId, LocalDateTime from, LocalDateTime to) {

        LocalDateTime now = WorkhorseUtil.timestamp();
        if (to == null || to.isAfter(now)) {
            to = now;
        }

        if (from == null || from.isAfter(to)) {
            from = to.minusHours(24);
        }

        return workhorseController.getJobExecutionCount(jobId, from, to);
    }

    /**
     * Retrieves the map between a job and their corresponding {@link JobThread} object
     * 
     * @return the map
     */
    public Map<Long, Set<JobThread>> getJobThreads() {
        return executionBuffer.getJobThreads();
    }

}
