package io.coodoo.workhorse.persistence.memory;

import io.coodoo.workhorse.core.entity.ExecutionStatus;

public class MemoryConfigBuilder {

    private MemoryConfig memoryConfig = new MemoryConfig();

    /**
     * Set another time zone. It is used to create instances of LocalDateTime.
     * 
     * @param timeZone
     * @return MemoryConfigBuilder
     */
    public MemoryConfigBuilder timeZone(String timeZone) {
        memoryConfig.setTimeZone(timeZone);
        return this;
    }

    public MemoryConfigBuilder bufferMaximumSize(Long bufferMax) {
        memoryConfig.setBufferMax(bufferMax);
        return this;
    }

    /**
     * set the ...
     * 
     * @param bufferMin
     * @return
     */
    public MemoryConfigBuilder bufferMinimumSize(int bufferMin) {
        memoryConfig.setBufferMin(bufferMin);
        return this;
    }

    public MemoryConfigBuilder bufferPollInterval(int bufferPollInterval) {
        memoryConfig.setBufferPollInterval(bufferPollInterval);
        return this;
    }

    public MemoryConfigBuilder bufferPushFallbackPollInterval(int bufferPushFallbackPollInterval) {
        memoryConfig.setBufferPushFallbackPollInterval(bufferPushFallbackPollInterval);
        return this;
    }

    public MemoryConfigBuilder executionTimeout(int executionTimeout) {
        memoryConfig.setExecutionTimeout(executionTimeout);
        return this;
    }

    public MemoryConfigBuilder executionTimeoutStatus(ExecutionStatus executionTimeoutStatus) {
        memoryConfig.setExecutionTimeoutStatus(executionTimeoutStatus);
        return this;
    }

    public MemoryConfigBuilder logChange(String logChange) {
        memoryConfig.setLogChange(logChange);
        return this;
    }

    public MemoryConfigBuilder logTimeFormat(String logTimeFormat) {
        memoryConfig.setLogTimeFormat(logTimeFormat);
        return this;
    }

    public MemoryConfigBuilder logInfoMarker(String logInfoMarker) {
        memoryConfig.setLogInfoMarker(logInfoMarker);
        return this;
    }

    public MemoryConfigBuilder logWarnMarker(String logWarnMarker) {
        memoryConfig.setLogWarnMarker(logWarnMarker);
        return this;
    }

    public MemoryConfigBuilder logErrorMarker(String logErrorMarker) {
        memoryConfig.setLogErrorMarker(logErrorMarker);
        return this;
    }

    public MemoryConfig build() {
        return memoryConfig;
    }
}
