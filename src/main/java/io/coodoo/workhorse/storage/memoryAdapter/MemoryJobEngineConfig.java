package io.coodoo.workhorse.storage.memoryAdapter;


import io.coodoo.workhorse.storage.persistenceInterface.PersistenceTyp;

import io.coodoo.workhorse.config.entity.JobEngineConfig;


import io.coodoo.workhorse.storage.persistenceInterface.JobEngineConfigPersistence;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class MemoryJobEngineConfig implements JobEngineConfigPersistence {

    @Inject
    Memory memoryService;

    @Override
    public JobEngineConfig get() {
        return memoryService.getJobEngineConfig();
    }

    @Override
    public JobEngineConfig update(JobEngineConfig jobEngineConfig) {
        memoryService.setJobEngineConfig(jobEngineConfig);
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
