package io.coodoo.workhorse.core.entity;

import java.time.LocalDateTime;
import java.time.ZoneId;

import io.coodoo.workhorse.persistence.interfaces.PersistenceTyp;

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
     * TODO
     */
    private int bufferPushFallbackPollInterval = 120;

    /**
     * Type of the peristence
     */
    // TODO wird ein STring mit dem namen "persistenceName"
    private PersistenceTyp persistenceTyp = PersistenceTyp.MEMORY;

    /**
     * Configuration for the choosen persistence.
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

    public WorkhorseConfig() {
    }

    public WorkhorseConfig(String timeZone, Long bufferMax, int bufferMin, int bufferPollInterval,
            int bufferPushFallbackPollInterval, PersistenceTyp persistenceTyp, int executionTimeout,
            ExecutionStatus executionTimeoutStatus, String logChange, String logTimeFormat, String logInfoMarker,
            String logWarnMarker, String logErrorMarker) {
        this.timeZone = timeZone;
        this.bufferMax = bufferMax;
        this.bufferMin = bufferMin;
        this.bufferPollInterval = bufferPollInterval;
        this.bufferPushFallbackPollInterval = bufferPushFallbackPollInterval;
        this.persistenceTyp = persistenceTyp;
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

    public Long getJobQueueMax() {
        return bufferMax;
    }

    public WorkhorseConfig setJobQueueMax(Long jobQueueMax) {
        this.bufferMax = jobQueueMax;
        return this;
    }

    public int getJobQueueMin() {
        return bufferMin;
    }

    public WorkhorseConfig setJobQueueMin(int jobQueueMin) {
        this.bufferMin = jobQueueMin;
        return this;
    }

    public int getJobQueuePollerInterval() {
        return bufferPollInterval;
    }

    public WorkhorseConfig setJobQueuePollerInterval(int jobQueuePollerInterval) {
        this.bufferPollInterval = jobQueuePollerInterval;
        return this;
    }

    public int getJobQueuePusherPoll() {
        return bufferPushFallbackPollInterval;
    }

    public WorkhorseConfig setJobQueuePusherPoll(int jobQueuePusherPoll) {
        this.bufferPushFallbackPollInterval = jobQueuePusherPoll;
        return this;
    }

    public PersistenceTyp getPersistenceTyp() {
        return persistenceTyp;
    }

    public Object getPersistenceConfig() {
        return persistenceConfig;
    }

    public WorkhorseConfig setPersistenceTyp(PersistenceTyp persistenceTyp, Object persistenceConfig) {
        this.persistenceTyp = persistenceTyp;
        this.persistenceConfig = persistenceConfig;
        return this;
    }

    public int getZombieRecognitionTime() {
        return executionTimeout;
    }

    public WorkhorseConfig setZombieRecognitionTime(int zombieRecognitionTime) {
        this.executionTimeout = zombieRecognitionTime;
        return this;
    }

    public ExecutionStatus getZombieCureStatus() {
        return executionTimeoutStatus;
    }

    public WorkhorseConfig setZombieCureStatus(ExecutionStatus zombieCureStatus) {
        this.executionTimeoutStatus = zombieCureStatus;
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
