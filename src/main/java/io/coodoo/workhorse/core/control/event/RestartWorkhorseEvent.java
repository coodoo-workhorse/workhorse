package io.coodoo.workhorse.core.control.event;

import io.coodoo.workhorse.config.entity.WorkhorseConfig;

/**
 * @author coodoo GmbH (coodoo.io)
 */
public class RestartWorkhorseEvent {

    private Object persistenceParams;

    private WorkhorseConfig workhorseConfig;

    public RestartWorkhorseEvent() {}

    public RestartWorkhorseEvent(Object persistenceParams, WorkhorseConfig jobEngine) {
        this.persistenceParams = persistenceParams;
        this.workhorseConfig = jobEngine;
    }

    public Object getPersistenceParams() {
        return persistenceParams;
    }

    public void setPersistenceParams(Object persistenceParams) {
        this.persistenceParams = persistenceParams;
    }

    public WorkhorseConfig getJobEngine() {
        return workhorseConfig;
    }

    public void setJobEngine(WorkhorseConfig jobEngine) {
        this.workhorseConfig = jobEngine;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("RestartWorkhorseEvent [persistenceParams=");
        builder.append(persistenceParams);
        builder.append(", workhorseConfig=");
        builder.append(workhorseConfig);
        builder.append("]");
        return builder.toString();
    }

}
