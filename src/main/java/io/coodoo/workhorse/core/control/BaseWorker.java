package io.coodoo.workhorse.core.control;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import javax.inject.Inject;

import org.slf4j.Logger;

import io.coodoo.workhorse.core.boundary.ExecutionContext;
import io.coodoo.workhorse.core.boundary.Worker;
import io.coodoo.workhorse.core.boundary.WorkerWith;
import io.coodoo.workhorse.core.boundary.WorkhorseLogService;
import io.coodoo.workhorse.core.entity.Execution;
import io.coodoo.workhorse.core.entity.ExecutionLog;
import io.coodoo.workhorse.core.entity.ExecutionStatus;
import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.core.entity.WorkhorseConfig;
import io.coodoo.workhorse.core.entity.WorkhorseLog;
import io.coodoo.workhorse.util.WorkhorseUtil;

/**
 * Base worker class to define the creation and processing of executions.
 * 
 * @author coodoo GmbH (coodoo.io)
 */
public abstract class BaseWorker {

    @Inject
    protected WorkhorseController workhorseController;

    @Inject
    protected ExecutionContext executionContext;

    @Inject
    protected WorkhorseLogService workhorseLogService;

    private Job job;

    /**
     * This method will be called by the schedule timer in order to check if there is stuff to do.<br>
     * Its goal is to create one (or more) {@link Execution} that gets added to the job engine to be executed. <i>If not overwritten, this method will create a
     * {@link Execution} without parameters or specific settings.</i>
     */
    public void onSchedule() {
        createExecution();
    }

    /**
     * The job engine will call this callback method after the job execution is finished. <br>
     * <i>If needed, this method can be overwritten to react on a finished job execution.</i>
     * 
     * @param executionId ID of current job execution that is finished
     */
    public void onFinished(Long executionId) {}

    /**
     * The job engine will call this callback method after the last job execution of a batch is finished. <br>
     * <i>If needed, this method can be overwritten to react on a finished batch.</i>
     * 
     * @param batchId batch ID
     * @param executionId ID of last job execution of a batch that is finished
     */
    public void onFinishedBatch(Long batchId, Long executionId) {}

    /**
     * The job engine will call this callback method after the last job execution of a chain is finished. <br>
     * <i>If needed, this method can be overwritten to react on a finished chain.</i>
     * 
     * @param chainId chain ID
     * @param executionId ID of last job execution of a chain that is finished
     */
    public void onFinishedChain(Long chainId, Long executionId) {}

    /**
     * The job engine will call this callback method after the job execution has failed and there will be a retry of the failed job execution. <br>
     * <i>If needed, this method can be overwritten to react on a retry job execution.</i>
     * 
     * @param failedExecutionId ID of current job execution that has failed
     * @param retryExecutionId ID of new job execution that that will retry the failed one
     */
    public void onRetry(Long failedExecutionId, Long retryExecutionId) {}

    /**
     * The job engine will call this callback method after the job execution has failed. <br>
     * <i>If needed, this method can be overwritten to react on a failed job execution.</i>
     * 
     * @param executionId ID of current job execution that has failed
     */
    public void onFailed(Long executionId) {}

    /**
     * The job engine will call this callback method after a batch has failed. <br>
     * <i>If needed, this method can be overwritten to react on a failed batch.</i>
     * 
     * @param batchId chain ID
     * @param executionId ID of last job execution of a batch that has failed
     */
    // FIXME restore usage:
    // https://github.com/coodoo-io/workhorse/blob/03c6ecebeed0cf0653c248f2256905cee6c49c16/src/main/java/io/coodoo/workhorse/jobengine/control/JobEngine.java#L273
    public void onFailedBatch(Long batchId, Long executionId) {}

    /**
     * The job engine will call this callback method after a chain has failed. <br>
     * <i>If needed, this method can be overwritten to react on a failed chain.</i>
     * 
     * @param chainId chain ID
     * @param executionId ID of last job execution of a chain that has failed
     */
    public void onFailedChain(Long chainId, Long executionId) {}

