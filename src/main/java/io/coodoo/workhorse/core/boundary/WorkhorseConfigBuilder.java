package io.coodoo.workhorse.core.boundary;

import io.coodoo.workhorse.core.entity.ExecutionStatus;
import io.coodoo.workhorse.core.entity.WorkhorseConfig;

public abstract class WorkhorseConfigBuilder {

    protected WorkhorseConfig workhorseConfig;

    public abstract WorkhorseConfig build();

    /**
     * Set another time zone. It is used to create instances of LocalDateTime.
     * 
     * @param timeZone
     * @return WorkhorseConfigBuilder
     */
    public WorkhorseConfigBuilder timeZone(String timeZone) {
        workhorseConfig.setTimeZone(timeZone);
        return this;
    }

    public WorkhorseConfigBuilder bufferMaximumSize(Long bufferMax) {
        workhorseConfig.setBufferMax(bufferMax);
        return this;
    }

    /**
     * Set the
     * 
     * @param bufferMin
     * @return
     */
    public WorkhorseConfigBuilder bufferMinimumSize(int bufferMin) {
        workhorseConfig.setBufferMin(bufferMin);
        return this;
    }

    public WorkhorseConfigBuilder bufferPollInterval(int bufferPollInterval) {
        workhorseConfig.setBufferPollInterval(bufferPollInterval);
        return this;
    }

    public WorkhorseConfigBuilder bufferPushFallbackPollInterval(int bufferPushFallbackPollInterval) {
        workhorseConfig.setBufferPushFallbackPollInterval(bufferPushFallbackPollInterval);
        return this;
    }

    public WorkhorseConfigBuilder executionTimeout(int executionTimeout) {
        workhorseConfig.setExecutionTimeout(executionTimeout);
        return this;
    }

    public WorkhorseConfigBuilder executionTimeoutStatus(ExecutionStatus executionTimeoutStatus) {
        workhorseConfig.setExecutionTimeoutStatus(executionTimeoutStatus);
        return this;
    }

    public WorkhorseConfigBuilder logChange(String logChange) {
        workhorseConfig.setLogChange(logChange);
        return this;
    }

    public WorkhorseConfigBuilder logTimeFormat(String logTimeFormat) {
        workhorseConfig.setLogTimeFormat(logTimeFormat);
        return this;
    }

    public WorkhorseConfigBuilder logInfoMarker(String logInfoMarker) {
        workhorseConfig.setLogInfoMarker(logInfoMarker);
        return this;
    }

    public WorkhorseConfigBuilder logWarnMarker(String logWarnMarker) {
        workhorseConfig.setLogWarnMarker(logWarnMarker);
        return this;
    }

    public WorkhorseConfigBuilder logErrorMarker(String logErrorMarker) {
        workhorseConfig.setLogErrorMarker(logErrorMarker);
        return this;
    }

}
