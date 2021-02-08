package io.coodoo.workhorse.core.control;

import io.coodoo.workhorse.core.entity.ExecutionStatus;
import io.coodoo.workhorse.core.entity.WorkhorseConfig;

public class ConfigBuilder {

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
     * TODO
     */
    private int bufferPushFallbackPollInterval = 120;

    /**
     * name of the persistenceistence
     */
    // TODO wird ein STring mit dem namen "persistenceName"
    private String persistenceTyp = "MEMORY";

    /**
     * Configuration for the choosen persistence
     */
    private Object persistenceConfig = null;
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

    public ConfigBuilder() {
    }

    public ConfigBuilder(String timeZone) {

        this.timeZone = timeZone;
    }

    public ConfigBuilder setJobQueueMax(Long jobQueueMax) {
        this.bufferMax = jobQueueMax;
        return this;
    }

    public ConfigBuilder setJobQueueMin(int jobQueueMin) {
        this.bufferMin = jobQueueMin;
        return this;
    }

    public ConfigBuilder setJobQueuePollerInterval(int jobQueuePollerInterval) {
        this.bufferPollInterval = jobQueuePollerInterval;
        return this;
    }

    public ConfigBuilder setJobQueuePusherPoll(int jobQueuePusherPoll) {
        this.bufferPushFallbackPollInterval = jobQueuePusherPoll;
        return this;
    }

    public ConfigBuilder setPersistenceTyp(String persistenceTyp) {
        this.persistenceTyp = persistenceTyp;
        return this;
    }

    public ConfigBuilder setZombieRecognitionTime(int zombieRecognitionTime) {
        this.executionTimeout = zombieRecognitionTime;
        return this;
    }

    public ConfigBuilder setZombieCureStatus(ExecutionStatus zombieCureStatus) {
        this.executionTimeoutStatus = zombieCureStatus;
        return this;
    }

    public ConfigBuilder setLogChange(String logChange) {
        this.logChange = logChange;
        return this;
    }

    public ConfigBuilder setLogTimeFormat(String logTimeFormat) {
        this.logTimeFormat = logTimeFormat;
        return this;
    }

    public ConfigBuilder setLogInfoMarker(String logInfoMarker) {
        this.logInfoMarker = logInfoMarker;
        return this;
    }

    public ConfigBuilder setLogWarnMarker(String logWarnMarker) {
        this.logWarnMarker = logWarnMarker;
        return this;
    }

    public ConfigBuilder setLogErrorMarker(String logErrorMarker) {
        this.logErrorMarker = logErrorMarker;
        return this;
    }

    public ConfigBuilder setPersistence(String persistenceTyp, Object persistenceConfig) {
        this.persistenceTyp = persistenceTyp;
        this.persistenceConfig = persistenceConfig;
        return this;
    }

    public GlobalConfig build() {
        return new GlobalConfig(new WorkhorseConfig(timeZone, bufferMax, bufferMin, bufferPollInterval,
                bufferPushFallbackPollInterval, persistenceTyp, executionTimeout, executionTimeoutStatus, logChange,
                logTimeFormat, logInfoMarker, logWarnMarker, logErrorMarker), persistenceTyp, persistenceConfig);
    }
}
