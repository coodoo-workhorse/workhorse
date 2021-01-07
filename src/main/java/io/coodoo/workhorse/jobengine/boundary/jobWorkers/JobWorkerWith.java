package io.coodoo.workhorse.jobengine.boundary.jobWorkers;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import io.coodoo.workhorse.jobengine.control.BaseJobWorker;
import io.coodoo.workhorse.jobengine.entity.JobExecution;
import io.coodoo.workhorse.util.JobEngineUtil;



public abstract class JobWorkerWith<T> extends BaseJobWorker {

    private Class<?> parametersClass;

    public abstract void doWork(T parameters) throws Exception;

    @Override
    public void doWork(JobExecution jobExecution) throws Exception {
        init(jobExecution);

        doWork(getParameters(jobExecution));
    }

    public T getParameters(JobExecution jobExecution) {
        getParametersClass();
        return (T)  JobEngineUtil.jsonToParameters(jobExecution.getParameters(), parametersClass);
    }

    protected Class<?> getParametersClass() {

        if (parametersClass != null) {
            return parametersClass;
        }
        Type type = getParameterWorkerClassType();
        parametersClass = (Class) type;
    
        return parametersClass;
    }

    public String getParametersClassName() {
        return getParameterWorkerClassType().getTypeName();
    }

    private Type getParameterWorkerClassType() {
        return ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

     /**
     * <i>Convenience method to create a job execution</i><br>
     * <br>
     * This creates a {@link JobExecution} object that gets added to the job engine with default options.
     * 
     * @param parameters needed parameters to do the job
     * @return job execution ID
     */
    public Long createJobExecution(T parameters) {
        return createJobExecution(parameters, false, null);
    }

    /**
     * <i>This is an access point to get the job engine started with a new job with job parameters.</i><br>
     * <br>
     * 
     * This creates a {@link JobExecution} object that gets added to the job engine to be executed as soon as possible.
     * 
     * @param parameters needed parameters to do the job
     * @param priority priority queuing
     * @param maturity specified time for the execution
     * @return job execution ID
     */
    public Long createJobExecution(T parameters, Boolean priority, LocalDateTime maturity) {
        return createJobExecution(parameters, priority, maturity, null, null, null).getId();
    }

    /**
     * <i>This is an access point to get the job engine started with a new job with job parameters.</i><br>
     * <br>
     * 
     * This creates a {@link JobExecution} object that gets added to the job engine to be executed as soon as possible.
     * 
     * @param parameters needed parameters to do the job
     * @param priority priority queuing
     * @param delayValue time to wait
     * @param delayUnit what kind of time to wait
     * @return job execution ID
     */
    public Long createJobExecution(T parameters, Boolean priority, Long delayValue, ChronoUnit delayUnit) {
        return createJobExecution(parameters, priority, delayToMaturity(delayValue, delayUnit), null, null, null).getId();
    }

     /**
     * <i>Convenience method to create a job execution</i><br>
     * <br>
     * This creates a {@link JobExecution} object that gets added to the priority queue of the job engine to be treated first class.
     * 
     * @param parameters needed parameters to do the job
     * @return job execution ID
     */
    public Long createPriorityJobExecution(T parameters) {
        return createJobExecution(parameters, true, null);
    }

    /**
     * <i>Convenience method to create a job execution</i><br>
     * <br>
     * This creates a {@link JobExecution} object that gets added to the job engine after the given delay.
     * 
     * @param parameters needed parameters to do the job
     * @param delayValue time to wait
     * @param delayUnit what kind of time to wait
     * @return job execution ID
     */
    public Long createDelayedJobExecution(T parameters, Long delayValue, ChronoUnit delayUnit) {
        return createJobExecution(parameters, false, delayValue, delayUnit);
    }

    /**
     * <i>Convenience method to create a job execution</i><br>
     * <br>
     * This creates a {@link JobExecution} object that gets added to the job engine at a specified time.
     * 
     * @param parameters needed parameters to do the job
     * @param maturity specified time for the execution
     * @return job execution ID
     */
    public Long createPlannedJobExecution(T parameters, LocalDateTime maturity) {
        return createJobExecution(parameters, false, maturity);
    }

     /**
     * This creates a batch of {@link JobExecution} objects
     * 
     * @param parametersList list of needed parameters to do the batch
     * @return batch ID
     */
    public Long createBatchJobExecutions(List<T> parametersList) {
        return createBatchJobExecutions(parametersList, false, null);
    }

    /**
     * This creates a batch of {@link JobExecution} objects
     * 
     * @param parametersList list of needed parameters to do the batch
     * @param priority priority queuing
     * @param maturity specified time for the execution
     * @return batch ID
     */
    public Long createBatchJobExecutions(List<T> parametersList, Boolean priority, LocalDateTime maturity) {

        Long batchId = null;

        for (T parameters : parametersList) {
            if (batchId == null) { // start of batch

                JobExecution jobExecution = createJobExecution(parameters, priority, maturity,  -1L, null, null);
                // Use the Id of the first added job execution in Batch as BatchId.
                jobExecution.setBatchId(jobExecution.getId());
                jobEngineController.updateJobExecution(jobExecution.getJobId(), jobExecution.getId(),  jobExecution);

                batchId = jobExecution.getId();
            } else { // now that we have the batch id, all the beloning executions can have it!
                createJobExecution(parameters, priority, maturity, batchId, null, null);
            }
        }
        return batchId;
    }

    /**
     * This creates a chain of {@link JobExecution} objects, so when the first one gets executed it will bring all its chained friends.
     * 
     * @param parametersList list of needed parameters to do the job in the order of the execution chain
     * @return chain ID
     */
    public Long createChainedJobExecutions(List<T> parametersList) {
        return createChainedJobExecutions(parametersList, false, null);
    }

    /**
     * This creates a chain of {@link JobExecution} objects, so when the first one gets executed it will bring all its chained friends.
     * 
     * @param parametersList list of needed parameters to do the job in the order of the execution chain
     * @param priority priority queuing
     * @param maturity specified time for the execution
     * @return chain ID
     */
    public Long createChainedJobExecutions(List<T> parametersList, Boolean priority, LocalDateTime maturity) {

        Long chainId = null;
        Long jobId = getJob().getId();
        Long chainedPreviousExecutionId = null;

        for (T parameters : parametersList) {
            if (chainId == null) { // start of chain
           
                JobExecution jobExecution = createJobExecution(parameters, priority, maturity,  null, -1L, null);
                jobExecution.setChainId(jobExecution.getId());
                jobEngineController.updateJobExecution(jobId, jobExecution.getId(),  jobExecution);

                chainId = jobExecution.getId();
                chainedPreviousExecutionId = jobExecution.getId();
                continue;
            }
            JobExecution jobExecution = createJobExecution(parameters, priority, maturity, null, chainId, chainedPreviousExecutionId);
            chainedPreviousExecutionId = jobExecution.getId();

            jobEngineController.addJobExecutionAtEndOfChain(jobId, chainId, jobExecution);
            

        }
        return chainId;
    }

}