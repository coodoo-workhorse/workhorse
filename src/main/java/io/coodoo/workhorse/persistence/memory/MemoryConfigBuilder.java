package io.coodoo.workhorse.persistence.memory;

import io.coodoo.workhorse.core.entity.ExecutionStatus;

public class MemoryConfigBuilder {

    private MemoryConfig memoryConfig = new MemoryConfig();

    public MemoryConfigBuilder setPersistenceName(String persistenceName) {
        memoryConfig.setPersistenceName(persistenceName);
        return this;
    }

    public MemoryConfigBuilder setTimeZone(String timeZone) {
        memoryConfig.setTimeZone(timeZone);
        return this;
    }

    public MemoryConfigBuilder setBufferMax(Long bufferMax) {
        memoryConfig.setBufferMax(bufferMax);
        return this;
    }

    public MemoryConfigBuilder setBufferMin(int bufferMin) {
        memoryConfig.setBufferMin(bufferMin);
        return this;
    }

    public MemoryConfigBuilder setBufferPollInterval(int bufferPollInterval) {
        memoryConfig.setBufferPollInterval(bufferPollInterval);
        return this;
    }

    public MemoryConfigBuilder setBufferPushFallbackPollInterval(int bufferPushFallbackPollInterval) {
        memoryConfig.setBufferPushFallbackPollInterval(bufferPushFallbackPollInterval);
        return this;
    }

    public MemoryConfigBuilder setExecutionTimeout(int executionTimeout) {
        memoryConfig.setExecutionTimeout(executionTimeout);
        return this;
    }

    public MemoryConfigBuilder setExecutionTimeoutStatus(ExecutionStatus executionTimeoutStatus) {
        memoryConfig.setExecutionTimeoutStatus(executionTimeoutStatus);
        return this;
    }

    public MemoryConfigBuilder setLogChange(String logChange) {
        memoryConfig.setLogChange(logChange);
        return this;
    }

    public MemoryConfigBuilder setLogTimeFormat(String logTimeFormat) {
        memoryConfig.setLogTimeFormat(logTimeFormat);
        return this;
    }

    public MemoryConfigBuilder setLogInfoMarker(String logInfoMarker) {
        memoryConfig.setLogInfoMarker(logInfoMarker);
        return this;
    }

    public MemoryConfigBuilder setLogWarnMarker(String logWarnMarker) {
        memoryConfig.setLogWarnMarker(logWarnMarker);
        return this;
    }

    public MemoryConfigBuilder setLogErrorMarker(String logErrorMarker) {
        memoryConfig.setLogErrorMarker(logErrorMarker);
        return this;
    }

    public MemoryConfig build() {
        return memoryConfig;
    }
}
