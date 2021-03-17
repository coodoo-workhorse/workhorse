package io.coodoo.workhorse.core.control;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.coodoo.workhorse.core.boundary.WorkerWith;
import io.coodoo.workhorse.core.boundary.WorkhorseLogService;
import io.coodoo.workhorse.core.boundary.annotation.InitialJobConfig;
import io.coodoo.workhorse.core.control.event.JobErrorEvent;
import io.coodoo.workhorse.core.entity.ErrorType;
import io.coodoo.workhorse.core.entity.Execution;
import io.coodoo.workhorse.core.entity.ExecutionFailStatus;
import io.coodoo.workhorse.core.entity.ExecutionLog;
import io.coodoo.workhorse.core.entity.ExecutionStatus;
import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.core.entity.JobStatus;
import io.coodoo.workhorse.persistence.interfaces.ExecutionPersistence;
import io.coodoo.workhorse.persistence.interfaces.JobPersistence;
import io.coodoo.workhorse.persistence.interfaces.listing.ListingParameters;
import io.coodoo.workhorse.persistence.interfaces.listing.ListingResult;
import io.coodoo.workhorse.persistence.interfaces.qualifier.ExecutionQualifier;
import io.coodoo.workhorse.persistence.interfaces.qualifier.JobQualifier;
import io.coodoo.workhorse.util.WorkhorseUtil;

/**
 * Class to acces to {@link Job} and {@link Execution}
 * 
 * @author coodoo GmbH (coodoo.io)
 */
@ApplicationScoped
public class WorkhorseController {

    private static final Logger log = LoggerFactory.getLogger(WorkhorseController.class);

    @Inject
    @Any
    Instance<BaseWorker> workerInstances;

    @Inject
    @JobQualifier
    JobPersistence jobPersistence;

    @Inject
    @ExecutionQualifier
    ExecutionPersistence executionPersistence;

    @Inject
    WorkhorseLogService workhorseLogService;

    @Inject
    Event<JobErrorEvent> jobErrorEvent;

    /**
     * Load all worker-Class of the classpath
     */
    public void loadWorkers() {

        List<Class<?>> workerClasses = new ArrayList<>();

        log.info("Initializing Workhorse Jobs...");

        // check whether new worker exists and must be created and persisted
        for (BaseWorker worker : workerInstances) {
            Class<?> workerclass = worker.getWorkerClass();
            Job job = jobPersistence.getByWorkerClassName(workerclass.getName());
            if (job == null) {
                job = createJob(worker);
                log.info("[{}] Job and Worker created and Configuration persisted ({})", job.getWorkerClassName(), job.getName());
            }
            workerClasses.add(workerclass);
        }

        // check if persisted job can be mapped a Worker class
        for (Job job : jobPersistence.getAll()) {
            String workerClassName = job.getWorkerClassName();
            BaseWorker workerOfDbJob;
            try {
                workerOfDbJob = getWorker(job);
            } catch (Exception exception) {

                job.setStatus(JobStatus.ERROR);
                log.error("[{}] Worker class not found ({})", workerClassName, job.getName());
                jobErrorEvent.fire(new JobErrorEvent(exception, ErrorType.ERROR_BY_FOUND_JOB_WORKER.getMessage(), job.getId(), job.getStatus()));
                continue;
            }

            Class<?> workerClass = workerOfDbJob.getWorkerClass();
            if (!workerClasses.contains(workerClass)) {
                log.trace("JobStatus of Job {} updated from {} to {}", job, job.getStatus(), JobStatus.NO_WORKER);
                job.setStatus(JobStatus.NO_WORKER);
                jobPersistence.update(job);
                log.error("No Worker Class found for Job: {}", job);
                jobErrorEvent.fire(new JobErrorEvent(new Throwable(ErrorType.NO_JOB_WORKER_FOUND.getMessage()), ErrorType.NO_JOB_WORKER_FOUND.getMessage(),
                                job.getId(), job.getStatus()));
                continue;
            }

            // Maybe in an iteration before there was no worker and now the worker is available - set this job to inactive
            if (job.getStatus().equals(JobStatus.NO_WORKER)) {
                job.setStatus(JobStatus.INACTIVE);
                jobPersistence.update(job);
                log.info("JobStatus of Job {} updated from {} to {}", job, JobStatus.NO_WORKER, JobStatus.INACTIVE);
                workhorseLogService.logChange(job.getId(), job.getStatus(), "Status", JobStatus.NO_WORKER, JobStatus.INACTIVE, "Worker class found.");

            }

            // Check if parameter class has changed
            String parametersClassName = getWorkerParameterName(workerOfDbJob);

            // The Objects-Class is null-safe and can handle Worker-classes without Parameters
            if (!Objects.equals(parametersClassName, job.getParametersClassName())) {
                log.info("Parameters class name of job worker {} changed from {} to {}", job.getWorkerClassName(), job.getParametersClassName(),
                                parametersClassName);
                workhorseLogService.logChange(job.getId(), job.getStatus(), "Parameters class", job.getParametersClassName(), parametersClassName, null);

                job.setParametersClassName(parametersClassName);
                jobPersistence.update(job);
            }

        }
    }

