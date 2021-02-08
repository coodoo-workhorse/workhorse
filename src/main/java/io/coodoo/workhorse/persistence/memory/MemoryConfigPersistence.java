package io.coodoo.workhorse.persistence.memory;

import io.coodoo.workhorse.core.entity.WorkhorseConfig;
import io.coodoo.workhorse.persistence.interfaces.ConfigPersistence;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class MemoryConfigPersistence implements ConfigPersistence {

    @Inject
    MemoryPersistence memoryPersistence;

    @Override
    public WorkhorseConfig get() {
        return memoryPersistence.getWorkhorseConfig();
    }

    @Override
    public WorkhorseConfig update(WorkhorseConfig workhorseConfig) {
        memoryPersistence.setWorkhorseConfig(workhorseConfig);
        return workhorseConfig;
    }

    @Override
    public void connect(Object... params) {
        // TODO Auto-generated method stub
    }

    @Override
    public String getPersistenceName() {
        return "MEMORY";
    }

}
