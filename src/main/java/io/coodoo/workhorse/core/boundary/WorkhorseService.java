package io.coodoo.workhorse.core.boundary;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.coodoo.workhorse.core.control.ExecutionBuffer;
import io.coodoo.workhorse.core.control.JobScheduler;
import io.coodoo.workhorse.core.control.Workhorse;
import io.coodoo.workhorse.core.control.WorkhorseConfigController;
import io.coodoo.workhorse.core.control.WorkhorseController;
import io.coodoo.workhorse.core.entity.WorkhorseConfig;
import io.coodoo.workhorse.core.entity.Execution;
import io.coodoo.workhorse.core.entity.ExecutionStatus;
import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.core.entity.JobStatus;
import io.coodoo.workhorse.core.entity.WorkhorseInfo;
import io.coodoo.workhorse.persistence.PersistenceManager;
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

    /**
     * Start Workhorse. The default persistence is Memory.
     */
    public void start() {
        start(new MemoryConfigBuilder().build());
    }

    /**
     * Start Workhorse with the configuration of a persistence.
     * 
     * The configuration can be built by using the builder that extends
     * {@link WorkhorseConfigBuilder} of the chosen persisitence.
     * 
     * For example, if you want to use the default persistence {@link MemoryConfig}
     * use the builder as follow:
     * 
     * <code>start(new MemoryConfigBuilder().build())</code>
     * 
     * @param workhorseConfig Configuration of the chosen persistence.
     */
    public void start(WorkhorseConfig workhorseConfig) {
        persistenceManager.initializePersistence(workhorseConfig);
        workhorseConfigController.initializeStaticConfig(workhorseConfig);
        workhorseController.loadWorkers();
        executionBuffer.initialize();
        workhorse.start();
        jobScheduler.startScheduler();
    }

    /**
     * Stop the Workhorse
     */
    public void stop() {
        workhorse.stop();
        for (Job job : getAllScheduledJobs()) {
            jobScheduler.stop(job);
            executionBuffer.cancelProcess(job);
        }
        executionBuffer.clear();
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
     * Retrieves all jobs
     * 
     * @return List of all Job
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
    public Job updateJob(Long jobId, String name, String description, String workerClassName, String schedule,
            JobStatus status, int threads, Integer maxPerMinute, int failRetries, int retryDelay, int daysUntilCleanUp,
            boolean uniqueQueued) {

        Job job = getJobById(jobId);

        jobScheduler.stop(job);
        // workhorse.stop(); maybe we don t need. To proove
        executionBuffer.cancelProcess(job);

        workhorseController.updateJob(jobId, name, description, workerClassName, schedule, status, threads,
                maxPerMinute, failRetries, retryDelay, daysUntilCleanUp, uniqueQueued);

        executionBuffer.initialize(job);
        // workhorse.start();
        jobScheduler.start(job);
        return job;

    }

    /**
     * Update a {@link Execution}
     */
    public Execution createExecution(Long jobId, String parameters, Boolean priority, LocalDateTime maturity,
            Long batchId, Long chainId, Long chainedPreviousExecutionId, boolean uniqueQueued) {
        return workhorseController.createExecution(jobId, parameters, priority, maturity, batchId, chainId,
                chainedPreviousExecutionId, uniqueQueued);
    }

    public Execution updateExecution(Long jobId, Long executionId, ExecutionStatus status, String parameters,
            boolean priority, LocalDateTime maturity, int fails) {

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
            log.info("Execution does not exist: {} ", execution);
            return;
        }
        if (execution.getStatus() == ExecutionStatus.QUEUED) {
            executionBuffer.removeFromBuffer(execution);
        }
        log.info("Execution removed: {}", execution);

        workhorseController.deleteExecution(jobId, executionId);
    }

    public void activateJob(Long jobId) {

        Job job = getJobById(jobId);
        if (job == null) {
            throw new RuntimeException("Job not found");
        }
        log.info("Activate job {}", job.getName());
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
        log.info("Deactivate job {}", job.getName());
        job.setStatus(JobStatus.INACTIVE);
        workhorseController.update(job.getId(), job);
        if (job.getSchedule() != null && !job.getSchedule().isEmpty()) {
            jobScheduler.stop(job);
            executionBuffer.cancelProcess(job);
        }
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

}
