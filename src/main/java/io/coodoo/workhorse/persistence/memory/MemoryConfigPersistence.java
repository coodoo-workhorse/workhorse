package io.coodoo.workhorse.persistence.memory;


import io.coodoo.workhorse.config.entity.WorkhorseConfig;
import io.coodoo.workhorse.persistence.interfaces.ConfigPersistence;
import io.coodoo.workhorse.persistence.interfaces.PersistenceTyp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class MemoryConfigPersistence implements ConfigPersistence {

    @Inject
    MemoryPersistence memoryService;

    @Override
    public WorkhorseConfig get() {
        return memoryService.getJobEngineConfig();
    }

    @Override
    public WorkhorseConfig update(WorkhorseConfig jobEngineConfig) {
        memoryService.setWorkhorseConfig(jobEngineConfig);
        return jobEngineConfig;
    }

    @Override
    public void connect(Object... params) {
        // TODO Auto-generated method stub
    }

    @Override
    public PersistenceTyp getPersistenceTyp() {
        return PersistenceTyp.MEMORY;
    }

    
}
