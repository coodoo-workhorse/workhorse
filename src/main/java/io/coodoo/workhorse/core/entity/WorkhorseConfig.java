package io.coodoo.workhorse.core.entity;

import java.time.LocalDateTime;
import java.time.ZoneId;

import javax.enterprise.context.ApplicationScoped;

import io.coodoo.workhorse.persistence.interfaces.PersistenceTyp;

/**
 * @author coodoo GmbH (coodoo.io)
 */

// TODO zentrale config.
// Probieren: BuilderPattern

@ApplicationScoped
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
     * TODO implement me!
     * 
     * A zombie is an execution that is stuck in status {@link ExecutionStatus#RUNNING} for this amount of minutes (if set to 0 there the hunt is off)
     */
    private int executionTimeout = 120;

    /**
     * TODO implement me!
     * 
     * If an execution is stuck in status {@link ExecutionStatus#RUNNING} and doesn't change, it has became a zombie! Once found we have a cure!
     */
    private ExecutionStatus executionTimeoutStatus = ExecutionStatus.ABORTED;

    /**
     * Log change pattern. Placeholder <code>%s</code> for changeParameter, changeOld and changeNew in this order <br>
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

    /**
     * @return Current Time by zone defined in {@link WorkhorseConfig#TIME_ZONE}
     */
    public LocalDateTime timestamp() {
        return LocalDateTime.now(ZoneId.of(timeZone));
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public Long getJobQueueMax() {
        return bufferMax;
    }

    public void setJobQueueMax(Long jobQueueMax) {
        this.bufferMax = jobQueueMax;
    }

    public int getJobQueueMin() {
        return bufferMin;
    }

    public void setJobQueueMin(int jobQueueMin) {
        this.bufferMin = jobQueueMin;
    }

    public int getJobQueuePollerInterval() {
        return bufferPollInterval;
    }

    public void setJobQueuePollerInterval(int jobQueuePollerInterval) {
        this.bufferPollInterval = jobQueuePollerInterval;
    }

    public int getJobQueuePusherPoll() {
        return bufferPushFallbackPollInterval;
    }

    public void setJobQueuePusherPoll(int jobQueuePusherPoll) {
        this.bufferPushFallbackPollInterval = jobQueuePusherPoll;
    }

    public PersistenceTyp getPersistenceTyp() {
        return persistenceTyp;
    }

    public void setPersistenceTyp(PersistenceTyp persistenceTyp) {
        this.persistenceTyp = persistenceTyp;
    }

    public int getZombieRecognitionTime() {
        return executionTimeout;
    }

    public void setZombieRecognitionTime(int zombieRecognitionTime) {
        this.executionTimeout = zombieRecognitionTime;
    }

    public ExecutionStatus getZombieCureStatus() {
        return executionTimeoutStatus;
    }

    public void setZombieCureStatus(ExecutionStatus zombieCureStatus) {
        this.executionTimeoutStatus = zombieCureStatus;
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

}
