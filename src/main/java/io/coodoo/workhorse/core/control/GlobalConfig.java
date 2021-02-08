package io.coodoo.workhorse.core.control;

import io.coodoo.workhorse.core.entity.WorkhorseConfig;
import io.coodoo.workhorse.persistence.interfaces.PersistenceTyp;

public class GlobalConfig {

    private WorkhorseConfig workhorseConfig;
    private PersistenceTyp persistenceTyp;
    private Object persistenceConfig;

    public GlobalConfig(WorkhorseConfig workhorseConfig, PersistenceTyp persistenceTyp, Object persistenceObject) {
        this.workhorseConfig = workhorseConfig;
        this.persistenceTyp = persistenceTyp;
        this.persistenceConfig = persistenceObject;
    }

    public WorkhorseConfig getWorkhorseConfig() {
        return workhorseConfig;
    }

    public PersistenceTyp getPersistenceTyp() {
        return persistenceTyp;
    }

    public Object getPersistenceConfig() {
        return persistenceConfig;
    }

}
