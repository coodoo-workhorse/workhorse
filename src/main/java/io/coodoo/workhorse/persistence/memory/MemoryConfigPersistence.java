package io.coodoo.workhorse.persistence.memory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.coodoo.workhorse.core.entity.WorkhorseConfig;
import io.coodoo.workhorse.persistence.interfaces.ConfigPersistence;
import io.coodoo.workhorse.util.WorkhorseUtil;

@ApplicationScoped
public class MemoryConfigPersistence implements ConfigPersistence {

    @Inject
    MemoryPersistence memoryPersistence;

    @Override
    public String getPersistenceName() {
        return MemoryConfig.NAME;
    }

    @Override
    public String getPersistenceVersion() {
        return WorkhorseUtil.getVersion();
    }

    @Override
    public void initialize(Object... params) {}

    @Override
    public WorkhorseConfig get() {
        return memoryPersistence.getWorkhorseConfig();
    }

    @Override
    public WorkhorseConfig update(WorkhorseConfig workhorseConfig) {
        memoryPersistence.setWorkhorseConfig(workhorseConfig);
        return workhorseConfig;
    }

}
