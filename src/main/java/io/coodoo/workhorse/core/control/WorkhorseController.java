package io.coodoo.workhorse.core.control;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.coodoo.workhorse.core.boundary.ExecutionContext;
import io.coodoo.workhorse.core.boundary.Worker;
import io.coodoo.workhorse.core.boundary.WorkerWith;
import io.coodoo.workhorse.core.boundary.WorkhorseLogService;
import io.coodoo.workhorse.core.boundary.annotation.InitialJobConfig;
import io.coodoo.workhorse.core.control.event.JobErrorEvent;
import io.coodoo.workhorse.core.entity.ErrorType;
import io.coodoo.workhorse.core.entity.Execution;
import io.coodoo.workhorse.core.entity.ExecutionFailStatus;
import io.coodoo.workhorse.core.entity.ExecutionLog;
import io.coodoo.workhorse.core.entity.ExecutionStatus;
import io.coodoo.workhorse.core.entity.ExecutionStatusCounts;
import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.core.entity.JobExecutionStatusSummary;
import io.coodoo.workhorse.core.entity.JobStatus;
import io.coodoo.workhorse.core.entity.JobStatusCount;
import io.coodoo.workhorse.persistence.interfaces.ExecutionPersistence;
import io.coodoo.workhorse.persistence.interfaces.JobPersistence;
import io.coodoo.workhorse.persistence.interfaces.listing.ListingParameters;
import io.coodoo.workhorse.persistence.interfaces.listing.ListingResult;
import io.coodoo.workhorse.persistence.interfaces.qualifier.ExecutionQualifier;
import io.coodoo.workhorse.persistence.interfaces.qualifier.JobQualifier;
import io.coodoo.workhorse.util.CronExpression;
import io.coodoo.workhorse.util.WorkhorseUtil;

/**
 * Class to access to {@link Job} and {@link Execution}
 * 
 * @author coodoo GmbH (coodoo.io)
 */
@ApplicationScoped
public class WorkhorseController {

    private static final Logger log = LoggerFactory.getLogger(WorkhorseController.class);

    @Inject
    BeanManager beanManager;

    @Inject
    ExecutionBuffer executionBuffer;

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

    @Inject
    ExecutionContext executionContext;

    /**
     * Load all worker-Class of the classpath
     */
    public void loadWorkers() {

        List<Class<?>> workerClasses = new ArrayList<>();
        @SuppressWarnings("serial")
        Set<Bean<?>> beans = beanManager.getBeans(BaseWorker.class, new AnnotationLiteral<Any>() {});
        List<Bean<?>> sortedBeans = new ArrayList<>(beans);
        // sort Beans by class name to avoid shuffled Job-IDs
        sortedBeans.sort(Comparator.comparing(bean -> bean.getBeanClass().getName()));
        // check whether new worker exists and must be created and persisted
        for (Bean<?> bean : sortedBeans) {
            Class<?> workerclass = bean.getBeanClass();
            Job job = jobPersistence.getByWorkerClassName(workerclass.getName());
            if (job == null) {
                try {
                    job = createJob(workerclass);
                    log.info("Registered new {}", job);
                } catch (RuntimeException runtimeException) {
                    log.error(runtimeException.getMessage());
                }
            }
            workerClasses.add(workerclass);
        }

        // check if persisted jobs can be mapped a with a worker class
        for (Job job : jobPersistence.getAll()) {
            Class<?> workerClass;
            try {
                workerClass = getWorkerClass(job);
            } catch (ClassNotFoundException exception) {

                job.setStatus(JobStatus.ERROR);
                log.error("Worker class not found for {}", job);
                jobErrorEvent.fire(new JobErrorEvent(exception, ErrorType.ERROR_BY_FOUND_JOB_WORKER.getMessage(), job.getId(), job.getStatus()));
                continue;
            }

            if (!workerClasses.contains(workerClass)) {
                log.trace("JobStatus of {} updated from {} to {}", job, job.getStatus(), JobStatus.NO_WORKER);
                job.setStatus(JobStatus.NO_WORKER);
                jobPersistence.update(job);
                log.error("Worker class not found for {}", job);
                jobErrorEvent.fire(new JobErrorEvent(new Throwable(ErrorType.NO_JOB_WORKER_FOUND.getMessage()), ErrorType.NO_JOB_WORKER_FOUND.getMessage(),
                                job.getId(), job.getStatus()));
                continue;
            }

            // Maybe in an iteration before there was no worker and now the worker is available - set this job to inactive
            if (job.getStatus().equals(JobStatus.NO_WORKER)) {
                job.setStatus(JobStatus.INACTIVE);
                jobPersistence.update(job);
                log.info("Status of {} updated from {} to {}", job, JobStatus.NO_WORKER, JobStatus.INACTIVE);
                workhorseLogService.logChange(job.getId(), job.getStatus(), "Status", JobStatus.NO_WORKER, JobStatus.INACTIVE, "Worker class found.");

            }

            // Check if parameter class has changed
            String parametersClassName = getWorkerParameterName(workerClass);
            // The Objects-Class is null-safe and can handle Worker-classes without Parameters
            if (!Objects.equals(parametersClassName, job.getParametersClassName())) {
                log.info("Parameters class changed from {} to {} for {}", job.getParametersClassName(), parametersClassName, job);
                workhorseLogService.logChange(job.getId(), job.getStatus(), "Parameters class", job.getParametersClassName(), parametersClassName, null);

                job.setParametersClassName(parametersClassName);
                jobPersistence.update(job);
            }
        }
    }

