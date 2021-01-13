package io.coodoo.workhorse.persistence.interfaces;

import java.time.LocalDateTime;
import java.util.List;

import io.coodoo.workhorse.jobengine.entity.JobExecution;

public interface JobExecutionPersistence {

        /**
         * Retrieves the job execution by given Id of a job and id of job execution
         * 
         * @param jobId Id of the job
         * @param id    Id of the job execution
         * @return A job execution
         */
        JobExecution getById(Long jobId, Long id);

        /**
         * Retrieves a limited list of job executions of the given job
         * 
         * @param jobId Id of the job
         * @param limit limit of job execution to return
         * @return a list of <code>limit</code> job execution
         */
        List<JobExecution> getByJobId(Long jobId, Long limit);

        /**
         * Retrieves the next queued job executions with given JobId, order by priority
         * and createdAt. 
         * Job executions with existent value <code>ChainedPreviousExecutionId</code> do not have to be retrieve.
         * 
         * @param jobId Id of th job
         * @return List of job execution order by job executions parameters
         *         <code>priority</code> and <code>createdAt</code>
         */
        List<JobExecution> pollNextJobExecutions(Long jobId, Long limit);

        /**
         * Count the number of job executions
         * 
         * @return
         */
        Long count();

        /**
         * Create a new job execution
         * 
         * @param jobExecution execution to create
         */
        void persist(JobExecution jobExecution);

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
         * @param jobId        Id of the correspondent job
         * @param id           Id of the job execution to update
         * @param jobExecution New value of the job execution
         * @return the updated job execution
         */
        JobExecution update(Long jobId, Long id, JobExecution jobExecution);

        /**
         * Given the Id of the corresponent job <code>jobId</code> and the limit date  <code>preDate</code>,
         * delete all job executions where <code>JobExecution.createdAt < preDate</code>
         * @param jobId
         * @param preDate
         * @return
         */
        int deleteOlderJobExecutions(Long jobId, LocalDateTime preDate);

        /**
         * Retrieve all job executions of a batchJobExecution
         * 
         * @param jobId   Id of the correspondent job
         * @param batchId Id of the batchJobExecution
         * @return List of all job executions of the batchJobExecution
         */
        List<JobExecution> getBatch(Long jobId, Long batchId);

        /**
         * Retrieve all job execution of a chainedJobExecution
         * 
         * @param jobId   Id of the correspondent job
         * @param chainId Id of the chainedJobExecution
         * @return List of all job executions of the chainedJobExecution
         */
        List<JobExecution> getChain(Long jobId, Long chainId);

        /**
         * Get the first found job execution of the Batch.
         * 
         * @param jobId   Id of the correspondent job
         * @param BatchId Id of the batchJobExecution
         * @return the found job executon
         */
        JobExecution getQueuedBatchJobExecution(Long jobId, Long batchId);

        /**
         * Get all Failed job executions of a batchJobExecution
         * 
         * @param jobId   Id of the correspondent job
         * @param BatchId Id of the batchJobExecution
         * @return
         */
        List<JobExecution> getFailedBatchJobExecutions(Long jobId, Long batchId);

        /**
         * Optional. Set the Id the given job execution <code>jobExecution</code> at end
         * of the chained JobExecution with Id <code>chainId</code> .
         * 
         * @param jobId        Id of the correspondent job
         * @param chainId      Id of the chainedJobExecution
         * @param jobExecution Job execution to set at end of the chain.
         * @return Last job execution of the chain.
         */
        JobExecution addJobExecutionAtEndOfChain(Long jobId, Long chainId, JobExecution jobExecution);

        /**
         * @param jobId        Id of the correspondent job
         * @param chainId      Id of the chainedJobExecution
         * @param jobExecution Job execution whose next execution have to be found.
         * @return Next job execution of the chain
         */
        JobExecution getNextQueuedJobExecutionInChain(Long jobId, Long chainId, JobExecution jobExecution);

        /**
         * Check if they are other QUEUED job execution of the given batchJobExecution
         * 
         * @param jobId   Id of the correspondent job
         * @param batchId Id of the batchJobExecution
         * @return
         */
        boolean isBatchFinished(Long jobId, Long batchId);

        /**
         * Abort all chained Job Execution of the given chainedJobExecution
         * 
         * @param jobId
         * @param chainId
         * @return <code>true</code> if successful and <code>false</code> otherwise
         */
        boolean abortChain(Long jobId, Long chainId);

        /**
         * 
         * initialize the connection with the persistence
         */
        void connect(Object... params);

        /**
         * 
         * @return retrieve the type of the persistence to initialize
         */
        PersistenceTyp getPersistenceTyp();

        /**
         * can the implemented persistence emit event by new created job execution
         * 
         * @return boolean
         */
        boolean isPusherAvailable();
}
