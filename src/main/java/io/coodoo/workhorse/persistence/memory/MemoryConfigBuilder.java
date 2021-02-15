package io.coodoo.workhorse.persistence.memory;

import io.coodoo.workhorse.core.boundary.WorkhorseService;
import io.coodoo.workhorse.core.entity.AbstractWorkhorseConfig;
import io.coodoo.workhorse.core.entity.AbstractWorkhorseConfigBuilder;

/**
 * A class to build an object of type {@link MemoryConfig}
 * 
 * @author coodoo GmbH (coodoo.io)
 */
public class MemoryConfigBuilder extends AbstractWorkhorseConfigBuilder {

    private MemoryConfig memoryConfig = new MemoryConfig();

    public MemoryConfigBuilder() {
        this.workhorseConfig = memoryConfig;
    }

    @Override
    public MemoryConfig build() {

        return this.memoryConfig;
    }

    public static void main(String[] args) {

        WorkhorseService workhorseService = new WorkhorseService();

        AbstractWorkhorseConfig c = new MemoryConfigBuilder().bufferMinimumSize(34).build();

        workhorseService.start(c);

    }

}
