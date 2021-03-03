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
    protected StringBuffer logBuffer;

    @Inject
    ExecutionPersistence executionPersistence;

    public void init(Execution execution) {
        this.execution = execution;
        if (execution != null && execution.getLog() != null) {
            this.logBuffer = new StringBuffer(execution.getLog());
        } else {
            this.logBuffer = new StringBuffer();
        }
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

        if (logBuffer != null && logBuffer.length() > 0) {
            return logBuffer.toString();
        }
        return null;
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
     * @param logger  server log logger
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
     * @param logger  server log logger
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
     * @param logger  server log logger
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
     * @param logger  server log logger
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
     * @param logger    server log logger
     * @param message   text to log
     * @param throwable cause of error
     */
    public void logError(Logger logger, String message, Throwable throwable) {
        logger.error(message, throwable);
        logError(message);
    }

    protected void appendLog(String message, boolean timestamp, String mode) {

        if (logBuffer != null) {
            if (logBuffer.length() > 0) {
                logBuffer.append(System.lineSeparator());
            }
            if (timestamp) {
                DateTimeFormatter logTimeFormat = DateTimeFormatter.ofPattern(StaticConfig.LOG_TIME_FORMATTER);
                logBuffer.append(WorkhorseUtil.timestamp().format(logTimeFormat));
                logBuffer.append(" ");
            }
            switch (mode) {
                case "i":
                    if (StaticConfig.LOG_INFO_MARKER != null) {
                        logBuffer.append(StaticConfig.LOG_INFO_MARKER);
                        logBuffer.append(" ");
                    }
                    break;
                case "w":
                    if (StaticConfig.LOG_WARN_MARKER != null) {
                        logBuffer.append(StaticConfig.LOG_WARN_MARKER);
                        logBuffer.append(" ");
                    }
                    break;
                case "e":
                    if (StaticConfig.LOG_ERROR_MARKER != null) {
                        logBuffer.append(StaticConfig.LOG_ERROR_MARKER);
                        logBuffer.append(" ");
                    }
                    break;
                default:
                    break;
            }
            logBuffer.append(message);

            if (executionPersistence != null) {
                executionPersistence.log(getJobId(), getExecutionId(), message);
            }

        }
    }

}
