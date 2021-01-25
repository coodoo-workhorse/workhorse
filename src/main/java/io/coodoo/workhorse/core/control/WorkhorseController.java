package io.coodoo.workhorse.core.control;

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

import org.jboss.logging.Logger;

import io.coodoo.workhorse.config.entity.WorkhorseConfig;
import io.coodoo.workhorse.core.boundary.WorkerWith;
import io.coodoo.workhorse.core.boundary.WorkhorseLogService;
import io.coodoo.workhorse.core.boundary.annotation.InitialJobConfig;
import io.coodoo.workhorse.core.control.event.JobErrorEvent;
import io.coodoo.workhorse.core.entity.ErrorType;
import io.coodoo.workhorse.core.entity.Execution;
import io.coodoo.workhorse.core.entity.ExecutionStatus;
import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.core.entity.JobStatus;
import io.coodoo.workhorse.persistence.interfaces.ExecutionPersistence;
import io.coodoo.workhorse.persistence.interfaces.JobPersistence;
import io.coodoo.workhorse.persistence.interfaces.qualifier.ExecutionQualifier;
import io.coodoo.workhorse.persistence.interfaces.qualifier.JobQualifier;
import io.coodoo.workhorse.util.WorkhorseUtil;

@ApplicationScoped
public class WorkhorseController {

    private static final Logger log = Logger.getLogger(WorkhorseController.class);

    @Inject
    @Any
    Instance<BaseWorker> workerInstances;

    @Inject
    @JobQualifier
    JobPersistence jobPersistence;

    @Inject
    WorkhorseConfig jobEngineConfig;

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

        List<Class<?>> workers = new ArrayList<>();

        // check Jobs of Worker classes
        for (BaseWorker worker : workerInstances) {
            Class<?> workerclass = worker.getWorkerClass();
            Job job = jobPersistence.getByWorkerClassName(workerclass.getName());
            if (job == null) {
                createJob(workerclass);
            }
            workers.add(workerclass);
        }