    protected Job createJob(Class<?> workerClass) {
        Job job = new Job();

        if (workerClass.isAnnotationPresent(InitialJobConfig.class)) {
            InitialJobConfig initialJobConfig = workerClass.getAnnotation(InitialJobConfig.class);
            if (!initialJobConfig.name().isEmpty()) {
                job.setName(initialJobConfig.name());
            } else {
                job.setName(workerClass.getSimpleName());
            }

            job.setDescription(initialJobConfig.description());

            // Convert the string of tags to a list of them
            if (!initialJobConfig.tags().isEmpty()) {
                List<String> tagsList = new ArrayList<>(Arrays.asList(initialJobConfig.tags().split(",")));
                tagsList.removeAll(Collections.singleton(null));
                tagsList.removeAll(Collections.singleton(""));
                job.setTags(tagsList);
            }

            job.setWorkerClassName(workerClass.getName());
            job.setStatus(initialJobConfig.status());

            // Set the defined schedule
            String schedule = initialJobConfig.schedule();
            if (schedule != null && !schedule.isEmpty()) {

                try {
                    // Try to interpret the schedule as cron-syntax
                    new CronExpression(schedule);

                    job.setSchedule(schedule);

                } catch (RuntimeException runtimeException) {

                    String exceptionMessage = "The job with worker's name " + workerClass.getName() + " could not be created due to invalid schedule: "
                                    + schedule + "\n" + runtimeException.getMessage();

                    JobErrorEvent jobErrorMessage = new JobErrorEvent(new RuntimeException(exceptionMessage), exceptionMessage, null, null);

                    jobErrorEvent.fireAsync(jobErrorMessage);

                    workhorseLogService.logException(jobErrorMessage);

                    throw new RuntimeException(exceptionMessage);
                }
            }
            job.setThreads(initialJobConfig.threads());

            if (initialJobConfig.maxPerMinute() != InitialJobConfig.JOB_CONFIG_MAX_PER_MINUTE) {
                job.setMaxPerMinute(initialJobConfig.maxPerMinute());
            }
            job.setFailRetries(initialJobConfig.failRetries());
            job.setRetryDelay(initialJobConfig.retryDelay());

            // Use workhorse default cleanup config if configuration is set to number less zero
            if (initialJobConfig.minutesUntilCleanUp() < 0) {
                job.setMinutesUntilCleanUp((int) StaticConfig.MINUTES_UNTIL_CLEANUP);
            } else if (initialJobConfig.minutesUntilCleanUp() >= 0) {
                job.setMinutesUntilCleanUp(initialJobConfig.minutesUntilCleanUp());
            }

            job.setUniqueQueued(initialJobConfig.uniqueQueued());

            job.setAsynchronous(initialJobConfig.asynchronous());

        } else {

            // Use initial default worker informations
            job.setName(workerClass.getSimpleName());
            job.setWorkerClassName(workerClass.getName());
            job.setUniqueQueued(InitialJobConfig.JOB_CONFIG_UNIQUE_IN_QUEUE);
            job.setStatus(JobStatus.ACTIVE);
            job.setThreads(InitialJobConfig.JOB_CONFIG_THREADS);
            job.setAsynchronous(InitialJobConfig.JOB_CONFIG_ASYNCHRONOUS);

            // In this case the value to use is the default one defined by StaticConfig.MINUTES_UNTIL_CLEANUP
            job.setMinutesUntilCleanUp((int) StaticConfig.MINUTES_UNTIL_CLEANUP);
        }

        String parameterClassName = getWorkerParameterName(workerClass);
        job.setParametersClassName(parameterClassName);

        Job persistedJob = jobPersistence.persist(job);

        if (persistedJob == null || persistedJob.getId() == null) {
            String exceptionMessage = "The job " + job.getName() + " couldn't be persisited by the persisitence " + jobPersistence.getPersistenceName();
            JobErrorEvent jobErrorMessage = new JobErrorEvent(new RuntimeException(exceptionMessage), exceptionMessage, null, null);

            jobErrorEvent.fireAsync(jobErrorMessage);

            workhorseLogService.logException(jobErrorMessage);

            throw new RuntimeException(exceptionMessage);
        }

        log.trace("Created {}", persistedJob);
        return job;
    }

