package io.coodoo.workhorse.persistence.memory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.coodoo.workhorse.core.entity.AbstractWorkhorseConfig;
import io.coodoo.workhorse.persistence.interfaces.ConfigPersistence;

@ApplicationScoped
public class MemoryConfigPersistence implements ConfigPersistence {

    @Inject
    MemoryPersistence memoryPersistence;

    @Override
    public AbstractWorkhorseConfig get() {
        return memoryPersistence.getWorkhorseConfig();
    }

    @Override
    public AbstractWorkhorseConfig update(AbstractWorkhorseConfig workhorseConfig) {
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

}
