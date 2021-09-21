package io.coodoo.workhorse.persistence.memory;

import io.coodoo.workhorse.core.entity.ExecutionStatus;
import io.coodoo.workhorse.core.entity.WorkhorseConfig;
import io.coodoo.workhorse.util.WorkhorseUtil;

/**
 * A class to access the {@link WorkhorseConfig} configurations defined by the Memory Persistence.
 * 
 * @author coodoo GmbH (coodoo.io)
 */
public class MemoryConfig extends WorkhorseConfig {

    public static final String NAME = "Memory Persistence";

    @Override
    public String getPersistenceName() {
        return NAME;
    }

    @Override
    public String getPersistenceVersion() {
        return WorkhorseUtil.getVersion();
    }

    public MemoryConfig() {}

    public MemoryConfig(String timeZone, int bufferMax, int bufferMin, int bufferPollInterval, int bufferPushFallbackPollInterval, long minutesUntilCleanup,
                    int executionTimeout, ExecutionStatus executionTimeoutStatus, String logChange, String logTimeFormat, String logInfoMarker,
                    String logWarnMarker, String logErrorMarker) {

        this.timeZone = timeZone;
        this.bufferMax = bufferMax;
        this.bufferMin = bufferMin;
        this.bufferPollInterval = bufferPollInterval;
        this.bufferPushFallbackPollInterval = bufferPushFallbackPollInterval;
        this.minutesUntilCleanup = minutesUntilCleanup;
        this.executionTimeout = executionTimeout;
        this.executionTimeoutStatus = executionTimeoutStatus;
        this.logChange = logChange;
        this.logTimeFormat = logTimeFormat;
        this.logInfoMarker = logInfoMarker;
        this.logWarnMarker = logWarnMarker;
        this.logErrorMarker = logErrorMarker;
    }
}