    /**
     * <i>This is an access point to get the job engine started with a new job execution.</i><br>
     * <br>
     * 
     * This creates a {@link Execution} object that gets added to the job engine to be executed as soon as possible.
     * 
     * @return execution ID
     */
    public Long createExecution() {
        return createExecution(null, null, null, null, null, null).getId();
    }

    protected Execution createExecution(Object parameters, Boolean priority, LocalDateTime plannedFor, LocalDateTime expiresAt, Long batchId, Long chainId) {
        Long jobId = getJob().getId();
        boolean uniqueQueued = getJob().isUniqueQueued();

        String parametersAsJson = WorkhorseUtil.parametersToJson(parameters);

        return workhorseController.createExecution(jobId, parametersAsJson, priority, plannedFor, expiresAt, batchId, chainId, uniqueQueued);

    }

    public Job getJob() {
        if (job == null) {
            job = workhorseController.getByWorkerClassName(getClassName());
        }
        return job;
    }

    /**
     * This method retrieves the exact class of this job worker. Without this, the proxy-client class will be retrieves.
     */
    public Class<? extends BaseWorker> getWorkerClass() {
        return getClass();
    }

    /**
     * This method retrieves the class's name without CDI-suffix (_SubClass) like stored in the persistence
     * 
     * @return the persisted class name
     */
    protected String getClassName() {

        // To support Quarkus 2.0
        // Using beanManager.getReference() to get an instance of a worker that we can use to call the doWork-method, the returned instance is a CDI-subclass
        // (from Worker to Worker_subClass) of the wanted class.
        // It is not fundamentally a problem, but the name of the class stored in the persistence don't have this suffix _subClass. This is the reason why we
        // have at this point to remove this suffix if existing.
        return getClass().getName().split(StaticConfig.CDI_WORKER_SUFFIX)[0];
    }

    /**
     * @return Current Time by zone defined in {@link WorkhorseConfig#TIME_ZONE}
     */
    public LocalDateTime timestamp() {
        return LocalDateTime.now(ZoneId.of(StaticConfig.TIME_ZONE));
    }

    /**
     * Adds the message text in as a new line to the executions log
     * <p>
     * <i> This will only work when used in method {@link #doWork(Execution)}! </i>
     * </p>
     * 
     * @param message text to log
     */
    protected void logLine(String message) {
        executionContext.logLine(message);
    }

    /**
     * Adds a timestamp followed by the message text in as a new line to the executions log <br>
     * Timestamp pattern: <code>[HH:mm:ss.SSS]</code> or as defined in {@link JobEngineConfig#LOG_TIME_FORMATTER}<br>
     * Example: <code>[22:06:42.680] Step 3 complete</code>
     * <p>
     * <i> This will only work when used in method {@link #doWork(Execution)}! </i>
     * </p>
     * 
     * @param message text to log
     */
    protected void logLineWithTimestamp(String message) {
        executionContext.logLineWithTimestamp(message);
    }

    /**
     * Adds a timestamp followed by an info marker and the info message text in as a new line to the executions log<br>
     * Timestamp pattern: <code>[HH:mm:ss.SSS]</code> or as defined in {@link JobEngineConfig#LOG_TIME_FORMATTER}<br>
     * Info marker: Only if defined in {@link JobEngineConfig#LOG_INFO_MARKER}<br>
     * Example: <code>[22:06:42.680] Step 3 complete</code>
     * <p>
     * <i> This will only work when used in method {@link #doWork(Execution)}! </i>
     * </p>
     * 
     * @param message text to log
     */
    protected void logInfo(String message) {
        executionContext.logInfo(message);
    }

