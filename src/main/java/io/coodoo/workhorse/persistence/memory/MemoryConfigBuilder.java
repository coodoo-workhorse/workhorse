package io.coodoo.workhorse.persistence.memory;

import io.coodoo.workhorse.core.boundary.WorkhorseConfigBuilder;

/**
 * A class to build an object of type {@link MemoryConfig}
 * 
 * @author coodoo GmbH (coodoo.io)
 */
public class MemoryConfigBuilder extends WorkhorseConfigBuilder {

    private MemoryConfig memoryConfig = new MemoryConfig();

    public MemoryConfigBuilder() {
        this.workhorseConfig = memoryConfig;
    }

    @Override
    public MemoryConfig build() {

        return this.memoryConfig;
    }

}
