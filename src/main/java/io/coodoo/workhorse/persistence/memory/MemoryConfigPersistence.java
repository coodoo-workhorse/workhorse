package io.coodoo.workhorse.persistence.memory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.coodoo.workhorse.core.entity.WorkhorseConfig;
import io.coodoo.workhorse.persistence.interfaces.ConfigPersistence;

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
        return MemoryPersistence.NAME;
    }

    @Override
    public String getPersistenceVersion() {

        return "2.0.0-RC2-SNAPSHOT";
    }

}
