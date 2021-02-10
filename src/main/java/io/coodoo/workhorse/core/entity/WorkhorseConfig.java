package io.coodoo.workhorse.core.entity;

/**
 * @author coodoo GmbH (coodoo.io)
 */
public class WorkhorseConfig {

    /**
     * ZoneId for LocalDateTime instance creation. Default is UTC
     */
    private String timeZone = "UTC";

    /**
     * Max amount of executions to load into the intern buffer per job
     */
    private Long bufferMax = 1000L;

    /**
     * Min amount of executions in intern buffer before the procesing begins
     */
    private int bufferMin = 1;

    /**
     * Polling interval in seconds at which the intern buffer is loaded
     */
    private int bufferPollInterval = 5;

    /**
     * Polling interval in seconds at which the intern buffer is loaded, that is
     * used as fallback mechanism when new Executions are pushed by the persistence
     */
    private int bufferPushFallbackPollInterval = 120;

    /**
     * Duration in seconds after which an EXECUTION in status
     * {@link ExecutionStatus#RUNNING} is consider as zombie oder expired.(if set to
     * 0 the value is ignored)
     */
    private int executionTimeout = 120;

    /**
     * 
     * If an execution is stuck in status {@link ExecutionStatus#RUNNING} and
     * doesn't change for {@link WorkhorseConfig#executionTimeout} seconds, it is
     * expired!
     * 
     * <code>executionTimeoutStatus</code> defines which status this expired
     * Execution have to get.
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

    @Override
    public String toString() {
        return "WorkhorseConfig [timeZone=" + timeZone + ", bufferMax=" + bufferMax + ", bufferMin=" + bufferMin
                + ", bufferPollInterval=" + bufferPollInterval + ", bufferPushFallbackPollInterval="
                + bufferPushFallbackPollInterval + ", executionTimeout=" + executionTimeout
                + ", executionTimeoutStatus=" + executionTimeoutStatus + ", logChange=" + logChange + ", logTimeFormat="
                + logTimeFormat + ", logInfoMarker=" + logInfoMarker + ", logWarnMarker=" + logWarnMarker
                + ", logErrorMarker=" + logErrorMarker + "]";
    }

}
