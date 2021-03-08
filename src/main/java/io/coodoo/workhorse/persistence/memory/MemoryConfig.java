package io.coodoo.workhorse.persistence.memory;

import io.coodoo.workhorse.core.entity.WorkhorseConfig;

/**
 * A class to access the {@link WorkhorseConfig} configurations defined by the Memory Persistence.
 * 
 * @author coodoo GmbH (coodoo.io)
 */
public class MemoryConfig extends WorkhorseConfig {

    private static final String NAME = "MEMORY";

    @Override
    public String getPersistenceName() {
        return NAME;
    }
}
