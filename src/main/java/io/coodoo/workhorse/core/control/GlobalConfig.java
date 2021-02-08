package io.coodoo.workhorse.core.control;

import io.coodoo.workhorse.core.entity.WorkhorseConfig;

public class GlobalConfig {

    private WorkhorseConfig workhorseConfig;
    private String persistenceTyp;
    private Object persistenceConfig;

    public GlobalConfig(WorkhorseConfig workhorseConfig, String persistenceTyp, Object persistenceObject) {
        this.workhorseConfig = workhorseConfig;
        this.persistenceTyp = persistenceTyp;
        this.persistenceConfig = persistenceObject;
    }

    public WorkhorseConfig getWorkhorseConfig() {
        return workhorseConfig;
    }

    public String getPersistenceTyp() {
        return persistenceTyp;
    }

    public Object getPersistenceConfig() {
        return persistenceConfig;
    }

}