    /**
     * Adds a timestamp followed by an info marker and the info message text in as a new line to the executions log and also adds the message in severity INFO
     * to the server log<br>
     * Timestamp pattern: <code>[HH:mm:ss.SSS]</code> or as defined in {@link JobEngineConfig#LOG_TIME_FORMATTER}<br>
     * Info marker: Only if defined in {@link JobEngineConfig#LOG_INFO_MARKER}<br>
     * Example: <code>[22:06:42.680] Step 3 complete</code>
     * <p>
     * <i> This will only work when used in method {@link #doWork(Execution)}! </i>
     * </p>
     * 
     * @param logger server log logger
     * @param message text to log
     */
    protected void logInfo(Logger logger, String message) {
        executionContext.logInfo(logger, message);
    }

    /**
     * Adds a timestamp followed by an warn marker and the warn message as a new line to the executions log<br>
     * Timestamp pattern: <code>[HH:mm:ss.SSS]</code> or as defined in {@link JobEngineConfig#LOG_TIME_FORMATTER}<br>
     * Error marker: <code>[WARN]</code> or as defined in {@link JobEngineConfig#LOG_WARN_MARKER}<br>
     * Example: <code>[22:06:42.680] [WARN] Well thats suspicious...</code>
     * <p>
     * <i> This will only work when used in method {@link #doWork(Execution)}! </i>
     * </p>
     * 
     * @param message text to log
     */
    protected void logWarn(String message) {
        executionContext.logWarn(message);
    }

    /**
     * Adds a timestamp followed by an warn marker and the warn message as a new line to the executions log. It also adds the message in severity WARN to the
     * server log<br>
     * Timestamp pattern: <code>[HH:mm:ss.SSS]</code> or as defined in {@link JobEngineConfig#LOG_TIME_FORMATTER}<br>
     * Error marker: <code>[WARN]</code> or as defined in {@link JobEngineConfig#LOG_WARN_MARKER}<br>
     * Example: <code>[22:06:42.680] [WARN] Well thats suspicious...</code>
     * <p>
     * <i> This will only work when used in method {@link #doWork(Execution)}! </i>
     * </p>
     * 
     * @param logger server log logger
     * @param message text to log
     */
    protected void logWarn(Logger logger, String message) {
        executionContext.logWarn(logger, message);
    }

    /**
     * Adds a timestamp followed by an error marker and the error message as a new line to the executions log<br>
     * Timestamp pattern: <code>[HH:mm:ss.SSS]</code> or as defined in {@link JobEngineConfig#LOG_TIME_FORMATTER}<br>
     * Error marker: <code>[ERROR]</code> or as defined in {@link JobEngineConfig#LOG_ERROR_MARKER}<br>
     * Example: <code>[22:06:42.680] [ERROR] Dafuq was that?!?!</code>
     * <p>
     * <i> This will only work when used in method {@link #doWork(Execution)}! </i>
     * </p>
     * 
     * @param message text to log
     */
    protected void logError(String message) {
        executionContext.logError(message);
    }

    /**
     * Adds a timestamp followed by an error marker and the error message as a new line to the executions log. It also adds the message in severity ERROR to the
     * server log<br>
     * Timestamp pattern: <code>[HH:mm:ss.SSS]</code> or as defined in {@link JobEngineConfig#LOG_TIME_FORMATTER}<br>
     * Error marker: <code>[ERROR]</code> or as defined in {@link JobEngineConfig#LOG_ERROR_MARKER}<br>
     * Example: <code>[22:06:42.680] [ERROR] Dafuq was that?!?!</code>
     * <p>
     * <i> This will only work when used in method {@link #doWork(Execution)}! </i>
     * </p>
     * 
     * @param logger server log logger
     * @param message text to log
     */
    protected void logError(Logger logger, String message) {
        executionContext.logError(logger, message);
    }

    /**
     * Adds a timestamp followed by an error marker and the error message as a new line to the executions log. It also adds the message in severity ERROR and
     * the throwable to the server log<br>
     * Timestamp pattern: <code>[HH:mm:ss.SSS]</code> or as defined in {@link JobEngineConfig#LOG_TIME_FORMATTER}<br>
     * Error marker: <code>[ERROR]</code> or as defined in {@link JobEngineConfig#LOG_ERROR_MARKER}<br>
     * Example: <code>[22:06:42.680] [ERROR] Dafuq was that?!?!</code>
     * <p>
     * <i> This will only work when used in method {@link #doWork(Execution)}! </i>
     * </p>
     * 
     * @param logger server log logger
     * @param message text to log
     * @param throwable cause of error
     */
    protected void logError(Logger logger, String message, Throwable throwable) {
        executionContext.logError(logger, message, throwable);
    }

