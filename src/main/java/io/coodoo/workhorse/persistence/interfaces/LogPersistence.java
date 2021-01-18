package io.coodoo.workhorse.persistence.interfaces;

import java.util.List;

import io.coodoo.workhorse.core.entity.WorkhorseLog;

public interface LogPersistence {

    /**
     * Get the log with given Id
     * @param id Id of the log to retrieve
     * @return JobEngineLog
     */
    WorkhorseLog get(Long id);

    /**
     * Update a log 
     * @param id Id of the log to retrieve
     * @param jobEngineLog log 
     * @return the updated log
     */
    WorkhorseLog update(Long id, WorkhorseLog jobEngineLog);

    /**
     * Delete a log
     * @param id Id of the log to delete
     */
    WorkhorseLog delete(Long id);

    /**
     * Create a new log 
     * @param jobEngineLog log to persit
     * @return new persisted JobEngineLog
     */
    WorkhorseLog persist(WorkhorseLog jobEngineLog);

    /**
     * Retrieves a limited number of logs
     * @param limit number of logs to get
     * @return List of logs
     */
    List<WorkhorseLog> getAll(int limit);

    /**
     * Delete all log of the given job
     * @param jobId Id of the job 
     * @return Number of log, that have been deleted
     */
    int deleteByJobId(Long jobId);

    /**
     * retrieve the type of the persistence to initialize
     * @return 
     */
    PersistenceTyp getPersistenceTyp();

    /**
     * Callback fonction to initialize the connection with the persistence
     */
    void connect(Object... params);

}
