package io.coodoo.workhorse.core.control;

import java.time.LocalDateTime;
import java.time.ZoneId;

import javax.inject.Inject;

import org.slf4j.Logger;

import io.coodoo.workhorse.core.boundary.ExecutionContext;
import io.coodoo.workhorse.core.boundary.WorkhorseLogService;
import io.coodoo.workhorse.core.entity.Execution;
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
     * The job engine will uses this method to perform the execution.
     * 
     * @param execution execution object, containing parameters and meta information
     * @throws Exception in case the execution fails
     */
    public abstract void doWork(Execution execution) throws Exception;

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
            job = workhorseController.getByWorkerClassName(getClass().getName());
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
     * @return Current Time by zone defined in {@link WorkhorseConfig#TIME_ZONE}
     */
    public LocalDateTime timestamp() {
        return LocalDateTime.now(ZoneId.of(StaticConfig.TIME_ZONE));
    }

    /**
     * Adds the message text in as a new line to the executions log <br>
     * <i>CAUTION: This will only work in the context of the doWork method!</i>
     * 
     * @param message text to log
     */
    protected void logLine(String message) {
        executionContext.logLine(message);
    }

    /**
     * Adds a timestamp followed by the message text in as a new line to the executions log <br>
     * Timestamp pattern: <code>[HH:mm:ss.SSS]</code> or as defined in {@link JobEngineConfig#LOG_TIME_FORMATTER}<br>
     * Example: <code>[22:06:42.680] Step 3 complete</code> <br>
     * <i>CAUTION: This will only work in the context of the doWork method!</i>
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
     * Example: <code>[22:06:42.680] Step 3 complete</code> <br>
     * <i>CAUTION: This will only work in the context of the doWork method!</i>
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
     * Example: <code>[22:06:42.680] Step 3 complete</code> <br>
     * <i>CAUTION: This will only work in the context of the doWork method!</i>
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
     * Example: <code>[22:06:42.680] [WARN] Well thats suspicious...</code> <br>
     * <i>CAUTION: This will only work in the context of the doWork method!</i>
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
     * Example: <code>[22:06:42.680] [WARN] Well thats suspicious...</code> <br>
     * <i>CAUTION: This will only work in the context of the doWork method!</i>
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
     * Example: <code>[22:06:42.680] [ERROR] Dafuq was that?!?!</code> <br>
     * <i>CAUTION: This will only work in the context of the doWork method!</i>
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
     * Example: <code>[22:06:42.680] [ERROR] Dafuq was that?!?!</code> <br>
     * <i>CAUTION: This will only work in the context of the doWork method!</i>
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
     * Example: <code>[22:06:42.680] [ERROR] Dafuq was that?!?!</code> <br>
     * <i>CAUTION: This will only work in the context of the doWork method!</i>
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
     * Add a message to summarize the last processed executions of this job
     * 
     * @param summary message to add
     */
    public void summarize(String summary) {
        workhorseController.summerizeExecutions(executionContext.getJobId(), executionContext.getExecutionId(), summary);
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
}
