package io.coodoo.workhorse.persistence.interfaces;

import io.coodoo.workhorse.config.entity.JobEngineConfig;

public interface JobEngineConfigPersistence {

    /**
     * Get the configuration of the JobEngine
     * 
     * @return JobEngineConfig
     */
    JobEngineConfig get();

    /**
     * Update the configuration of the JobEngine
     * 
     * @param jobEngineConfig configuration of the JobEngine
     * @return new configuration of the JobEngine
     */
    JobEngineConfig update(JobEngineConfig jobEngineConfig);

    /**
     * initialize the connection with the persistence
     */
    void connect(Object... params);

    /**
     * @return retrieve the type of the persistence to initialize
     */
    PersistenceTyp getPersistenceTyp();

}
