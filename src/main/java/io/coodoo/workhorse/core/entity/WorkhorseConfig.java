package io.coodoo.workhorse.core.entity;

import java.time.ZoneId;

/**
 * The class defines all the configurations that can be applied to Workhorse to adapt it for multiples usage.
 * 
 * @author coodoo GmbH (coodoo.io)
 */
public abstract class WorkhorseConfig {

    /**
     * ZoneId for LocalDateTime instance creation. The default setting is that defined by the system.
     */
    protected String timeZone = ZoneId.systemDefault().getId();

    /**
     * Max amount of executions to load into the intern buffer per job
     */
    protected int bufferMax = 1000;

    /**
     * Min amount of executions in intern buffer before the processing begins
     */
    protected int bufferMin = 1;

    /**
     * Polling interval in seconds at which the intern buffer is loaded
     */
    protected int bufferPollInterval = 5;

    /**
     * Polling interval in seconds at which the intern buffer is loaded, that is used as fallback mechanism when new Executions are pushed by the persistence
     */
    protected int bufferPushFallbackPollInterval = 120;

    /**
     * Number of minutes an execution can be held in the persistence before being automatically deleted.
     * 
     * Default is 30 days (432000 minutes) but it depends on the active persictence.
     * 
     * If set to 0, no cleanup is performed.
     */
    protected long minutesUntilCleanup = 432000l;

    /**
     * Duration in seconds after which an EXECUTION in status {@link ExecutionStatus#RUNNING} is consider in timeout.(if set to 0 the value is ignored)
     */
    protected int executionTimeout = 120;

    /**
     * Max length of the summary of an execution. Default is <code>140</code>
     */
    protected int maxExecutionSummaryLength = 140;

    /**
     * 
     * If an execution is stuck in status {@link ExecutionStatus#RUNNING} and doesn't change for {@link WorkhorseConfig#executionTimeout} seconds, it is in
     * timeout!
     * 
     * <code>executionTimeoutStatus</code> defines which status this execution in timeout have to get.
     */
    protected ExecutionStatus executionTimeoutStatus = ExecutionStatus.ABORTED;

    /**
     * Log change pattern. Placeholder <code>%s</code> for changeParameter, changeOld and changeNew in this order <br>
     * Default is <code>Changed %s from '%s' to '%s'</code>
     */
    protected String logChange = "%s changed from '%s' to '%s'";

    /**
     * Execution log timestamp pattern. Default is <code>[HH:mm:ss.SSS]</code>
     */
    protected String logTimeFormat = "'['HH:mm:ss.SSS']'";

    /**
     * Execution log info marker. Default is none
     */
    protected String logInfoMarker = "";

    /**
     * Execution log warn marker. Default is <code>[WARN]</code>
     */
    protected String logWarnMarker = "[WARN]";

    /**
     * Execution log error marker. Default is <code>[ERROR]</code>
     */
    protected String logErrorMarker = "[ERROR]";

    /**
     * Retrieves the name of the persistence that extends the class WorkhorseConfig.
     * 
     * @return the name of the persistence
     */
    public abstract String getPersistenceName();

    /**
     * Retrieves the version of the persistence that extends the class WorkhorseConfig.
     * 
     * @return the version of the persistence
     */
    public abstract String getPersistenceVersion();

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public int getBufferMax() {
        return bufferMax;
    }

    public void setBufferMax(int bufferMax) {
        this.bufferMax = bufferMax;
    }

    public int getBufferMin() {
        return bufferMin;
    }

    public void setBufferMin(int bufferMin) {
        this.bufferMin = bufferMin;
    }

    public int getBufferPollInterval() {
        return bufferPollInterval;
    }

    public void setBufferPollInterval(int bufferPollInterval) {
        this.bufferPollInterval = bufferPollInterval;
    }

    public int getBufferPushFallbackPollInterval() {
        return bufferPushFallbackPollInterval;
    }

    public void setBufferPushFallbackPollInterval(int bufferPushFallbackPollInterval) {
        this.bufferPushFallbackPollInterval = bufferPushFallbackPollInterval;
    }

    public long getMinutesUntilCleanup() {
        return minutesUntilCleanup;
    }

    public void setMinutesUntilCleanup(long minutesUntilCleanup) {
        this.minutesUntilCleanup = minutesUntilCleanup;
    }

    public int getExecutionTimeout() {
        return executionTimeout;
    }

    public void setExecutionTimeout(int executionTimeout) {
        this.executionTimeout = executionTimeout;
    }

    public int getMaxExecutionSummaryLength() {
        return maxExecutionSummaryLength;
    }

    public void setMaxExecutionSummaryLength(int maxExecutionSummaryLength) {
        this.maxExecutionSummaryLength = maxExecutionSummaryLength;
    }

    public ExecutionStatus getExecutionTimeoutStatus() {
        return executionTimeoutStatus;
    }

    public void setExecutionTimeoutStatus(ExecutionStatus executionTimeoutStatus) {
        this.executionTimeoutStatus = executionTimeoutStatus;
    }

    public String getLogChange() {
        return logChange;
    }

    public void setLogChange(String logChange) {
        this.logChange = logChange;
    }

    public String getLogTimeFormat() {
        return logTimeFormat;
    }

    public void setLogTimeFormat(String logTimeFormat) {
        this.logTimeFormat = logTimeFormat;
    }

    public String getLogInfoMarker() {
        return logInfoMarker;
    }

    public void setLogInfoMarker(String logInfoMarker) {
        this.logInfoMarker = logInfoMarker;
    }

    public String getLogWarnMarker() {
        return logWarnMarker;
    }

    public void setLogWarnMarker(String logWarnMarker) {
        this.logWarnMarker = logWarnMarker;
    }

    public String getLogErrorMarker() {
        return logErrorMarker;
    }

    public void setLogErrorMarker(String logErrorMarker) {
        this.logErrorMarker = logErrorMarker;
    }

    @Override
    public String toString() {
        return "WorkhorseConfig [bufferMax=" + bufferMax + ", bufferMin=" + bufferMin + ", bufferPollInterval=" + bufferPollInterval
                        + ", bufferPushFallbackPollInterval=" + bufferPushFallbackPollInterval + ", executionTimeout=" + executionTimeout
                        + ", executionTimeoutStatus=" + executionTimeoutStatus + ", logChange=" + logChange + ", logErrorMarker=" + logErrorMarker
                        + ", logInfoMarker=" + logInfoMarker + ", logTimeFormat=" + logTimeFormat + ", logWarnMarker=" + logWarnMarker
                        + ", maxExecutionSummaryLength=" + maxExecutionSummaryLength + ", minutesUntilCleanup=" + minutesUntilCleanup + ", timeZone=" + timeZone
                        + "]";
    }

}
