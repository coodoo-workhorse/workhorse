package io.coodoo.workhorse.core.boundary;

import java.time.format.DateTimeFormatter;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.slf4j.Logger;

import io.coodoo.workhorse.config.entity.WorkhorseConfig;
import io.coodoo.workhorse.core.entity.Execution;
import io.coodoo.workhorse.core.entity.Job;

/**
 * @author coodoo GmbH (coodoo.io)
 */
@RequestScoped
public class JobContext {

    // TODO wieder einführen!

    @Inject
    WorkhorseConfig jobEngineConfig;

    protected Job job;

    protected Execution jobExecution;

    private StringBuffer logBuffer;

    public void init(Execution jobExecution) {

        this.jobExecution = jobExecution;
        if (jobExecution != null && jobExecution.getLog() != null) {
            this.logBuffer = new StringBuffer(jobExecution.getLog());
        } else {
            this.logBuffer = new StringBuffer();
        }
    }

    public Execution getJobExecution() {
        return jobExecution;
    }

    public Long getJobId() {
        return jobExecution.getJobId();
    }

    public Long getJobExecutionId() {
        return jobExecution.getId();
    }

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

    private void appendLog(String message, boolean timestamp, String mode) {

        if (logBuffer != null) {
            if (logBuffer.length() > 0) {
                logBuffer.append(System.lineSeparator());
            }
            if (timestamp) {
                DateTimeFormatter logTimeFormat = DateTimeFormatter.ofPattern(jobEngineConfig.getLogTimeFormat());
                logBuffer.append(jobEngineConfig.timestamp().format(logTimeFormat));
                logBuffer.append(" ");
            }
            switch (mode) {
                case "i":
                    if (jobEngineConfig.getLogInfoMarker() != null) {
                        logBuffer.append(jobEngineConfig.getLogInfoMarker());
                        logBuffer.append(" ");
                    }
                    break;
                case "w":
                    if (jobEngineConfig.getLogWarnMarker() != null) {
                        logBuffer.append(jobEngineConfig.getLogWarnMarker());
                        logBuffer.append(" ");
                    }
                    break;
                case "e":
                    if (jobEngineConfig.getLogErrorMarker() != null) {
                        logBuffer.append(jobEngineConfig.getLogErrorMarker());
                        logBuffer.append(" ");
                    }
                    break;
                default:
                    break;
            }
            logBuffer.append(message);
        }
    }

}
