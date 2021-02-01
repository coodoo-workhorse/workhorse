package io.coodoo.workhorse.core.control.event;

import io.coodoo.workhorse.config.entity.WorkhorseConfig;

/**
 * @author coodoo GmbH (coodoo.io)
 */
public class RestartWorkhorseEvent {

    private Object persistenceParams;

    private WorkhorseConfig workhorseConfig;

    public RestartWorkhorseEvent() {}

    public RestartWorkhorseEvent(Object persistenceParams, WorkhorseConfig workhorseConfig) {
        super();
        this.persistenceParams = persistenceParams;
        this.workhorseConfig = workhorseConfig;
    }

    public Object getPersistenceParams() {
        return persistenceParams;
    }

    public void setPersistenceParams(Object persistenceParams) {
        this.persistenceParams = persistenceParams;
    }

    public WorkhorseConfig getWorkhorseConfig() {
        return workhorseConfig;
    }

    public void setWorkhorseConfig(WorkhorseConfig workhorseConfig) {
        this.workhorseConfig = workhorseConfig;
    }

}
