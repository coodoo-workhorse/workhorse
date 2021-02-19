package io.coodoo.workhorse.core.entity;

/**
 * Abstract class to build an object of type {@link WorkhorseConfig}
 * 
 * @author coodoo GmbH (coodoo.io)
 */
public abstract class WorkhorseConfigBuilder {

    protected WorkhorseConfig workhorseConfig;

    /**
     * Build an {@link WorkhorseConfig} object
     */
    public abstract WorkhorseConfig build();

    /**
     * Set another time zone. It is used to create instances of LocalDateTime.
     * 
     * @param timeZone the timeZone as ZoneId
     * @return the builder to set another configuration
     */
    public WorkhorseConfigBuilder timeZone(String timeZone) {
        workhorseConfig.setTimeZone(timeZone);
        return this;
    }

    /**
     * Set the maximum amount of executions to load into the intern buffer per job.
     * 
     * @param bufferMax the maximum amount of executions
     * @return the builder to set another configuration
     */
    public WorkhorseConfigBuilder bufferMaximumSize(Long bufferMax) {
        workhorseConfig.setBufferMax(bufferMax);
        return this;
    }

    /**
     * Set the minimum amount of executions per job to load into the intern buffer
     * before the processing begins.
     * 
     * @param bufferMin the minimum amount of executions
     * @return the builder to set another configuration
     */
    public WorkhorseConfigBuilder bufferMinimumSize(int bufferMin) {
        workhorseConfig.setBufferMin(bufferMin);
        return this;
    }

    /**
     * Set the polling interval in seconds at which the intern buffer is loaded.
     * 
     * @param bufferPollInterval the polling interval
     * @return the builder to set another configuration
     */
    public WorkhorseConfigBuilder bufferPollInterval(int bufferPollInterval) {
        workhorseConfig.setBufferPollInterval(bufferPollInterval);
        return this;
    }

    /**
     * Set the polling interval in seconds at which the intern buffer is loaded,
     * when new executions are pushed by the persistence.
     * 
     * This is used as fallback mechanism to support the persistence.
     * 
     * @param bufferPushFallbackPollInterval the polling interval, that is used,
     *                                       when the persistence can push events.
     * @return the builder to set another configuration
     */
    public WorkhorseConfigBuilder bufferPushFallbackPollInterval(int bufferPushFallbackPollInterval) {
        workhorseConfig.setBufferPushFallbackPollInterval(bufferPushFallbackPollInterval);
        return this;
    }

    /***
     * Set the number of days an execution can be held in the persistence before
     * being automatic deleted
     * 
     * @param daysUntilCleanup number of days
     * @return the builder to set another configuration
     */
    public WorkhorseConfigBuilder daysUntilCleanup(int daysUntilCleanup) {
        workhorseConfig.setDaysUntilCleanup(daysUntilCleanup);
        return this;
    }

    /**
     * Set the duration in seconds after which an execution in status
     * {@link ExecutionStatus#RUNNING} is considered as expired.
     * 
     * If set to 0 the value is ignored
     * 
     * @param executionTimeout the duration after which an running execution is
     *                         considered as expired.
     * @return the builder to set another configuration
     */
    public WorkhorseConfigBuilder executionTimeout(int executionTimeout) {
        workhorseConfig.setExecutionTimeout(executionTimeout);
        return this;
    }

    /**
     * Set the status that executions, that are stuck in status
     * {@link ExecutionStatus#RUNNING} for {@link WorkhorseConfig#executionTimeout}
     * seconds have to get to be cured.
     * 
     * @param executionTimeoutStatus status that the execution will get.
     * @return the builder to set another configuration
     */
    public WorkhorseConfigBuilder executionTimeoutStatus(ExecutionStatus executionTimeoutStatus) {
        workhorseConfig.setExecutionTimeoutStatus(executionTimeoutStatus);
        return this;
    }

    /**
     * Set the log change pattern
     * 
     * Default is <code>Changed %s from '%s' to '%s'</code>
     * 
     * @param logChange log change pattern
     * @return the builder to set another configuration
     */
    public WorkhorseConfigBuilder logChange(String logChange) {
        workhorseConfig.setLogChange(logChange);
        return this;
    }

    /**
     * Set the execution log timestamp pattern
     * 
     * Default is <code>[HH:mm:ss.SSS]</code>
     * 
     * @param logTimeFormat log timestamp pattern.
     * @return the builder to set another configuration
     */
    public WorkhorseConfigBuilder logTimeFormat(String logTimeFormat) {
        workhorseConfig.setLogTimeFormat(logTimeFormat);
        return this;
    }

    /**
     * Set the execution log info marker.
     * 
     * Default is none.
     * 
     * @param logInfoMarker log info marker
     * @return the builder to set another configuration
     */
    public WorkhorseConfigBuilder logInfoMarker(String logInfoMarker) {
        workhorseConfig.setLogInfoMarker(logInfoMarker);
        return this;
    }

    /**
     * Set the execution log warn marker.
     * 
     * Default is <code>[WARN]</code>
     * 
     * @param logWarnMarker
     * @return the builder to set another configuration
     */
    public WorkhorseConfigBuilder logWarnMarker(String logWarnMarker) {
        workhorseConfig.setLogWarnMarker(logWarnMarker);
        return this;
    }

    /**
     * Set the execution log error marker.
     * 
     * Default is <code>[ERROR]</code>
     * 
     * @param logErrorMarker log error marker
     * @return the builder to set another configuration
     */
    public WorkhorseConfigBuilder logErrorMarker(String logErrorMarker) {
        workhorseConfig.setLogErrorMarker(logErrorMarker);
        return this;
    }

}
