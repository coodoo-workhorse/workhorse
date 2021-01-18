package io.coodoo.workhorse.config.entity;

import java.time.LocalDateTime;
import java.time.ZoneId;

import javax.enterprise.context.ApplicationScoped;

import io.coodoo.workhorse.core.entity.ExecutionStatus;
import io.coodoo.workhorse.persistence.interfaces.PersistenceTyp;

/**
 * Basic configuration<br>
 * <i>There is only one entry in this table and its purpose is to persist the values of {@link JobEngineConfig}!</i>
 * 
 * @author coodoo GmbH (coodoo.io)
 */
@ApplicationScoped
public class WorkhorseConfig {

    /**
     * ZoneId Object time zone for LocalDateTime instance creation. Default is UTC
     */
    private String timeZone = "UTC+1";

    /**
     * Max amount of executions to load into the memory queue per job
     */
    private Long jobQueueMax = 10000L;

    /**
     * Min amount of executions in memory queue before the poller gets to add more
     */
    private int jobQueueMin = 1;

    /**
     * Job queue poller interval in seconds
     */
    private int jobQueuePollerInterval = 5; // In second

    /**
     * Job queue poller interval in seconds by Push
     */
    private int jobQueuePusherPoll = 120; // In second

    /**
     * Type of the peristence
     */
    private PersistenceTyp persistenceTyp = PersistenceTyp.MEMORY;

    /**
     * A zombie is an execution that is stuck in status {@link ExecutionStatus#RUNNING} for this amount of minutes (if set to 0 there the hunt is off)
     */
    private int zombieRecognitionTime = 120;

    /**
     * If an execution is stuck in status {@link ExecutionStatus#RUNNING} and doesn't change, it has became a zombie! Once found we have a cure!
     */
    private ExecutionStatus zombieCureStatus = ExecutionStatus.ABORTED;

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
    private String logInfoMarker = "[INFO]";

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

    public Long getJobQueueMax() {
        return jobQueueMax;
    }

    public void setJobQueueMax(Long jobQueueMax) {
        this.jobQueueMax = jobQueueMax;
    }

    public int getJobQueueMin() {
        return jobQueueMin;
    }

    public void setJobQueueMin(int jobQueueMin) {
        this.jobQueueMin = jobQueueMin;
    }

    public PersistenceTyp getPersistenceTyp() {
        return persistenceTyp;
    }

    public void setPersistenceTyp(PersistenceTyp persistenceTyp) {
        this.persistenceTyp = persistenceTyp;
    }

    public int getJobQueuePollerInterval() {
        return jobQueuePollerInterval;
    }

    public void setJobQueuePollerInterval(int jobQueuePollerInterval) {
        this.jobQueuePollerInterval = jobQueuePollerInterval;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    @Override
    public String toString() {
        return "JobEngineConfig [jobQueueMax=" + jobQueueMax + ", jobQueueMin=" + jobQueueMin + ", jobQueuePollerInterval=" + jobQueuePollerInterval
                        + ", jobQueuePusherPoll=" + jobQueuePusherPoll + ", persistenceTyp=" + persistenceTyp + ", timeZone=" + timeZone + "]";
    }

    public int getJobQueuePusherPoll() {
        return jobQueuePusherPoll;
    }

    public void setJobQueuePusherPoll(int jobQueueSafetyPoll) {
        this.jobQueuePusherPoll = jobQueueSafetyPoll;
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

    public int getZombieRecognitionTime() {
        return zombieRecognitionTime;
    }

    public void setZombieRecognitionTime(int zombieRecognitionTime) {
        this.zombieRecognitionTime = zombieRecognitionTime;
    }

    public ExecutionStatus getZombieCureStatus() {
        return zombieCureStatus;
    }

    public void setZombieCureStatus(ExecutionStatus zombieCureStatus) {
        this.zombieCureStatus = zombieCureStatus;
    }

}
