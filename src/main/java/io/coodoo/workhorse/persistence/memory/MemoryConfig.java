package io.coodoo.workhorse.persistence.memory;

import io.coodoo.workhorse.core.entity.AbstractWorkhorseConfig;

/**
 * A class to access the {@link AbstractWorkhorseConfig} configurations defined by the Memory Persistence.
 * 
 * @author coodoo GmbH (coodoo.io)
 */
public class MemoryConfig extends AbstractWorkhorseConfig {

    private static final String NAME = "MEMORY";

    @Override
    public String getPersistenceName() {
        return NAME;
    }
}
