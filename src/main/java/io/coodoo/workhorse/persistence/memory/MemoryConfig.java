package io.coodoo.workhorse.persistence.memory;

import io.coodoo.workhorse.core.entity.WorkhorseConfig;

public class MemoryConfig extends WorkhorseConfig {

    private static final String NAME = "MEMORY";

    public MemoryConfig() {
        super();
        this.logInfoMarker = "[INFO]";
        this.executionTimeout = 10;
    }

    @Override
    public String getPersistenceName() {
        return NAME;
    }
}
