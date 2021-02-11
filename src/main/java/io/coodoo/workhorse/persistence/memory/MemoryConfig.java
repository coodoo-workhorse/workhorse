package io.coodoo.workhorse.persistence.memory;

import io.coodoo.workhorse.core.entity.WorkhorseConfig;

public class MemoryConfig extends WorkhorseConfig {

    private String persistenceName = "MEMORY";

    @Override
    public String getPersistenceName() {
        return persistenceName;
    }

    public void setPersistenceName(String persistenceName) {
        this.persistenceName = persistenceName;
    }
}
