package io.coodoo.workhorse.persistence.interfaces;

import java.time.LocalDateTime;
import java.util.List;

import io.coodoo.workhorse.core.entity.Execution;
import io.coodoo.workhorse.core.entity.ExecutionStatus;

public interface ExecutionPersistence {

    /**
     * Retrieves the job execution by given Id of a job and id of job execution
     * 
     * @param jobId Id of the job
     * @param id    Id of the job execution
     * @return A job execution
     */
    Execution getById(Long jobId, Long id);

    /**
     * Retrieves a limited list of job executions of the given job
     * 
     * @param jobId Id of the job
     * @param limit limit of job execution to return
     * @return a list of <code>limit</code> job execution
     */
    List<Execution> getByJobId(Long jobId, Long limit);

    /**
     * Retrieves the next queued executions with given JobId, order by priority and
     * createdAt. Executions with existent value
     * <code>ChainedPreviousExecutionId</code> do not have to be retrieve.
     * 
     * @param jobId Id of the job
     * @return List of executions order by <code>priority</code> and
     *         <code>createdAt</code>
     */
    List<Execution> pollNextExecutions(Long jobId, Long limit);

    /**
     * Count the number of job executions
     * 
     * @return
     */
    Long count();

    /**
     * Create a new job execution
     * 
     * @param execution execution to create
     */
    void persist(Execution execution);

    /**
     * Delete a job execution by <code>jobId</code> and <code>id</code> of the
     * execution
     * 
     * @param jobId Id of the job
     * @param id    Id of the job execution
     */
    void delete(Long jobId, Long id);

    /**
     * Update a job execution
     * 
     * @param jobId     Id of the correspondent job
     * @param id        Id of the job execution to update
     * @param execution New value of the job execution
     * @return the updated job execution
     */
    Execution update(Long jobId, Long id, Execution execution);

    /**
     * Given the Id of the corresponent job <code>jobId</code> and the limit date
     * <code>preDate</code>, delete all job executions where
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
     * @param jobId   Id of the correspondent job
     * @param batchId Id of the batchExecution
     * @return List of all job executions of the batchExecution
     */
    List<Execution> getBatch(Long jobId, Long batchId);

    /**
     * Retrieve all job execution of a chainedExecution
     * 
     * @param jobId   Id of the correspondent job
     * @param chainId Id of the chainedExecution
     * @return List of all job executions of the chainedExecution
     */
    List<Execution> getChain(Long jobId, Long chainId);

    /**
     * Get the first found job execution of the Batch.
     * 
     * @param jobId   Id of the correspondent job
     * @param BatchId Id of the batchExecution
     * @return the found job executon
     */
    Execution getQueuedBatchExecution(Long jobId, Long batchId);

    /**
     * Get all Failed job executions of a batchExecution
     * 
     * @param jobId   Id of the correspondent job
     * @param BatchId Id of the batchExecution
     * @return
     */
    List<Execution> getFailedBatchExecutions(Long jobId, Long batchId);

    /**
     * Optional. Set the Id the given job execution <code>execution</code> at end of
     * the chained Execution with Id <code>chainId</code> .
     * 
     * @param jobId     Id of the correspondent job
     * @param chainId   Id of the chainedExecution
     * @param execution Job execution to set at end of the chain.
     * @return Last job execution of the chain.
     */
    Execution addExecutionAtEndOfChain(Long jobId, Long chainId, Execution execution);

    /**
     * @param jobId     Id of the correspondent job
     * @param chainId   Id of the chainedExecution
     * @param execution Job execution whose next execution have to be found.
     * @return Next job execution of the chain
     */
    Execution getNextQueuedExecutionInChain(Long jobId, Long chainId, Execution execution);

    /**
     * Check whether there is already an execution with these parameters and whether
     * it has the status QUEUED. If so, return this.
     * 
     * @param jobId          the jobId
     * @param parametersHash the parameterHash
     * @return List of Execution
     */
    Execution getFirstCreatedByJobIdAndParametersHash(Long jobId, Integer parameterHash);

    /**
     * Check if they are other QUEUED job execution of the given batchExecution
     * 
     * @param jobId   Id of the correspondent job
     * @param batchId Id of the batchExecution
     * @return
     */
    boolean isBatchFinished(Long jobId, Long batchId);

    /**
     * Set the status of all queued Executions of the given chainedExecution to
     * {@link ExecutionStatus#FAILED}
     * 
     * @param jobId   id of the job
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
