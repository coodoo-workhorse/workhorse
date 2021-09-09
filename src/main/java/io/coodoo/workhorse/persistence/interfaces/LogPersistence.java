package io.coodoo.workhorse.persistence.interfaces;

import java.util.List;

import io.coodoo.workhorse.core.entity.WorkhorseLog;
import io.coodoo.workhorse.persistence.interfaces.listing.ListingParameters;
import io.coodoo.workhorse.persistence.interfaces.listing.ListingResult;

public interface LogPersistence {

    /**
     * retrieve the name of the persistence to initialize
     * 
     * @return
     */
    String getPersistenceName();

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
     * Get the listing result of logs
     * 
     * @param listingParameters defines the listing queue. It contains optional query parameters as described above
     * @return list of logs
     */
    ListingResult<WorkhorseLog> getWorkhorseLogListing(ListingParameters listingParameters);

    /**
     * Delete all log of the given job
     * 
     * @param jobId Id of the job
     * @return Number of log, that have been deleted
     */
    int deleteByJobId(Long jobId);

}
