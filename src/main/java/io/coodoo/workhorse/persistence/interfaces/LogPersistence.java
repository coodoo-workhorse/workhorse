package io.coodoo.workhorse.persistence.interfaces;

import java.util.List;

import io.coodoo.workhorse.core.entity.WorkhorseLog;

public interface LogPersistence {

    /**
     * Get the log with given Id
     * 
     * @param logId Id of the log to retrieve
     * @return WorkhorseLog
     */
    WorkhorseLog get(Long logId);

    /**
     * Update a log
     * 
     * @param logId Id of the log to retrieve
     * @param workhorseLog log
     * @return the updated log
     */
    WorkhorseLog update(Long logId, WorkhorseLog workhorseLog);

    /**
     * Delete a log
     * 
     * @param logId Id of the log to delete
     */
    WorkhorseLog delete(Long logId);

    /**
     * Create a new log
     * 
     * @param workhorseLog log to persit
     * @return new persisted WorkhorseLog
     */
    WorkhorseLog persist(WorkhorseLog workhorseLog);

    /**
     * Retrieves a limited number of logs
     * 
     * @param limit number of logs to get
     * @return List of logs
     */
    List<WorkhorseLog> getAll(int limit);

    /**
     * Delete all log of the given job
     * 
     * @param jobId Id of the job
     * @return Number of log, that have been deleted
     */
    int deleteByJobId(Long jobId);

    /**
     * retrieve the name of the persistence to initialize
     * 
     * @return
     */
    String getPersistenceName();

    /**
     * Callback fonction to initialize the connection with the persistence
     */
    void connect(Object... params);

}
