package io.coodoo.workhorse.persistence.memory;

import io.coodoo.workhorse.core.entity.WorkhorseConfig;

/**
 * A class to access the {@link WorkhorseConfig} configurations defined by the Memory Persistence.
 * 
 * @author coodoo GmbH (coodoo.io)
 */
public class MemoryConfig extends WorkhorseConfig {

    public static final String NAME = "MEMORY";
    public static final String VERSION = "2.0.0-RC2-SNAPSHOT";

    @Override
    public String getPersistenceName() {
        return NAME;
    }

    @Override
    public String getPersistenceVersion() {
        return VERSION;
    }
}
