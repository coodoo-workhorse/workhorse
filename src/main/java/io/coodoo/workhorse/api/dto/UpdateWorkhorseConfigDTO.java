package io.coodoo.workhorse.api.dto;

import io.coodoo.workhorse.core.entity.ExecutionStatus;

public class UpdateWorkhorseConfigDTO {
    public String timeZone;
    public int bufferMax;
    public int bufferMin;
    public int bufferPollInterval; // in seconds
    public int bufferPushFallbackPollInterval; // in seconds
    public int minutesUntilCleanup;
    public int executionTimeout;
    public ExecutionStatus executionTimeoutStatus;
    public String logChange;
    public String logTimeFormat;
    public String logInfoMarker;
    public String logWarnMarker;
    public String logErrorMarker;

    public UpdateWorkhorseConfigDTO() {}
}
