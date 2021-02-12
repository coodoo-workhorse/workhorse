package io.coodoo.workhorse.persistence.memory;

import io.coodoo.workhorse.core.entity.WorkhorseConfig;

public class MemoryConfig extends WorkhorseConfig {

    private static final String NAME = "MEMORY";

    protected String url = "";

    public MemoryConfig() {
        super();
        this.logInfoMarker = "[INFO]";
        this.executionTimeout = 10;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String getPersistenceName() {
        return NAME;
    }
}