    /**
     * Logs a text message directly to the job
     * 
     * @param message text to log
     * @return the resulting log entry
     */
    public WorkhorseLog logOnJob(String message) {
        return workhorseLogService.logMessage(message, getJob().getId(), false);
    }

    /**
     * @return the log text of the current running job execution or <code>null</code> if there isn't any
     */
    public String getJobExecutionLog() {
        return executionContext.getLog();
    }

    /**
     * @return the context of the current running job execution
     */
    public ExecutionContext getExecutionContext() {
        return executionContext;
    }

    /**
     * <p>
     * Add a short message to summarize current execution, that is also logged with the severity INFO
     * </p>
     * <p>
     * <i> This will only work when used in method {@link #doWork(Execution)}! </i>
     * </p>
     * The number of character in a summary can not exceed a value defined in {@link WorkhorseConfig#getMaxExecutionSummaryLength()}.<br>
     * Otherwise the summary is cut to the permitted length and the full-length summary is appended to the logs ({@link ExecutionLog#getLog()}) of the current
     * execution.
     * 
     * @param logger server log logger
     * @param summary short message to add
     */
    public void summarizeInfo(Logger logger, String summary) {
        summarizeInfo(logger, summary);
    }

    /**
     * <p>
     * Add a short message to summarize the current execution, that is also logged with the severity ERROR
     * </p>
     * <p>
     * <i> This will only work when used in method {@link #doWork(Execution)}! </i>
     * </p>
     * The number of character in a summary can not exceed a value defined in {@link WorkhorseConfig#getMaxExecutionSummaryLength()}.<br>
     * Otherwise the summary is cut to the permitted length and the full-length summary is appended to the logs ({@link ExecutionLog#getLog()}) of the current
     * execution.
     * 
     * @param logger server log logger
     * @param summary short message to add
     */
    public void summarizeError(Logger logger, String summary) {
        summarizeError(logger, summary);
    }