    private Job createJob(BaseWorker worker) {
        Job job = new Job();

        Class<? extends BaseWorker> workerClass = worker.getWorkerClass();

        if (workerClass.isAnnotationPresent(InitialJobConfig.class)) {
            InitialJobConfig initialJobConfig = workerClass.getAnnotation(InitialJobConfig.class);
            if (!initialJobConfig.name().isEmpty()) {
                job.setName(initialJobConfig.name());
            } else {
                job.setName(workerClass.getSimpleName());
            }

            job.setDescription(initialJobConfig.description());

            job.setWorkerClassName(workerClass.getName());
            job.setSchedule(initialJobConfig.schedule());
            job.setStatus(initialJobConfig.status());
            job.setThreads(initialJobConfig.threads());

            if (initialJobConfig.maxPerMinute() != InitialJobConfig.JOB_CONFIG_MAX_PER_MINUTE) {
                job.setMaxPerMinute(initialJobConfig.maxPerMinute());
            }
            job.setFailRetries(initialJobConfig.failRetries());
            job.setRetryDelay(initialJobConfig.retryDelay());

            job.setMinutesUntilCleanUp(initialJobConfig.minutesUntilCleanUp());

            job.setUniqueQueued(initialJobConfig.uniqueQueued());

        } else {

            // Use initial default worker informations
            job.setName(workerClass.getSimpleName());
            job.setWorkerClassName(workerClass.getName());
            job.setUniqueQueued(InitialJobConfig.JOB_CONFIG_UNIQUE_IN_QUEUE);
            job.setStatus(JobStatus.ACTIVE);
            job.setThreads(InitialJobConfig.JOB_CONFIG_THREADS);
            job.setMinutesUntilCleanUp(InitialJobConfig.JOB_CONFIG_MINUTES_UNTIL_CLEANUP);
        }

        String parameterClassName = getWorkerParameterName(worker);
        job.setParametersClassName(parameterClassName);

        Job persistedJob = jobPersistence.persist(job);

        if (persistedJob == null || persistedJob.getId() == null) {
            JobErrorEvent jobErrorMessage = new JobErrorEvent(new Throwable(ErrorType.ERROR_BY_JOB_PERSIST.getMessage()),
                            ErrorType.ERROR_BY_JOB_PERSIST.getMessage(), null, null);

            jobErrorEvent.fireAsync(jobErrorMessage);

            workhorseLogService.logException(jobErrorMessage);

            throw new RuntimeException("The job " + job + " couldn't be persisited by the persisitence " + jobPersistence.getPersistenceName());
        }

        log.trace("Created {}", persistedJob);
        return job;
    }

    public void deleteJob(Long jobId) {
        jobPersistence.deleteJob(jobId);
    }

    /**
     * retrieves the parameter's name of a Job from current workspace as String.
     * 
     * @param job
     * @return
     * @throws ClassNotFoundException
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public String getWorkerParameterName(BaseWorker worker) {
        if (worker instanceof WorkerWith) {
            return ((WorkerWith) worker).getParametersClassName();
        }
        return null;
    }

    /**
     * retrieves the Worker of a Job.
     * 
     * @param job
     * @return Baseworker
     * @throws ClassNotFoundException
     * @throws Exception
     */
    private BaseWorker getWorker(Job job) throws ClassNotFoundException {
        for (BaseWorker worker : workerInstances) {
            if (job.getWorkerClassName().equals(worker.getWorkerClass().getName())) {
                return worker;
            }
        }

        log.error("No Worker class found for {}", job);
        workhorseLogService.logChange(job.getId(), JobStatus.NO_WORKER, "Status", job.getStatus(), JobStatus.NO_WORKER, null);

        job.setStatus(JobStatus.NO_WORKER);
        jobPersistence.update(job);

        jobErrorEvent.fire(new JobErrorEvent(new ClassNotFoundException(), ErrorType.NO_JOB_WORKER_FOUND.getMessage(), job.getId(), job.getStatus()));
        throw new ClassNotFoundException();

    }

