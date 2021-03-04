package io.coodoo.workhorse.core.boundary;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import io.coodoo.workhorse.core.control.BaseWorker;
import io.coodoo.workhorse.core.entity.Execution;
import io.coodoo.workhorse.util.WorkhorseUtil;

/**
 * worker class to define the creation and processing of execution with parameters. <br>
 * <tt>T</tt> can be any Object or a {@link List} of {@link String} of {@link Integer} <br>
 * Your job does not need parameters? See {@link Worker}!
 * 
 * @author coodoo GmbH (coodoo.io)
 */
public abstract class WorkerWith<T> extends BaseWorker {

    private Class<?> parametersClass;

    public abstract void doWork(T parameters) throws Exception;

    @Override
    public void doWork(Execution execution) throws Exception {

        this.executionContext.init(execution);

        doWork(getParameters(execution));
    }

    @SuppressWarnings("unchecked")
    public T getParameters(Execution execution) {
        getParametersClass();
        return (T) WorkhorseUtil.jsonToParameters(execution.getParameters(), parametersClass);
    }

    protected Class<?> getParametersClass() {

        if (parametersClass != null) {
            return parametersClass;
        }

        Type type = getParameterWorkerClassType();
        parametersClass = type2Class(type);

        return parametersClass;
    }

    // TODO Test complete the method
    protected Class<?> type2Class(Type type) { // https://stackoverflow.com/a/48066001/4034100
        if (type instanceof Class) {
            return (Class<?>) type;
        } else if (type instanceof GenericArrayType) {
            // having to create an array instance to get the class is kinda nasty
            // but apparently this is a current limitation of java-reflection concerning
            // array classes.
            // E.g. T[] -> T -> Object.class if <T> or Number.class if <T extends Number &
            // Comparable>
            return Array.newInstance(type2Class(((GenericArrayType) type).getGenericComponentType()), 0).getClass();
        } else if (type instanceof ParameterizedType) {
            return type2Class(((ParameterizedType) type).getRawType()); // Eg. List<T> would return List.class
        } else if (type instanceof TypeVariable) {
            Type[] bounds = ((TypeVariable<?>) type).getBounds();
            return bounds.length == 0 ? Object.class : type2Class(bounds[0]); // erasure is to the left-most bound.
        } else if (type instanceof WildcardType) {
            Type[] bounds = ((WildcardType) type).getUpperBounds();
            return bounds.length == 0 ? Object.class : type2Class(bounds[0]); // erasure is to the left-most upper
                                                                              // bound.
        } else {
            throw new UnsupportedOperationException("cannot handle type class: " + type.getClass());
        }
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
     * This creates a {@link Execution} object that gets added to the job engine with default options.
     * 
     * @param parameters needed parameters to do the job
     * @return job execution ID
     */
    public Long createExecution(T parameters) {
        return createExecution(parameters, false, null);
    }

    /**
     * <i>This is an access point to get the job engine started with a new job with job parameters.</i><br>
     * <br>
     * 
     * This creates a {@link Execution} object that gets added to the job engine to be executed as soon as possible.
     * 
     * @param parameters needed parameters to do the job
     * @param priority   priority queuing
     * @param plannedFor specified time for the execution
     * @return job execution ID
     */
    public Long createExecution(T parameters, Boolean priority, LocalDateTime plannedFor) {
        return createExecution(parameters, priority, plannedFor, null, null, null, null).getId();
    }

    /**
     * <i>This is an access point to get the job engine started with a new job with job parameters.</i><br>
     * <br>
     * 
     * This creates a {@link Execution} object that gets added to the job engine to be executed as soon as possible.
     * 
     * @param parameters needed parameters to do the job
     * @param priority   priority queuing
     * @param delayValue time to wait
     * @param delayUnit  what kind of time to wait
     * @return job execution ID
     */
    public Long createExecution(T parameters, Boolean priority, Long delayValue, ChronoUnit delayUnit) {
        return createExecution(parameters, priority, delayToMaturity(delayValue, delayUnit), null, null, null, null).getId();
    }

    /**
     * <i>Convenience method to create a job execution</i><br>
     * <br>
     * This creates a {@link Execution} object that gets added to the priority queue of the job engine to be treated first class.
     * 
     * @param parameters needed parameters to do the job
     * @return job execution ID
     */
    public Long createPriorityExecution(T parameters) {
        return createExecution(parameters, true, null);
    }

    /**
     * <i>Convenience method to create a job execution</i><br>
     * <br>
     * This creates a {@link Execution} object that gets added to the job engine after the given delay.
     * 
     * @param parameters needed parameters to do the job
     * @param delayValue time to wait
     * @param delayUnit  what kind of time to wait
     * @return job execution ID
     */
    public Long createDelayedExecution(T parameters, Long delayValue, ChronoUnit delayUnit) {
        return createExecution(parameters, false, delayValue, delayUnit);
    }

    /**
     * <i>Convenience method to create a job execution</i><br>
     * <br>
     * This creates a {@link Execution} object that gets added to the job engine at a specified time.
     * 
     * @param parameters needed parameters to do the job
     * @param plannedFor specified time for the execution
     * @return job execution ID
     */
    public Long createPlannedExecution(T parameters, LocalDateTime plannedFor) {
        return createExecution(parameters, false, plannedFor);
    }

    /**
     * This creates a batch of {@link Execution} objects
     * 
     * @param parametersList list of needed parameters to do the batch
     * @return batch ID
     */
    public Long createBatchExecutions(List<T> parametersList) {
        return createBatchExecutions(parametersList, false, null);
    }

    /**
     * This creates a batch of {@link Execution} objects
     * 
     * @param parametersList list of needed parameters to do the batch
     * @param priority       priority queuing
     * @param plannedFor     specified time for the execution
     * @return batch ID
     */
    public Long createBatchExecutions(List<T> parametersList, Boolean priority, LocalDateTime plannedFor) {

        Long batchId = null;

        for (T parameters : parametersList) {
            if (batchId == null) { // start of batch

                Execution execution = createExecution(parameters, priority, plannedFor, null, -1L, null, null);
                // Use the Id of the first added job execution in Batch as BatchId.
                execution.setBatchId(execution.getId());
                workhorseController.updateExecution(execution);

                batchId = execution.getId();
            } else { // now that we have the batch id, all the beloning executions can have it!
                createExecution(parameters, priority, plannedFor, null, batchId, null, null);
            }
        }
        return batchId;
    }

    /**
     * This creates a chain of {@link Execution} objects, so when the first one gets executed it will bring all its chained friends.
     * 
     * @param parametersList list of needed parameters to do the job in the order of the execution chain
     * @return chain ID
     */
    public Long createChainedExecutions(List<T> parametersList) {
        return createChainedExecutions(parametersList, false, null);
    }

    /**
     * This creates a chain of {@link Execution} objects, so when the first one gets executed it will bring all its chained friends.
     * 
     * @param parametersList list of needed parameters to do the job in the order of the execution chain
     * @param priority       priority queuing
     * @param plannedFor     specified time for the execution
     * @return chain ID
     */
    public Long createChainedExecutions(List<T> parametersList, Boolean priority, LocalDateTime plannedFor) {

        Long chainId = null;
        Long jobId = getJob().getId();
        Long chainedPreviousExecutionId = null;

        for (T parameters : parametersList) {
            if (chainId == null) { // start of chain

                Execution execution = createExecution(parameters, priority, plannedFor, null, null, -1L, null);
                execution.setChainId(execution.getId());
                workhorseController.updateExecution(execution);

                chainId = execution.getId();
                chainedPreviousExecutionId = execution.getId();
                continue;
            }
            Execution execution = createExecution(parameters, priority, plannedFor, null, null, chainId, chainedPreviousExecutionId);
            chainedPreviousExecutionId = execution.getId();

            workhorseController.addExecutionAtEndOfChain(jobId, chainId, execution);

        }
        return chainId;
    }

}