    public void deleteJob(Long jobId) {
        jobPersistence.deleteJob(jobId);
    }

    /**
     * Retrieves the name of the parameter of a worker that extends the class {@link WorkerWith}.
     * 
     * @param worker class of the worker
     * @return name of the parameter
     */
    @SuppressWarnings("rawtypes")
    public String getWorkerParameterName(Class<?> worker) {
        Object workerInstance;
        try {
            workerInstance = worker.newInstance();
            log.trace("new instance created: {}", workerInstance);
            if (workerInstance instanceof WorkerWith) {
                log.trace("new instance created is a workerWith: {}", workerInstance);
                return ((WorkerWith) workerInstance).getParametersClassName();
            }
        } catch (InstantiationException | IllegalAccessException e) {

            log.error("An new instance of {} could not be created. {}", worker, e);
        }
        return null;
    }

    /**
     * Get a job by the worker's classname
     * 
     * @param workerClassName
     * @return the founded job
     */
    public Job getByWorkerClassName(String workerClassName) {
        return jobPersistence.getByWorkerClassName(workerClassName);
    }

    /**
     * retrieves the Worker of a Job.
     * 
     * @param job
     * @return Baseworker
     * @throws ClassNotFoundException
     * @throws Exception
     */
    public BaseWorker getWorker(Job job) throws ClassNotFoundException {
        @SuppressWarnings("serial")
        Set<Bean<?>> beans = beanManager.getBeans(BaseWorker.class, new AnnotationLiteral<Any>() {});
        for (Bean<?> bean : beans) {
            Class<?> workerclass = bean.getBeanClass();
            if (job.getWorkerClassName().equals(workerclass.getName())) {
                CreationalContext<?> creationalContext = beanManager.createCreationalContext(bean);
                return (BaseWorker) beanManager.getReference(bean, bean.getBeanClass(), creationalContext);
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
     * retrieves the Worker of a Job.
     * 
     * @param job
     * @return Baseworker
     * @throws ClassNotFoundException
     * @throws Exception
     */
    private Class<?> getWorkerClass(Job job) throws ClassNotFoundException {
        @SuppressWarnings("serial")
        Set<Bean<?>> beans = beanManager.getBeans(BaseWorker.class, new AnnotationLiteral<Any>() {});
        for (Bean<?> bean : beans) {
            Class<?> workerclass = bean.getBeanClass();
            if (job.getWorkerClassName().equals(workerclass.getName())) {
                return workerclass;
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
     * create an {@link Execution}
     * 
     * @param jobId Id of the corresponding job
     * @param parameters parameters of the execution
     * @param priority if <code>true</code> the execution will be process before other execution. Otherwise the execution will be process in order of add.
     * @param plannedFor If a plannedFor is given, the job execution will not be executed before this time.
     * @param expiresAt If expiresAt is given, the execution have to be process before this time. Otherwise the execution is cancelled.
     * @param batchId Id to refer to a group of executions to handle as a single entity.
     * @param chainId Id to refer to a group of executions to process by an order.
     * @param uniqueQueued if true then no more than one execution with specified parameters can be queued at the time.
     * @return the created execution
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
            // Pr??fen ob es bereits eine excecution mit diesen parametern existiert und
            // im Status QUEUED ist. Wenn ja diese zur??ckgeben.
            Execution equalQueuedJobExcecution = executionPersistence.getFirstCreatedByJobIdAndParametersHash(jobId, parametersHash);
            if (equalQueuedJobExcecution != null) {
                log.trace("Attempt to create {} that is already in queue! Parameters: {}", equalQueuedJobExcecution, parameters);
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
     * Set the execution on status FINISHED and calls the callback method onFinished() and/or onFinishedBatch()
     * 
     * @param job the corresponding job
     * @param execution the execution to finish
     * @param workerInstance an instance of {@link BaseWorker}
     * @param worker an instance of {@link Worker}
     * @param workerWith an instance of {@link WorkerWith}
     * @param isWorkerWithParameters is true if the job take parameters (instance of WorkerWith)
     * @param parameters the parameters used during the executions
     * @param summary a message to summarize the execution
     */
    public void finishExecution(Job job, Execution execution, BaseWorker workerInstance, Worker worker, WorkerWith<Object> workerWith,
                    boolean isWorkerWithParameters, Object parameters, String summary) {

        if (summary != null && !summary.isEmpty()) {
            executionContext.summarize(execution, summary);
        }

        setExecutionStatusToFinished(execution);

        log.trace("Execution {}, duration: {} was successfull", execution.getId(), execution.getDuration());
        executionBuffer.removeRunningExecution(job.getId(), execution.getId());

        if (isWorkerWithParameters) {
            workerWith.onFinished(job.getId(), parameters, summary);
        } else {
            worker.onFinished(job.getId(), summary);
        }
        if (executionPersistence.isBatchFinished(job.getId(), execution.getBatchId())) {
            workerInstance.onFinishedBatch(execution.getBatchId(), execution.getId());
        }
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

    /***
     * Handle a failed execution by creating another one, if permitted by configurations.
     * 
     * @param job correspondent job
     * @param executionId ID of the execution that failed
     * @param throwable Cause of the failure occurred during execution processing
     * @param duration duration of the execution
     * @param isWorkerWithParameters <code>true</code>, if the workers instance is based on {@link WorkerWith}
     * @param worker instance of {@link Worker}, given if parameter <code>isWorkerWithParameters</code> is <code>false</code>
     * @param workerWith instance of {@link WorkerWith}, given if parameter <code>isWorkerWithParameters</code> is <code>true</code>
     * @param parameters job parameters object if parameter <code>isWorkerWithParameters</code> is <code>true</code>
     * @return new created/cloned execution
     */
    public synchronized Execution handleFailedExecution(Job job, Long executionId, Throwable throwable, Long duration, boolean isWorkerWithParameters,
                    Worker worker, WorkerWith<Object> workerWith, Object parameters) {

        executionBuffer.removeRunningExecution(job.getId(), executionId);

        Execution failedExecution = executionPersistence.getById(job.getId(), executionId);
        Execution retryExecution = null;

        log.error("The execution failed", throwable);
        if (failedExecution == null) {
            String message = "The execution with ID: " + executionId + " of job: " + job.getName() + " with JobID: " + job.getId()
                            + " could not be found in the persistence.";
            log.error(message);
            workhorseLogService.logMessage(message, job.getId(), false);
            return null;
        }

        // Asynchronous Job didn't support Retry Execution
        if (!job.isAsynchronous() && failedExecution.getFailRetry() < job.getFailRetries()) {
            retryExecution = createRetryExecution(failedExecution);
        } else if (failedExecution.getChainId() != null) {

            executionPersistence.abortChain(job.getId(), failedExecution.getChainId());
        }

        failedExecution.setStatus(ExecutionStatus.FAILED);
        failedExecution.setEndedAt(LocalDateTime.now(ZoneId.of(StaticConfig.TIME_ZONE)));
        failedExecution.setDuration(duration);

        if (failedExecution.getSummary() == null) {
            failedExecution.setSummary(WorkhorseUtil.getMessagesFromException(throwable));
        } else {
            executionPersistence.log(job.getId(), executionId, WorkhorseUtil.getMessagesFromException(throwable));
        }
        executionPersistence.logStacktrace(job.getId(), executionId, WorkhorseUtil.stacktraceToString(throwable));

        if (isWorkerWithParameters) {
            if (retryExecution == null) {
                workerWith.onFailed(executionId, parameters, throwable);
                if (failedExecution.getChainId() != null) {
                    workerWith.onFailedChain(failedExecution.getChainId(), executionId);
                }
            } else {
                workerWith.onRetry(executionId, retryExecution.getId(), parameters, throwable);
            }
        } else {
            if (retryExecution == null) {
                worker.onFailed(executionId, throwable);
                if (failedExecution.getChainId() != null) {
                    worker.onFailedChain(failedExecution.getChainId(), executionId);
                }
            } else {
                worker.onRetry(executionId, retryExecution.getId(), throwable);
            }
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

        // In this case the cleanup of executions is disabled for this job
        if (minMinutesOld == 0) {
            return 0;
        }

        // In this case the value to use is the default one defined by StaticConfig.MINUTES_UNTIL_CLEANUP
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
        return updateJob(job.getId(), job.getName(), job.getDescription(), job.getWorkerClassName(), job.getSchedule(), job.getStatus(), job.getThreads(),
                        job.getMaxPerMinute(), job.getFailRetries(), job.getRetryDelay(), job.getMinutesUntilCleanUp(), job.isUniqueQueued());
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

        return jobPersistence.update(job);
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

    public void appendExecutionLog(Long jobId, Long executionId, String log) {
        executionPersistence.log(jobId, executionId, log);
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
     * Get all jobs, whose executions are in the given status
     * 
     * @param status status of execution
     * @param since only executions that have been created after this timestamp have to be returned. If null, it is no more considered
     * @return list of job
     */
    public List<JobExecutionStatusSummary> getJobExecutionStatusSummaries(ExecutionStatus status, LocalDateTime since) {
        return executionPersistence.getJobExecutionStatusSummaries(status, since);
    }

    /**
     * Retrieves the counts of {@link Execution} by status for a specific job or for all jobs between a time interval
     * 
     * @param jobId ID of the corresponding job
     * @param from only executions that were created after this timestamp are considered
     * @param to only executions that were created before this timestamp are considered
     * @return {@link JobExecutionCount}
     */
    public ExecutionStatusCounts getExecutionStatusCounts(Long jobId, LocalDateTime from, LocalDateTime to) {
        return executionPersistence.getExecutionStatusCounts(jobId, from, to);
    }

    /**
     * Get the count of jobs by status
     * 
     * @return JobStatusCount
     */
    public JobStatusCount getJobStatusCount() {
        return jobPersistence.getJobStatusCount();
    }

}