    /**
     * Terminate an execution of the corresponding asynchronous job
     * 
     * @param jobId ID of the job
     * @param executionId ID of the execution to terminate
     * @param summary message to summarize the execution
     * @return true if the execution could be terminated successfully
     */
    @SuppressWarnings("unchecked")
    public boolean terminateExecution(Long executionId, String summary) {

        Job job = getJob();

        // Only jobs with the flag job.asynchronous = true can be terminated outside of the JobThread
        if (job != null && job.isAsynchronous()) {

            Execution execution = workhorseController.getExecutionById(job.getId(), executionId);

            if (ExecutionStatus.RUNNING.equals(execution.getStatus())) {

                try {
                    final BaseWorker workerInstance = workhorseController.getWorker(job);
                    boolean isWorkerWithParamters = workerInstance instanceof WorkerWith;
                    Worker worker = null;
                    WorkerWith<Object> workerWith = null;
                    if (isWorkerWithParamters) {
                        workerWith = (WorkerWith<Object>) workerInstance;
                    } else {
                        worker = ((Worker) workerInstance);
                    }

                    workhorseController.finishExecution(job, execution, workerInstance, worker, workerWith, isWorkerWithParamters, workerWith, summary);
                    return true;

                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

            }
        }
        return false;
    }

    public abstract class BaseExecutionBuilder<T> {

        protected boolean priority;
        protected LocalDateTime plannedFor;
        protected LocalDateTime expiresAt;

        /**
         * Builds an execution with the defined attributes.
         * 
         * @return execution ID
         */
        public Long build() {
            return createExecution(null, priority, plannedFor, expiresAt, null, null).getId();
        }

        /**
         * Prioritize an execution over others of the worker class
         * 
         * @return the builder to set another feature
         */
        @SuppressWarnings("unchecked")
        public T prioritize() {
            this.priority = true;
            return (T) this;
        }

        /**
         * Plan the processing of an execution to a given timestamp
         * 
         * @param plannedFor plannedFor specified time for the execution
         * @return the builder to set another feature
         */
        @SuppressWarnings("unchecked")
        public T plannedFor(LocalDateTime plannedFor) {
            this.plannedFor = plannedFor;
            return (T) this;
        }

        /**
         * Delay the processing of an execution for a given amount of seconds
         * 
         * <pre>
         * Example:
         * {@code 
         * delayedForSeconds(20)
         * }
         * </pre>
         * 
         * @param delayValue time to wait in seconds
         * @return the builder to set another feature
         */
        public T delayedForSeconds(int delayValue) {
            return delayedFor(delayValue, ChronoUnit.SECONDS);
        }

        /**
         * Delay the processing of an execution for a given amount of minutes
         * 
         * <pre>
         * Example:
         * {@code 
         * delayedForMinutes(30)
         * }
         * </pre>
         * 
         * @param delayValue time to wait in minutes
         * @return the builder to set another feature
         */
        public T delayedForMinutes(int delayValue) {
            return delayedFor(delayValue, ChronoUnit.MINUTES);
        }

        /**
         * Delay the processing of an execution for a given amount of hours
         * 
         * <pre>
         * Example:
         * {@code 
         * delayedForHours(6)
         * }
         * </pre>
         * 
         * @param delayValue time to wait in hours
         * @return the builder to set another feature
         */
        public T delayedForHours(int delayValue) {
            return delayedFor(delayValue, ChronoUnit.HOURS);
        }

        /**
         * Delay the processing of an execution for a given amount of days
         * 
         * <pre>
         * Example:
         * {@code 
         * delayedForDays(12)
         * }
         * </pre>
         * 
         * @param delayValue time to wait in days
         * @return the builder to set another feature
         */
        public T delayedForDays(int delayValue) {
            return delayedFor(delayValue, ChronoUnit.DAYS);
        }

        /**
         * Delay the processing of an execution for a given amount of weeks
         * 
         * <pre>
         * Example:
         * {@code 
         * delayedForWeeks(2)
         * }
         * </pre>
         * 
         * @param delayValue time to wait in weeks
         * @return the builder to set another feature
         */
        public T delayedForWeeks(int delayValue) {
            return delayedFor(delayValue, ChronoUnit.WEEKS);
        }

        /**
         * Delay the processing of an execution for a given amount of months
         * 
         * <pre>
         * Example:
         * {@code 
         * delayedForMonths(3)
         * }
         * </pre>
         * 
         * @param delayValue time to wait in months
         * @return the builder to set another feature
         */
        public T delayedForMonths(int delayValue) {
            return delayedFor(delayValue, ChronoUnit.MONTHS);
        }

        /**
         * Delay the processing of an execution for a given amount of time
         * 
         * <pre>
         * Example:
         * {@code 
         * delayedFor(42, ChronoUnit.MINUTES)
         * }
         * </pre>
         * 
         * @param delayValue time to wait
         * @param delayUnit what kind of time to wait
         * @return the builder to set another feature
         */
        public T delayedFor(int delayValue, ChronoUnit delayUnit) {
            return delayedFor(Long.valueOf(delayValue), delayUnit);
        }

        /**
         * Delay the processing of an execution for a given amount of time
         * 
         * <pre>
         * Example:
         * {@code 
         * delayedFor(3L, ChronoUnit.SECONDS)
         * }
         * </pre>
         * 
         * @param delayValue time to wait
         * @param delayUnit what kind of time to wait
         * @return the builder to set another feature
         */
        @SuppressWarnings("unchecked")
        public T delayedFor(long delayValue, ChronoUnit delayUnit) {
            this.plannedFor = WorkhorseUtil.delayToMaturity(Long.valueOf(delayValue), delayUnit);
            return (T) this;
        }

        /**
         * Define a timestamp up to which the execution will expire (cancel), if not being processed
         * 
         * @param expiresAt specified time to cancel the execution
         * @return the builder to set another feature
         */
        @SuppressWarnings("unchecked")
        public T expiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return (T) this;
        }

        /**
         * The execution will expire within the given amount of seconds
         * 
         * <pre>
         * Example:
         * {@code 
         * expiresAtSeconds(20)
         * }
         * </pre>
         * 
         * @param expiresAt Time until expiration in seconds
         * @return the builder to set another feature
         */
        public T expiresAtSeconds(int expiresAt) {
            return expiresAt(expiresAt, ChronoUnit.SECONDS);
        }

        /**
         * The execution will expire within the given amount of minutes
         * 
         * <pre>
         * Example:
         * {@code 
         * expiresAtMinutes(30)
         * }
         * </pre>
         * 
         * @param expiresAt Time until expiration in minutes
         * @return the builder to set another feature
         */
        public T expiresAtMinutes(int expiresAt) {
            return expiresAt(expiresAt, ChronoUnit.MINUTES);
        }

        /**
         * The execution will expire within the given amount of hours
         * 
         * <pre>
         * Example:
         * {@code 
         * expiresAtHours(6)
         * }
         * </pre>
         * 
         * @param expiresAt Time until expiration in hours
         * @return the builder to set another feature
         */
        public T expiresAtHours(int expiresAt) {
            return expiresAt(expiresAt, ChronoUnit.HOURS);
        }

        /**
         * The execution will expire within the given amount of days
         * 
         * <pre>
         * Example:
         * {@code 
         * expiresAtDays(12)
         * }
         * </pre>
         * 
         * @param expiresAt Time until expiration in days
         * @return the builder to set another feature
         */
        public T expiresAtDays(int expiresAt) {
            return expiresAt(expiresAt, ChronoUnit.DAYS);
        }

        /**
         * The execution will expire within the given amount of weeks
         * 
         * <pre>
         * Example:
         * {@code 
         * expiresAtWeeks(2)
         * }
         * </pre>
         * 
         * @param expiresAt Time until expiration in weeks
         * @return the builder to set another feature
         */
        public T expiresAtWeeks(int expiresAt) {
            return expiresAt(expiresAt, ChronoUnit.WEEKS);
        }

        /**
         * The execution will expire within the given amount of months
         * 
         * <pre>
         * Example:
         * {@code 
         * expiresAtMonths(3)
         * }
         * </pre>
         * 
         * @param expiresAt Time until expiration in months
         * @return the builder to set another feature
         */
        public T expiresAtMonths(int expiresAt) {
            return expiresAt(expiresAt, ChronoUnit.MONTHS);
        }

        /**
         * The execution will expire within the given amount of time
         * 
         * <pre>
         * Example:
         * {@code 
         * expiresAt(42, ChronoUnit.MINUTES)
         * }
         * </pre>
         * 
         * @param expiresAt time to wait
         * @param delayUnit what kind of time to observe
         * @return the builder to set another feature
         */
        public T expiresAt(int expiresValue, ChronoUnit expiresUnit) {
            return expiresAt(Long.valueOf(expiresValue), expiresUnit);
        }

        /**
         * Define an period of time before the execution is expired (cancel), if not being processed
         * 
         * <pre>
         * Example:
         * {@code 
         * expiresAt(3L, ChronoUnit.SECONDS)
         * }
         * </pre>
         * 
         * @param expiresValue time to observe
         * @param expiresUnit what kind of time to observe
         * @return the builder to set another feature
         */
        @SuppressWarnings("unchecked")
        public T expiresAt(long expiresValue, ChronoUnit expiresUnit) {

            this.expiresAt = WorkhorseUtil.delayToMaturity(expiresValue, expiresUnit);
            return (T) this;
        }
    }
}
