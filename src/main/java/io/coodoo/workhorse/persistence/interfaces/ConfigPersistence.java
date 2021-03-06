package io.coodoo.workhorse.persistence.interfaces;

import io.coodoo.workhorse.core.entity.WorkhorseConfig;

public interface ConfigPersistence {

    /**
     * @return retrieve the name of the persistence to initialize
     */
    String getPersistenceName();

    /**
     * Get the version of the used Persistence
     * 
     * @return
     */
    String getPersistenceVersion();

    /**
     * initialize the connection with the persistence
     */
    void initialize(Object... params);

    /**
     * Get the configuration of the Workhorse
     * 
     * @return WorkhorseConfig
     */
    WorkhorseConfig get();

    /**
     * Update the configuration of the Workhorse
     * 
     * @param workhorseConfig configuration of the Workhorse
     * @return new configuration of the Workhorse
     */
    WorkhorseConfig update(WorkhorseConfig workhorseConfig);

}