    /**
     * retrieves a worker by his class name
     * 
     * @param className
     * @return
     */
    public Class<? extends BaseWorker> getWorkerByClassName(String className) {
        for (BaseWorker worker : workerInstances) {
            Class<? extends BaseWorker> workerclass = worker.getWorkerClass();
            if (workerclass.getName().equals(className)) {
                return workerclass;
            }
        }
        return null;
    }

    /**
     * Create a job execution for a scheduled job
     * 
     * @param job job to execute
     * @throws ClassNotFoundException
     */
    public void triggerScheduledExecutionCreation(Job job) throws ClassNotFoundException {
        BaseWorker worker = getWorker(job);
        worker.onSchedule();
    }

    /**
     * retrieves the scheduled jobs
     * 
     * @return List of scheduled jobs
     */
    public List<Job> getAllScheduledJobs() {
        List<Job> scheduledJobs = new ArrayList<>();
        scheduledJobs.addAll(jobPersistence.getAllScheduled());

        return scheduledJobs;
    }

    /**
     * create a Job Execution
     * 
     * @param jobId Id of the corresponding job
     * @param parameters parameters of the execution
     * @param priority if <code>true</code> the execution will be process before other execution. Otherwise the execution will be process in order of add.
     * @param plannedFor If a plannedFor is given, the job execution will not be executed before this time.
     * @param expiresAt If expiresAt is given, the execution have to be process before this time. Otherwise the execution is cancelled.
     * @param batchId Id to refer to a group of executions to handle as a single entity.
     * @param chainId Id to refer to a group of executions to process by an order.
     * @param uniqueQueued
     * @return the created Job Execution
     */
    public Execution createExecution(Long jobId, String parameters, Boolean priority, LocalDateTime plannedFor, LocalDateTime expiresAt, Long batchId,
                    Long chainId, boolean uniqueQueued) {

        Integer parametersHash = null;
        if (parameters != null) {
            parametersHash = parameters.hashCode();
            if (parameters.trim().isEmpty()) {
                parameters = null;
                parametersHash = null;
            }
        }

        if (uniqueQueued) {
            // Prüfen ob es bereits eine excecution mit diesen parametern existiert und
            // im Status QUEUED ist. Wenn ja diese zurückgeben.
            Execution equalQueuedJobExcecution = executionPersistence.getFirstCreatedByJobIdAndParametersHash(jobId, parametersHash);
            if (equalQueuedJobExcecution != null) {
                // TODO Warn Log ausgeben, dass es versucht wurde, eine Execution mit dem
                // gleichen Parameter zu erstellen.
                return equalQueuedJobExcecution;
            }
        }
        Execution execution = new Execution();
        execution.setJobId(jobId);

        if (plannedFor != null && WorkhorseUtil.timestamp().isBefore(plannedFor)) {
            execution.setStatus(ExecutionStatus.PLANNED);
        } else {
            execution.setStatus(ExecutionStatus.QUEUED);
        }

        execution.setParameters(parameters);
        execution.setParametersHash(parametersHash);
        execution.setPriority(priority != null ? priority : false);
        execution.setPlannedFor(plannedFor);
        execution.setExpiresAt(expiresAt);
        execution.setBatchId(batchId);
        execution.setChainId(chainId);

        Execution persistedExecution = executionPersistence.persist(execution);

        if (persistedExecution == null || persistedExecution.getId() == null) {
            JobErrorEvent jobErrorMessage = new JobErrorEvent(new Throwable(ErrorType.ERROR_BY_EXECUTION_PERSIST.getMessage()),
                            ErrorType.ERROR_BY_EXECUTION_PERSIST.getMessage(), execution.getJobId(), null);

            jobErrorEvent.fireAsync(jobErrorMessage);
            workhorseLogService.logException(jobErrorMessage);

            throw new RuntimeException("The execution " + execution + " couldn't be persisited by the persisitence.");
        }
        log.trace("Execution successfully created: {}", persistedExecution);
        return persistedExecution;

    }

