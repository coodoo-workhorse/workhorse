package io.coodoo.workhorse.persistence.memory;

import io.coodoo.workhorse.core.boundary.WorkhorseConfigBuilder;

public class MemoryConfigBuilder extends WorkhorseConfigBuilder {

    private MemoryConfig memoryConfig = new MemoryConfig();

    public MemoryConfigBuilder() {
        this.workhorseConfig = memoryConfig;
    }

    public MemoryConfigBuilder useUrl(String url) {
        this.memoryConfig.setUrl(url);
        return this;
    }

    @Override
    public MemoryConfig build() {

        return this.memoryConfig;
    }

}
