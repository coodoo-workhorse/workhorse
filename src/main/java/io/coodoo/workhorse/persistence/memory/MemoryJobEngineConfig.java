package io.coodoo.workhorse.persistence.memory;


import io.coodoo.workhorse.config.entity.JobEngineConfig;
import io.coodoo.workhorse.persistence.interfaces.JobEngineConfigPersistence;
import io.coodoo.workhorse.persistence.interfaces.PersistenceTyp;

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
