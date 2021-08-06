package io.coodoo.workhorse.core.boundary;

import java.time.format.DateTimeFormatter;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.slf4j.Logger;

import io.coodoo.workhorse.core.control.StaticConfig;
import io.coodoo.workhorse.core.entity.Execution;
import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.core.entity.WorkhorseConfig;
import io.coodoo.workhorse.persistence.interfaces.ExecutionPersistence;
import io.coodoo.workhorse.persistence.interfaces.qualifier.ExecutionQualifier;
import io.coodoo.workhorse.util.WorkhorseUtil;

/**
 * This class gets the access to an running execution from anywhere in the business logic. It allows access for logging and to read meta data from the current
 * job and execution while the doWork()-method of the worker class is invoked.
 * 
 * @author coodoo GmbH (coodoo.io)
 */
@RequestScoped
public class ExecutionContext {

    // The visibility is protected to enable mocking in Junit Tests
    protected Execution execution;

    @Inject
    @ExecutionQualifier
    ExecutionPersistence executionPersistence;

    public void init(Execution execution) {
        this.execution = execution;
    }

    /**
     * Retrieves the ID of the {@link Job} object of the current execution. WARNING: Don't change the value of the job with the retrieved ID. Changes can be
     * fatal or have no effect.
     * 
     * @return the ID of the job
     */
    public Long getJobId() {
        if (execution == null) {
            return null;
        }
        return execution.getJobId();
    }

    /**
     * Retrieves the ID of the current execution. WARNING: Don't change the value of the execution with the retrieved ID. Changes can be fatal or have no
     * effect.
     * 
     * @return the ID of the execution
     */
    public Long getExecutionId() {
        if (execution == null) {
            return null;
        }
        return execution.getId();
    }

    /**
     * Retrieves the messages logged during the processing of the current execution
     * 
     * @return the messages
     */
    public String getLog() {

        return executionPersistence.getLog(execution.getJobId(), execution.getId()).getLog();
    }

    /**
     * Adds the message text in as a new line to the executions log
     * 
     * @param message text to log
     */
    public void logLine(String message) {
        appendLog(message, false, "l");
    }

    /**
     * Adds the message text in as a new line to the executions log and also adds the message in severity INFO to the server log
     * 
     * @param logger server log logger
     * @param message text to log
     */
    public void logLine(Logger logger, String message) {
        logger.info(message);
        logLine(message);
    }

    /**
     * Adds a timestamp followed by the message text in as a new line to the executions log <br>
     * Timestamp pattern: <code>[HH:mm:ss.SSS]</code> or as defined in {@link WorkhorseConfig#LOG_TIME_FORMATTER}<br>
     * Example: <code>[22:06:42.680] Step 3 complete</code>
     * 
     * @param message text to log
     */
    public void logLineWithTimestamp(String message) {
        appendLog(message, true, "lt");
    }

    /**
     * Adds a timestamp followed by an info marker and the info message text in as a new line to the executions log<br>
     * Timestamp pattern: <code>[HH:mm:ss.SSS]</code> or as defined in {@link WorkhorseConfig#LOG_TIME_FORMATTER}<br>
     * Info marker: Only if defined in {@link WorkhorseConfig#LOG_INFO_MARKER}<br>
     * Example: <code>[22:06:42.680] Step 3 complete</code>
     * 
     * @param message text to log
     */
    public void logInfo(String message) {
        appendLog(message, true, "i");
    }

    /**
     * Adds a timestamp followed by an info marker and the info message text in as a new line to the executions log and also adds the message in severity INFO
     * to the server log<br>
     * Timestamp pattern: <code>[HH:mm:ss.SSS]</code> or as defined in {@link WorkhorseConfig#LOG_TIME_FORMATTER}<br>
     * Info marker: Only if defined in {@link WorkhorseConfig#LOG_INFO_MARKER}<br>
     * Example: <code>[22:06:42.680] Step 3 complete</code>
     * 
     * @param logger server log logger
     * @param message text to log
     */
    public void logInfo(Logger logger, String message) {
        logger.info(message);
        logInfo(message);
    }

    /**
     * Adds a timestamp followed by an warn marker and the warn message as a new line to the executions log<br>
     * Timestamp pattern: <code>[HH:mm:ss.SSS]</code> or as defined in {@link WorkhorseConfig#LOG_TIME_FORMATTER}<br>
     * Error marker: <code>[WARN]</code> or as defined in {@link WorkhorseConfig#LOG_WARN_MARKER}<br>
     * Example: <code>[22:06:42.680] [WARN] Well thats suspicious...</code>
     * 
     * @param message text to log
     */
    public void logWarn(String message) {
        appendLog(message, true, "w");
    }

    /**
     * Adds a timestamp followed by an warn marker and the warn message as a new line to the executions log. It also adds the message in severity WARN to the
     * server log<br>
     * Timestamp pattern: <code>[HH:mm:ss.SSS]</code> or as defined in {@link WorkhorseConfig#LOG_TIME_FORMATTER}<br>
     * Error marker: <code>[WARN]</code> or as defined in {@link WorkhorseConfig#LOG_WARN_MARKER}<br>
     * Example: <code>[22:06:42.680] [WARN] Well thats suspicious...</code>
     * 
     * @param logger server log logger
     * @param message text to log
     */
    public void logWarn(Logger logger, String message) {
        logger.warn(message);
        logWarn(message);
    }