    /**
     * Create a new job execution to retry an execution
     * 
     * @param failedExecution job execution to retry
     * @return the created job execution
     */
    public Execution createRetryExecution(Execution failedExecution) {

        Execution retryExecution = new Execution();
        retryExecution.setJobId(failedExecution.getJobId());
        retryExecution.setStatus(failedExecution.getStatus());
        retryExecution.setStartedAt(LocalDateTime.now(ZoneId.of(StaticConfig.TIME_ZONE)));
        retryExecution.setPriority(failedExecution.isPriority());
        retryExecution.setPlannedFor(failedExecution.getPlannedFor());
        retryExecution.setChainId(failedExecution.getChainId());
        retryExecution.setParameters(failedExecution.getParameters());
        retryExecution.setParametersHash(failedExecution.getParametersHash());

        // increase failure number
        retryExecution.setFailRetry(failedExecution.getFailRetry() + 1);
        if (retryExecution.getFailRetryExecutionId() == null) {
            retryExecution.setFailRetryExecutionId(failedExecution.getId());
        }

        Execution persistedExecution = executionPersistence.persist(retryExecution);

        if (persistedExecution == null || persistedExecution.getId() == null) {
            JobErrorEvent jobErrorMessage = new JobErrorEvent(new Throwable(ErrorType.ERROR_BY_EXECUTION_PERSIST.getMessage()),
                            ErrorType.ERROR_BY_EXECUTION_PERSIST.getMessage(), retryExecution.getJobId(), null);

            jobErrorEvent.fireAsync(jobErrorMessage);
            workhorseLogService.logException(jobErrorMessage);

            throw new RuntimeException("The execution " + retryExecution + " couldn't be persisited by the persisitence.");
        }
        log.trace("Execution successfully created: {}", persistedExecution);
        return persistedExecution;
    }

    public synchronized Execution handleFailedExecution(Job job, Long executionId, Exception exception, Long duration, BaseWorker worker) {
        Execution failedExecution = executionPersistence.getById(job.getId(), executionId);
        Execution retryExecution = null;

        if (failedExecution.getFailRetry() < job.getFailRetries()) {
            retryExecution = createRetryExecution(failedExecution);
        } else if (failedExecution.getChainId() != null) {

            executionPersistence.abortChain(job.getId(), failedExecution.getChainId());
        }

        failedExecution.setStatus(ExecutionStatus.FAILED);
        failedExecution.setEndedAt(LocalDateTime.now(ZoneId.of(StaticConfig.TIME_ZONE)));
        failedExecution.setDuration(duration);

        executionPersistence.log(job.getId(), executionId, WorkhorseUtil.getMessagesFromException(exception), WorkhorseUtil.stacktraceToString(exception));

        if (retryExecution == null) {
            worker.onFailed(executionId);
            if (failedExecution.getChainId() != null) {
                worker.onFailedChain(failedExecution.getChainId(), executionId);
            }
        } else {
            worker.onRetry(executionId, retryExecution.getId());
        }

        executionPersistence.update(failedExecution);

        return retryExecution;
    }

    /**
     * Delete all executions of a job that were created for a given number of minutes
     * 
     * @param jobId ID of the job
     * @param minMinutesOld Minimum number of minutes, that an execution have to exist to be deleted
     * @return number of deleted executions
     */
    public int deleteOlderExecutions(Long jobId, long minMinutesOld) {

        // Is minMinutesOld negativ the global default value of minutesUntilCleanup is
        // to use.
        if (minMinutesOld < 0) {
            minMinutesOld = StaticConfig.MINUTES_UNTIL_CLEANUP;
        }

        LocalDateTime time = LocalDateTime.now(ZoneId.of(StaticConfig.TIME_ZONE)).minusMinutes(minMinutesOld);

        return executionPersistence.deleteOlderExecutions(jobId, time);
    }

    /**
     * Get the listing result of execution
     * 
     * @param jobId Id of the corresponding job
     * @param listingParameters defines the listing queue. It contains optional query parameters as described above
     * @return list of execution
     */
    public ListingResult<Execution> getExecutionListing(Long jobId, ListingParameters listingParameters) {
        return executionPersistence.getExecutionListing(jobId, listingParameters);
    }

    /**
     * Get the listing result of jobs
     * 
     * @param listingParameters defines the listing queue. It contains optional query parameters as described above
     * @return list of jobs
     */
    public ListingResult<Job> getJobListing(ListingParameters listingParameters) {
        return jobPersistence.getJobListing(listingParameters);
    }