        // check Worker class of persisted job
        for (Job job : jobPersistence.getAll()) {
            try {
                Class<?> workerClass = getWorker(job).getWorkerClass();
                if (!workers.contains(workerClass)) {
                    job.setStatus(JobStatus.NO_WORKER);
                    jobPersistence.update(job.getId(), job);
                    log.error("No Worker Class found for Job: " + job);
                    log.info("JobStatus of Job " + job + " updated from " + JobStatus.NO_WORKER + " to " + JobStatus.NO_WORKER);
                    jobErrorEvent.fire(new JobErrorEvent(new Throwable(ErrorType.NO_JOB_WORKER_FOUND.getMessage()), ErrorType.NO_JOB_WORKER_FOUND.getMessage(),
                                    job.getId(), job.getStatus()));
                    continue;
                }
                if (job.getStatus().equals(JobStatus.NO_WORKER)) {
                    job.setStatus(JobStatus.INACTIVE);
                    jobPersistence.update(job.getId(), job);
                    log.info("JobStatus of Job " + job + "updated from " + JobStatus.NO_WORKER + " to " + JobStatus.INACTIVE);
                    workhorseLogService.logChange(job.getId(), job.getStatus(), " Status ", JobStatus.NO_WORKER, JobStatus.INACTIVE, " Worker class found. ");

                } else {

                    String parametersClassName = getWorkerParameterName(job);

                    // The Objects-Class is null-safe and can handle Worker-classes without
                    // Parameters
                    if (!Objects.equals(parametersClassName, job.getParametersClassName())) {
                        log.warn("Parameters class name of " + job.getWorkerClassName() + " changed from " + job.getParametersClassName() + " to "
                                        + parametersClassName);
                        workhorseLogService.logChange(job.getId(), job.getStatus(), " Parameters class ", job.getParametersClassName(), parametersClassName,
                                        null);

                        job.setParametersClassName(parametersClassName);
                        jobPersistence.update(job.getId(), job);
                    }
                }
            } catch (Exception e) {

                job.setStatus(JobStatus.ERROR);
                log.error("Can't handle Worker class found for job: " + job + " Exception " + e);
                jobErrorEvent.fire(new JobErrorEvent(e, ErrorType.ERROR_BY_FOUND_JOB_WORKER.getMessage(), job.getId(), job.getStatus()));
            }
        }
    }

    public void createJob(Class<?> workerClass) {
        Job job = new Job();

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
            job.setDaysUntilCleanUp(initialJobConfig.daysUntilCleanUp());
            job.setUniqueInQueue(initialJobConfig.uniqueInQueue());

        } else {

            // Use initial default worker informations
            job.setName(workerClass.getSimpleName());
            job.setWorkerClassName(workerClass.getName());
            job.setUniqueInQueue(InitialJobConfig.JOB_CONFIG_UNIQUE_IN_QUEUE);
            job.setStatus(JobStatus.ACTIVE);
            job.setThreads(InitialJobConfig.JOB_CONFIG_THREADS);
        }

        try {
            String parameterClassName = getWorkerParameterName(job);
            job.setParametersClassName(parameterClassName);

        } catch (Exception e) {
            log.error("Could not read parameters class name of job " + job.getName());
        }

        jobPersistence.persist(job);

        log.info("Job created:" + job);
    }

    /**
     * retrieves the parameter's name of a Job from current workspace as String.
     * 
     * @param job
     * @return
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public String getWorkerParameterName(Job job) throws Exception {

        BaseWorker worker = getWorker(job);
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
    public BaseWorker getWorker(Job job) throws ClassNotFoundException {
        for (BaseWorker worker : workerInstances) {
            if (job.getWorkerClassName().equals(worker.getWorkerClass().getName())) {
                return worker;
            }
        }

        log.error("No Worker class found for " + job);
        workhorseLogService.logChange(job.getId(), JobStatus.NO_WORKER, "Status", job.getStatus(), JobStatus.NO_WORKER, null);

        job.setStatus(JobStatus.NO_WORKER);
        jobPersistence.update(job.getId(), job);

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
    public void triggerScheduledJobExecutionCreation(Job job) throws ClassNotFoundException {
        BaseWorker worker = getWorker(job);
        worker.createJobExecution();
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
     * @param jobId Id of the correspondant JOB
     * @param parameters parameters of the execution
     * @param priority if <code>true</code> the execution will be process before other execution. Otherwise the execution will be process in order of add.
     * @param maturity If a maturity is given, the job execution will not be executed before this time.
     * @param batchId Id to refer to a group of executions to handle as a single entity.
     * @param chainId Id to refer to a group of executions to process by an order.
     * @param chainedPreviousExecutionId Id to the previous execution to process, if the execution belong to a chained JobExecution.
     * @param uniqueInQueue
     * @return the created Job Execution
     */
    public Execution createJobExecution(Long jobId, String parameters, Boolean priority, LocalDateTime maturity, Long batchId, Long chainId,
                    Long chainedPreviousExecutionId, boolean uniqueInQueue) {

        if (uniqueInQueue) {
            // To Do: Look for an Execution with the given parameterHash and return it
        }
        Execution jobExecution = new Execution();
        jobExecution.setJobId(jobId);
        jobExecution.setStatus(ExecutionStatus.QUEUED);
        jobExecution.setParameters(parameters);
        jobExecution.setPriority(priority != null ? priority : false);
        jobExecution.setMaturity(maturity);
        jobExecution.setBatchId(batchId);
        jobExecution.setChainId(chainId);
        jobExecution.setChainedPreviousExecutionId(chainedPreviousExecutionId);

        // Temporar add. Have to be replace as soon as possible.
        if (chainId != null) {
            jobExecution.setChainedNextExecutionId(-1L);
        }

        executionPersistence.persist(jobExecution);
        log.info("JobExecution successfully created: " + jobExecution);
        return jobExecution;

    }

    /**
     * Create a new job execution to retry an execution
     * 
     * @param failedJobExecution job execution to retry
     * @return the created job execution
     */
    public Execution createRetryExecution(Execution failedJobExecution) {

        Execution retryExecution = new Execution();
        retryExecution.setJobId(failedJobExecution.getJobId());
        retryExecution.setStatus(failedJobExecution.getStatus());
        retryExecution.setStartedAt(LocalDateTime.now(ZoneId.of(jobEngineConfig.getTimeZone())));
        retryExecution.setPriority(failedJobExecution.getPriority());
        retryExecution.setMaturity(failedJobExecution.getMaturity());
        retryExecution.setChainId(failedJobExecution.getChainId());
        retryExecution.setChainedNextExecutionId(failedJobExecution.getChainedNextExecutionId());
        retryExecution.setChainedPreviousExecutionId(failedJobExecution.getChainedPreviousExecutionId());
        retryExecution.setParameters(failedJobExecution.getParameters());
        retryExecution.setParametersHash(failedJobExecution.getParametersHash());

        // increase failure number
        retryExecution.setFailRetry(failedJobExecution.getFailRetry() + 1);
        if (retryExecution.getFailRetryJobExecutionId() == null) {
            retryExecution.setFailRetryJobExecutionId(failedJobExecution.getId());
        }

        executionPersistence.persist(retryExecution);
        return retryExecution;

    }

    public synchronized Execution handleFailedJobExecution(Job job, Long jobExecutionId, Exception exception, Long duration, BaseWorker worker,
                    String jobExecutionLog) {
        Execution failedJobExecution = executionPersistence.getById(job.getId(), jobExecutionId);
        Execution retryJobExecution = null;

        if (failedJobExecution.getFailRetry() < job.getFailRetries()) {
            retryJobExecution = createRetryExecution(failedJobExecution);
        } else if (failedJobExecution.getChainId() != null) {

            executionPersistence.abortChain(job.getId(), failedJobExecution.getChainId());
        }

        failedJobExecution.setStatus(ExecutionStatus.FAILED);
        failedJobExecution.setEndedAt(LocalDateTime.now(ZoneId.of(jobEngineConfig.getTimeZone())));
        failedJobExecution.setDuration(duration);

        failedJobExecution.setLog(jobExecutionLog);
        failedJobExecution.setFailMessage(WorkhorseUtil.getMessagesFromException(exception));
        failedJobExecution.setFailStacktrace(WorkhorseUtil.stacktraceToString(exception));

        if (retryJobExecution == null) {
            worker.onFailed(jobExecutionId);
            if (failedJobExecution.getChainId() != null) {
                worker.onFailedChain(failedJobExecution.getChainId(), jobExecutionId);
            }
        } else {
            worker.onRetry(jobExecutionId, retryJobExecution.getId());
        }

        executionPersistence.update(job.getId(), failedJobExecution.getId(), failedJobExecution);

        return retryJobExecution;
    }

    public int deleteOlderExecutions(Long jobId, int minDaysOld) {
        return executionPersistence.deleteOlderExecutions(jobId, LocalDateTime.now(ZoneId.of(jobEngineConfig.getTimeZone())).minusMinutes(minDaysOld));
    }

    public List<Job> getAllJobs() {
        return jobPersistence.getAll();
    }

    public List<Job> getAllJobsByStatus(JobStatus jobStatus) {
        return jobPersistence.getAllByStatus(jobStatus);
    }

    public Execution getJobExecutionById(Long jobExecutionId) {
        return getJobExecutionById(null, jobExecutionId);
    }

    public Execution getJobExecutionById(Long jobId, Long jobExecutionId) {
        return executionPersistence.getById(jobId, jobExecutionId);
    }

    public Job getJobById(Long jobId) {
        return jobPersistence.get(jobId);
    }

    public Job update(Long jobId, Job job) {
        jobPersistence.update(jobId, job);
        return getJobById(jobId);
    }

    public Job updateJob(Long jobId, String name, String description, String workerClassName, String schedule, JobStatus status, int threads,
                    Integer maxPerMinute, int failRetries, int retryDelay, int daysUntilCleanUp, boolean uniqueInQueue) {

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
        if (!Objects.equals(job.getDaysUntilCleanUp(), daysUntilCleanUp)) {
            workhorseLogService.logChange(jobId, status, "Days until cleanup", job.getDaysUntilCleanUp(), daysUntilCleanUp, null);
            job.setDaysUntilCleanUp(daysUntilCleanUp);
        }
        if (!Objects.equals(job.isUniqueInQueue(), uniqueInQueue)) {
            workhorseLogService.logChange(jobId, status, "Unique in queue", job.isUniqueInQueue(), uniqueInQueue, null);
            job.setUniqueInQueue(uniqueInQueue);
        }

        log.info("Job updated: " + job);

        jobPersistence.update(jobId, job);

        return job;
    }

    public List<Execution> getBatch(Long jobId, Long batchId) {
        return executionPersistence.getBatch(jobId, batchId);
    }

    public List<Execution> getchain(Long jobId, Long chainId) {
        return executionPersistence.getChain(jobId, chainId);
    }

    public List<Execution> getExecutions(Long jobId) {
        return executionPersistence.getByJobId(jobId, 100L);
    }

    public Execution updateJobExecution(Long jobId, Long jobExecutionId, Execution jobExecution) {
        return executionPersistence.update(jobId, jobExecutionId, jobExecution);
    }

    public void deleteJobExecution(Long jobId, Long jobExecutionId) {
        executionPersistence.delete(jobId, jobExecutionId);
    }

    public void addJobExecutionAtEndOfChain(Long jobId, Long chainId, Execution jobExecution) {
        executionPersistence.addJobExecutionAtEndOfChain(jobId, chainId, jobExecution);
    }

}