    /**
     * Adds a timestamp followed by an error marker and the error message as a new line to the executions log<br>
     * Timestamp pattern: <code>[HH:mm:ss.SSS]</code> or as defined in {@link WorkhorseConfig#LOG_TIME_FORMATTER}<br>
     * Error marker: <code>[ERROR]</code> or as defined in {@link WorkhorseConfig#LOG_ERROR_MARKER}<br>
     * Example: <code>[22:06:42.680] [ERROR] Dafuq was that?!?!</code>
     * 
     * @param message text to log
     */
    public void logError(String message) {
        appendLog(message, true, "e");
    }

    /**
     * Adds a timestamp followed by an error marker and the error message as a new line to the executions log. It also adds the message in severity ERROR to the
     * server log<br>
     * Timestamp pattern: <code>[HH:mm:ss.SSS]</code> or as defined in {@link WorkhorseConfig#LOG_TIME_FORMATTER}<br>
     * Error marker: <code>[ERROR]</code> or as defined in {@link WorkhorseConfig#LOG_ERROR_MARKER}<br>
     * Example: <code>[22:06:42.680] [ERROR] Dafuq was that?!?!</code>
     * 
     * @param logger server log logger
     * @param message text to log
     */
    public void logError(Logger logger, String message) {
        logger.error(message);
        logError(message);
    }

    /**
     * Adds a timestamp followed by an error marker and the error message as a new line to the executions log. It also adds the message in severity ERROR and
     * the throwable to the server log<br>
     * Timestamp pattern: <code>[HH:mm:ss.SSS]</code> or as defined in {@link WorkhorseConfig#LOG_TIME_FORMATTER}<br>
     * Error marker: <code>[ERROR]</code> or as defined in {@link WorkhorseConfig#LOG_ERROR_MARKER}<br>
     * Example: <code>[22:06:42.680] [ERROR] Dafuq was that?!?!</code>
     * 
     * @param logger server log logger
     * @param message text to log
     * @param throwable cause of error
     */
    public void logError(Logger logger, String message, Throwable throwable) {
        logger.error(message, throwable);
        logError(message);
    }

    /**
     * <p>
     * Add a short message to summarize this execution.
     * </p>
     * The number of character in a summary can not exceed a value defined in {@link WorkhorseConfig#getMaxExecutionSummaryLength()}.<br>
     * Otherwise the summary is cut to the permitted length and the full-length summary is appended to the logs ({@link ExecutionLog#getLog()}) of the current
     * execution.
     * 
     * @param summary short message to add
     */
    public void summerize(String summary) {

        // If the execution context is used in a custom service it can be invoked without an execution present.
        // It also check if the summary is empty or null.
        if (executionPersistence == null || execution == null || execution.getId() == null || summary == null || summary.trim().isEmpty()) {
            return;
        }

        StringBuilder summaryToPersist = new StringBuilder();
        int maxSummaryLength = StaticConfig.MAX_EXECUTION_SUMMARY_LENGTH;

        if (summary.length() > maxSummaryLength) {

            summaryToPersist.append(summary.substring(0, maxSummaryLength));
            summaryToPersist.append("...");

            StringBuilder summaryToPersistInExecutionLog = new StringBuilder();

            DateTimeFormatter logTimeFormat = DateTimeFormatter.ofPattern(StaticConfig.LOG_TIME_FORMATTER);
            String time = WorkhorseUtil.timestamp().format(logTimeFormat) + " ";

            String marker = time + "[SUMMARY]" + " ";

            summaryToPersistInExecutionLog.append(marker).append(summary);
            executionPersistence.log(getJobId(), getExecutionId(), summaryToPersistInExecutionLog.toString());
        } else {
            summaryToPersist.append(summary);
        }

        execution.setSummary(summaryToPersist.toString());

        // No special update of the summary field is defined as adding a summary-information do not occur often. Only one execution of a series holds the
        // summary of all executions of this series.
        executionPersistence.update(execution);

    }

    protected void appendLog(String message, boolean timestamp, String mode) {

        // If the execution context is used in a custom service it can be invoked without an execution present.
        if (executionPersistence == null || execution == null || execution.getId() == null) {
            return;
        }

        String marker = "";
        String time = "";

        if (timestamp) {
            DateTimeFormatter logTimeFormat = DateTimeFormatter.ofPattern(StaticConfig.LOG_TIME_FORMATTER);
            time = WorkhorseUtil.timestamp().format(logTimeFormat) + " ";
        }

        switch (mode) {
            case "i":
                if (StaticConfig.LOG_INFO_MARKER != null) {

                    marker = time + StaticConfig.LOG_INFO_MARKER + " ";
                }
                break;
            case "w":
                if (StaticConfig.LOG_WARN_MARKER != null) {

                    marker = time + StaticConfig.LOG_WARN_MARKER + " ";
                }
                break;
            case "e":
                if (StaticConfig.LOG_ERROR_MARKER != null) {

                    marker = time + StaticConfig.LOG_ERROR_MARKER + " ";
                }
                break;
            default:
                break;
        }

        String log = marker + message;
        executionPersistence.log(getJobId(), getExecutionId(), log);
    }

}