    public List<Job> getAllJobs() {
        return jobPersistence.getAll();
    }

    public List<Job> getAllJobsByStatus(JobStatus jobStatus) {
        return jobPersistence.getAllByStatus(jobStatus);
    }

    public Execution getExecutionById(Long executionId) {
        return getExecutionById(null, executionId);
    }

    public Execution getExecutionById(Long jobId, Long executionId) {
        return executionPersistence.getById(jobId, executionId);
    }

    public Job getJobById(Long jobId) {
        return jobPersistence.get(jobId);
    }

    public Job getJobByClassName(String className) {
        return jobPersistence.getByName(className);
    }

    public Job update(Job job) {
        jobPersistence.update(job);
        return getJobById(job.getId());
    }

    public Job updateJob(Long jobId, String name, String description, String workerClassName, String schedule, JobStatus status, int threads,
                    Integer maxPerMinute, int failRetries, int retryDelay, int minutesUntilCleanUp, boolean uniqueQueued) {

        Job job = getJobById(jobId);

        if (!Objects.equals(job.getName(), name)) {
            workhorseLogService.logChange(jobId, status, "Name", job.getName(), name, null);
            job.setName(name);
        }
        if (!Objects.equals(job.getDescription(), description)) {
            workhorseLogService.logChange(jobId, status, "Description", job.getDescription(), description, null);
            job.setDescription(description);
        }
        if (!Objects.equals(job.getWorkerClassName(), workerClassName)) {
            workhorseLogService.logChange(jobId, status, "Worker class name", job.getWorkerClassName(), workerClassName, null);
            job.setWorkerClassName(workerClassName);
        }
        if (!Objects.equals(job.getSchedule(), schedule)) {
            workhorseLogService.logChange(jobId, status, "Schedule", job.getSchedule(), schedule, null);
            job.setSchedule(schedule);
        }
        if (!Objects.equals(job.getStatus(), status)) {
            workhorseLogService.logChange(jobId, status, "Status", job.getStatus(), status.name(), null);
            job.setStatus(status);
        }
        if (!Objects.equals(job.getThreads(), threads)) {
            workhorseLogService.logChange(jobId, status, "Threads", job.getThreads(), threads, null);
            job.setThreads(threads);
        }
        if (!Objects.equals(job.getMaxPerMinute(), maxPerMinute)) {
            workhorseLogService.logChange(jobId, status, "Max executions per minute", job.getMaxPerMinute(), maxPerMinute, null);
            job.setMaxPerMinute(maxPerMinute);
        }
        if (!Objects.equals(job.getFailRetries(), failRetries)) {
            workhorseLogService.logChange(jobId, status, "Fail retries", job.getFailRetries(), failRetries, null);
            job.setFailRetries(failRetries);
        }
        if (!Objects.equals(job.getRetryDelay(), retryDelay)) {
            workhorseLogService.logChange(jobId, status, "Retry delay", job.getRetryDelay(), retryDelay, null);
            job.setRetryDelay(retryDelay);
        }
        if (!Objects.equals(job.getMinutesUntilCleanUp(), minutesUntilCleanUp)) {
            workhorseLogService.logChange(jobId, status, "Minutes until cleanup", job.getMinutesUntilCleanUp(), minutesUntilCleanUp, null);
            job.setMinutesUntilCleanUp(minutesUntilCleanUp);
        }
        if (job.isUniqueQueued() != uniqueQueued) {
            workhorseLogService.logChange(jobId, status, "Unique in status queued", job.isUniqueQueued(), uniqueQueued, null);
            job.setUniqueQueued(uniqueQueued);
        }

        log.trace("Job updated: {}", job);

        jobPersistence.update(job);

        return job;
    }

    public List<Execution> getBatch(Long jobId, Long batchId) {
        return executionPersistence.getBatch(jobId, batchId);
    }

    public List<Execution> getchain(Long jobId, Long chainId) {
        return executionPersistence.getChain(jobId, chainId);
    }

    public ExecutionLog getExecutionLog(Long jobId, Long executionId) {

        return executionPersistence.getLog(jobId, executionId);
    }

    public List<Execution> getExecutions(Long jobId) {
        return executionPersistence.getByJobId(jobId, 100L);
    }

    public void setExecutionStatusToRunning(Execution execution) {
        updateExecutionStatus(execution, ExecutionStatus.RUNNING, ExecutionFailStatus.NONE);
    }

