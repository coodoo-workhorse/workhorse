package io.coodoo.workhorse.persistence.interfaces;

import java.time.LocalDateTime;
import java.util.List;

import io.coodoo.workhorse.core.entity.Execution;
import io.coodoo.workhorse.core.entity.ExecutionFailStatus;
import io.coodoo.workhorse.core.entity.ExecutionLog;
import io.coodoo.workhorse.core.entity.ExecutionStatus;
import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.core.entity.JobExecutionStatusSummary;
import io.coodoo.workhorse.persistence.interfaces.listing.ListingParameters;
import io.coodoo.workhorse.persistence.interfaces.listing.ListingResult;

/**
 * @author coodoo GmbH (coodoo.io)
 */
public interface ExecutionPersistence {

    /**
     * Retrieves the job execution by given Id of a job and id of job execution
     * 
     * @param jobId Id of the job
     * @param executionId Id of the job execution
     * @return A job execution
     */
    Execution getById(Long jobId, Long executionId);

    /**
     * Retrieves a limited list of job executions of the given job
     * 
     * @param jobId Id of the job
     * @param limit limit of job execution to return
     * @return a list of <code>limit</code> job execution
     */
    List<Execution> getByJobId(Long jobId, Long limit);

    /**
     * Get the listing result of execution
     * 
     * @param jobId Id of the job
     * @param listingParameters defines the listing queue. It contains optional query parameters as described above
     * @return list of execution
     */
    ListingResult<Execution> getExecutionListing(Long jobId, ListingParameters listingParameters);

    /**
     * Retrieves the next queued executions and the planned executions that have to be process now with given JobId, order by priority and createdAt. Executions
     * with existent value <code>ChainedPreviousExecutionId</code> do not have to be retrieve.
     * 
     * @param jobId Id of the job
     * @return List of executions order by <code>priority</code> and <code>createdAt</code>
     */
    List<Execution> pollNextExecutions(Long jobId, int limit);

    /**
     * Create a new job execution
     * 
     * @param execution execution to create
     * @return new persisted execution
     */
    Execution persist(Execution execution);

    /**
     * Delete a job execution by <code>jobId</code> and <code>id</code> of the execution
     * 
     * @param jobId Id of the job
     * @param executionId Id of the job execution
     */
    void delete(Long jobId, Long executionId);

    /**
     * Update a job execution
     * 
     * @param execution New value of the job execution
     * 
     * @return the updated job execution
     */
    Execution update(Execution execution);

    /**
     * Update the status of an execution
     * 
     * @param jobId ID of the corresponding job
     * @param executionId ID of the execution
     * @param status New Status to set
     * @param failStatus Specific status of a failed execution
     * @return the updated execution
     */
    Execution updateStatus(Long jobId, Long executionId, ExecutionStatus status, ExecutionFailStatus failStatus);

    /**
     * Given the Id of the corresponent job <code>jobId</code> and the limit date <code>preDate</code>, delete all job executions where
     * <code>Execution.createdAt < preDate</code>
     * 
     * @param jobId
     * @param preDate
     * @return
     */
    int deleteOlderExecutions(Long jobId, LocalDateTime preDate);

    /**
     * Retrieve all job executions of a batchExecution
     * 
     * @param jobId Id of the corresponding job
     * @param batchId Id of the batchExecution
     * @return List of all job executions of the batchExecution
     */
    List<Execution> getBatch(Long jobId, Long batchId);

    /**
     * Retrieve all execution of a chainedExecution
     * 
     * @param jobId Id of the corresponding job
     * @param chainId Id of the chainedExecution
     * @return List of all job executions of the chainedExecution
     */
    List<Execution> getChain(Long jobId, Long chainId);

    /**
     * Check whether there is already an execution with these parameters and whether it has the status QUEUED. If so, return this.
     * 
     * @param jobId the jobId
     * @param parametersHash the parameterHash
     * @return List of Execution
     */
    Execution getFirstCreatedByJobIdAndParametersHash(Long jobId, Integer parameterHash);

    /**
     * Check if they are other QUEUED job execution of the given batchExecution
     * 
     * @param jobId Id of the corresponding job
     * @param batchId Id of the batchExecution
     * @return
     */
    boolean isBatchFinished(Long jobId, Long batchId);

    /**
     * Set the status of all queued Executions of the given chainedExecution to {@link ExecutionStatus#FAILED}
     * 
     * @param jobId id of the job
     * @param chainId id of the chain
     * @return <code>true</code> if successful and <code>false</code> otherwise
     */
    boolean abortChain(Long jobId, Long chainId);

    /**
     * Find all Executions, that have started sooner as the given time.
     * 
     * @param time
     * @return list of Executions to abort
     */
    List<Execution> findTimeoutExecutions(LocalDateTime time);

    /**
     * Get all jobs, whose executions are in the given status
     * 
     * @param status status of execution
     * @param since only executions that have been created after this timestamp have to be returned. If null it will be ignored.
     * @return list of job
     */
    List<JobExecutionStatusSummary> getJobExecutionStatusSummaries(ExecutionStatus status, LocalDateTime since);

    /**
     * Get a {@link ExecutionLog}
     * 
     * @param executionId ID of the corresponding {@link Execution}
     * @return {@link ExecutionLog}
     */
    ExecutionLog getLog(Long jobId, Long executionId);

    /**
     * Adds a message to the log as a new line.
     * 
     * @param jobId ID of the corresponding {@link Job}
     * @param executionId ID of corresponding {@link Execution}
     * @param log message to log.
     */
    void log(Long jobId, Long executionId, String log);

    /**
     * Add an error message and stacktrace to the log.
     * 
     * @param jobId Id of the corresponding {@link Job}
     * @param executionId ID of corresponding {@link Execution}
     * @param error error message
     * @param stacktrace stacktrace of the {@link Execution}
     */
    void log(Long jobId, Long executionId, String error, String stacktrace);

    /**
     * 
     * initialize the connection with the persistence
     */
    void connect(Object... params);

    /**
     * 
     * @return retrieve the name of the persistence to initialize
     */
    String getPersistenceName();

    /**
     * can the implemented persistence emit event by new created job execution
     * 
     * @return boolean
     */
    boolean isPusherAvailable();
}
