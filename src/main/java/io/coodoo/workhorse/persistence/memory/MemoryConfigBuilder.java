package io.coodoo.workhorse.persistence.memory;

import io.coodoo.workhorse.core.entity.WorkhorseConfigBuilder;

/**
 * A class to build an object of type {@link MemoryConfig}
 * 
 * @author coodoo GmbH (coodoo.io)
 */
public class MemoryConfigBuilder extends WorkhorseConfigBuilder {

    private MemoryConfig memoryConfig = new MemoryConfig();

    public MemoryConfigBuilder() {

        // let the executions last 2 hours per default...
        memoryConfig.setMinutesUntilCleanup(120);

        this.workhorseConfig = memoryConfig;
    }

    @Override
    public MemoryConfig build() {

        return this.memoryConfig;
    }

}