    public void setExecutionStatusToFinished(Execution execution) {
        updateExecutionStatus(execution, ExecutionStatus.FINISHED, ExecutionFailStatus.NONE);
    }

    /**
     * Set the status of the execution to fail.
     * 
     * The default fail status is {@link ExecutionFailStatus#EXCEPTION}
     * 
     * @param execution Execution to update
     */
    public void setExecutionStatusToFailed(Execution execution) {
        setExecutionStatusToFailed(execution, ExecutionFailStatus.EXCEPTION);
    }

    /**
     * Set the status of the execution to fail.
     * 
     * @param execution Execution to update
     * @param failStatus Status that describes the cause of the failure
     */
    public void setExecutionStatusToFailed(Execution execution, ExecutionFailStatus failStatus) {
        updateExecutionStatus(execution, ExecutionStatus.FAILED, failStatus);
    }

    public Execution updateExecutionStatus(Long jobId, Long executionId, ExecutionStatus executionStatus) {
        return executionPersistence.updateStatus(jobId, executionId, executionStatus, ExecutionFailStatus.NONE);
    }

    /**
     * Update the status of an execution.
     * 
     * @param jobId ID of the corresponding job
     * @param id ID of the execution
     * @param status New Status to set
     * @param failStatus Specific status of a failed execution
     * @return the updated execution
     */
    protected Execution updateExecutionStatus(Execution execution, ExecutionStatus status, ExecutionFailStatus failStatus) {

        switch (status) {

            case RUNNING:
                execution.setStartedAt(WorkhorseUtil.timestamp());
                break;

            case FINISHED:

                LocalDateTime endTime = WorkhorseUtil.timestamp();
                Long duration = Duration.between(execution.getStartedAt(), endTime).toMillis();
                execution.setEndedAt(endTime);
                execution.setDuration(duration);
                break;

            case FAILED:
                if (execution.getStatus().equals(ExecutionStatus.RUNNING)) {
                    LocalDateTime endFailTime = WorkhorseUtil.timestamp();
                    Long durationToFail = Duration.between(execution.getStartedAt(), endFailTime).toMillis();
                    execution.setEndedAt(endFailTime);
                    execution.setDuration(durationToFail);
                }
                break;
            default:
                break;
        }

        execution.setStatus(status);
        if (failStatus != null) {
            execution.setFailStatus(failStatus);
        }
        execution.setUpdatedAt(WorkhorseUtil.timestamp());

        return updateExecution(execution);
    }

    public Execution updateExecution(Execution execution) {
        return executionPersistence.update(execution);
    }

    public void deleteExecution(Long jobId, Long executionId) {
        executionPersistence.delete(jobId, executionId);
    }

    /**
     * Hunt timeout executions and cure them
     */
    public void huntTimeoutExecution() {

        if (StaticConfig.EXECUTION_TIMEOUT <= 0) {
            return;
        }

        LocalDateTime time = WorkhorseUtil.timestamp().minusSeconds(StaticConfig.EXECUTION_TIMEOUT);
        List<Execution> timeoutExecutions = executionPersistence.findTimeoutExecutions(time);

        if (timeoutExecutions.isEmpty()) {
            return;
        }

        for (Execution timeoutExecution : timeoutExecutions) {
            log.warn("Zombie found! {}", timeoutExecution);

            ExecutionStatus cure = StaticConfig.EXECUTION_TIMEOUT_STATUS;
            String logMessage = "Zombie execution found (ID: " + timeoutExecution.getId() + "): ";

            switch (cure) {
                case QUEUED:
                    Execution retryExecution = createRetryExecution(timeoutExecution);
                    timeoutExecution.setStatus(ExecutionStatus.FAILED);
                    log.info("Zombie killed and risen from the death! Now it is {}", retryExecution);
                    workhorseLogService.logMessage(logMessage + "Marked as failed and queued a clone", timeoutExecution.getJobId(), false);
                    break;
                case RUNNING:
                    log.warn("Zombie will still walk free with status {}", cure);
                    workhorseLogService.logMessage(logMessage + "No action is taken", timeoutExecution.getJobId(), false);
                    break;
                default:
                    timeoutExecution.setStatus(cure);
                    log.info("Zombie is cured with status {}", cure);
                    workhorseLogService.logMessage(logMessage + "Put in status " + cure, timeoutExecution.getJobId(), false);
                    break;
            }

            executionPersistence.update(timeoutExecution);

        }
    }

}
