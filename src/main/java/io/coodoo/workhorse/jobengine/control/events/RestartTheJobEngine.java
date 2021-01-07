package io.coodoo.workhorse.jobengine.control.events;

import io.coodoo.workhorse.config.entity.JobEngineConfig;

public class RestartTheJobEngine {

    private Object persistenceParams;

    private JobEngineConfig jobEngine;

    public RestartTheJobEngine() {
    }
    
    public RestartTheJobEngine(Object persistenceParams, JobEngineConfig jobEngine) {
        this.persistenceParams = persistenceParams;
        this.jobEngine = jobEngine;
    }

    public Object getPersistenceParams() {
        return persistenceParams;
    }

    public void setPersistenceParams(Object persistenceParams) {
        this.persistenceParams = persistenceParams;
    }

    public JobEngineConfig getJobEngine() {
        return jobEngine;
    }

    public void setJobEngine(JobEngineConfig jobEngine) {
        this.jobEngine = jobEngine;
    }

    
    
}
