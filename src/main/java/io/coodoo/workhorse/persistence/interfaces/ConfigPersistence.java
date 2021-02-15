package io.coodoo.workhorse.persistence.interfaces;

import io.coodoo.workhorse.core.entity.AbstractWorkhorseConfig;

public interface ConfigPersistence {

    /**
     * Get the configuration of the Workhorse
     * 
     * @return WorkhorseConfig
     */
    AbstractWorkhorseConfig get();

    /**
     * Update the configuration of the Workhorse
     * 
     * @param workhorseConfig configuration of the Workhorse
     * @return new configuration of the Workhorse
     */
    AbstractWorkhorseConfig update(AbstractWorkhorseConfig workhorseConfig);

    /**
     * initialize the connection with the persistence
     */
    void connect(Object... params);

    /**
     * @return retrieve the name of the persistencesistence to initialize
     */
    String getPersistenceName();

}
