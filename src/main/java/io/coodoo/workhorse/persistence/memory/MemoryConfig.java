package io.coodoo.workhorse.persistence.memory;

import io.coodoo.workhorse.core.entity.WorkhorseConfig;
import io.coodoo.workhorse.util.WorkhorseUtil;

/**
 * A class to access the {@link WorkhorseConfig} configurations defined by the Memory Persistence.
 * 
 * @author coodoo GmbH (coodoo.io)
 */
public class MemoryConfig extends WorkhorseConfig {

    public static final String NAME = "MEMORY";

    @Override
    public String getPersistenceName() {
        return NAME;
    }

    @Override
    public String getPersistenceVersion() {
        return WorkhorseUtil.getVersion();
    }
}
