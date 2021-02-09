package io.coodoo.workhorse.core.entity;

/**
 * @author coodoo GmbH (coodoo.io)
 */
public class WorkhorseConfig {

    /**
     * ZoneId Object time zone for LocalDateTime instance creation. Default is UTC
     */
    private String timeZone = "UTC";

    /**
     * Max amount of executions to load into the memory queue per job
     */
    private Long bufferMax = 10000L;

    /**
     * Min amount of executions in memory queue before the poller gets to add more
     */
    private int bufferMin = 1;

    /**
     * Job queue poller interval in seconds
     */
    private int bufferPollInterval = 5;

    /**
     * TODO kommentieren
     */
    private int bufferPushFallbackPollInterval = 120;

    /**
     * TODO implement me!
     * 
     * A zombie is an execution that is stuck in status
     * {@link ExecutionStatus#RUNNING} for this amount of minutes (if set to 0 there
     * the hunt is off)
     */
    private int executionTimeout = 120;

    /**
     * TODO implement me!
     * 
     * If an execution is stuck in status {@link ExecutionStatus#RUNNING} and
     * doesn't change, it has became a zombie! Once found we have a cure!
     */
    private ExecutionStatus executionTimeoutStatus = ExecutionStatus.ABORTED;

    /**
     * Log change pattern. Placeholder <code>%s</code> for changeParameter,
     * changeOld and changeNew in this order <br>
     * Default is <code>Changed %s from '%s' to '%s'</code>
     */
    private String logChange = "%s changed from '%s' to '%s'";

    /**
     * Execution log timestamp pattern. Default is <code>[HH:mm:ss.SSS]</code>
     */
    private String logTimeFormat = "'['HH:mm:ss.SSS']'";

    /**
     * Execution log info marker. Default is none
     */
    private String logInfoMarker = "";

    /**
     * Execution log warn marker. Default is <code>[WARN]</code>
     */
    private String logWarnMarker = "[WARN]";

    /**
     * Execution log error marker. Default is <code>[ERROR]</code>
     */
    private String logErrorMarker = "[ERROR]";

    public WorkhorseConfig() {
    }

    public WorkhorseConfig(String timeZone, Long bufferMax, int bufferMin, int bufferPollInterval,
            int bufferPushFallbackPollInterval, int executionTimeout, ExecutionStatus executionTimeoutStatus,
            String logChange, String logTimeFormat, String logInfoMarker, String logWarnMarker, String logErrorMarker) {
        this.timeZone = timeZone;
        this.bufferMax = bufferMax;
        this.bufferMin = bufferMin;
        this.bufferPollInterval = bufferPollInterval;
        this.bufferPushFallbackPollInterval = bufferPushFallbackPollInterval;
        this.executionTimeout = executionTimeout;
        this.executionTimeoutStatus = executionTimeoutStatus;
        this.logChange = logChange;
        this.logTimeFormat = logTimeFormat;
        this.logInfoMarker = logInfoMarker;
        this.logWarnMarker = logWarnMarker;
        this.logErrorMarker = logErrorMarker;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public WorkhorseConfig setTimeZone(String timeZone) {
        this.timeZone = timeZone;
        return this;
    }

    public Long getBufferMax() {
        return bufferMax;
    }

    public WorkhorseConfig setBufferMax(Long bufferMax) {
        this.bufferMax = bufferMax;
        return this;
    }

    public int getBufferMin() {
        return bufferMin;
    }

    public WorkhorseConfig setBufferMin(int bufferMin) {
        this.bufferMin = bufferMin;
        return this;
    }

    public int getBufferPollInterval() {
        return bufferPollInterval;
    }

    public WorkhorseConfig setBufferPollInterval(int bufferPollInterval) {
        this.bufferPollInterval = bufferPollInterval;
        return this;
    }

    public int getBufferPushFallbackPollInterval() {
        return bufferPushFallbackPollInterval;
    }

    public WorkhorseConfig setBufferPushFallbackPollInterval(int bufferPushFallbackPollInterval) {
        this.bufferPushFallbackPollInterval = bufferPushFallbackPollInterval;
        return this;
    }

    public int getExecutionTimeout() {
        return executionTimeout;
    }

    public WorkhorseConfig setExecutionTimeout(int executionTimeout) {
        this.executionTimeout = executionTimeout;
        return this;
    }

    public ExecutionStatus getExecutionTimeoutStatus() {
        return executionTimeoutStatus;
    }

    public WorkhorseConfig setExecutionTimeoutStatus(ExecutionStatus executionTimeoutStatus) {
        this.executionTimeoutStatus = executionTimeoutStatus;
        return this;
    }

    public String getLogChange() {
        return logChange;
    }

    public WorkhorseConfig setLogChange(String logChange) {
        this.logChange = logChange;
        return this;
    }

    public String getLogTimeFormat() {
        return logTimeFormat;
    }

    public WorkhorseConfig setLogTimeFormat(String logTimeFormat) {
        this.logTimeFormat = logTimeFormat;
        return this;
    }

    public String getLogInfoMarker() {
        return logInfoMarker;
    }

    public WorkhorseConfig setLogInfoMarker(String logInfoMarker) {
        this.logInfoMarker = logInfoMarker;
        return this;
    }

    public String getLogWarnMarker() {
        return logWarnMarker;
    }

    public WorkhorseConfig setLogWarnMarker(String logWarnMarker) {
        this.logWarnMarker = logWarnMarker;
        return this;
    }

    public String getLogErrorMarker() {
        return logErrorMarker;
    }

    public WorkhorseConfig setLogErrorMarker(String logErrorMarker) {
        this.logErrorMarker = logErrorMarker;
        return this;
    }

}
