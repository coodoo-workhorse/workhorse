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

    /**
     * Process the execution
     * 
     * @param parameters the object parameter needed for the execution
     * @return a message to summarize the execution
     * @throws Exception
     */
    public abstract String doWork(T parameters) throws Exception;

    @Override
    public String doWork(Execution execution) throws Exception {

        this.executionContext.init(execution);

        return doWork(getParameters(execution));
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

        Class<?> workerClass;
        try {

            // In Quarkus 2.0 the instance of a worker is a pseudo class with suffix like myClass_SubClass.java.
            // To get the real class of our worker we have to use the real worker name as stored in the persistence.
            // With the real class of the worker we can access the parameter class <T> of a WorkerWith<T> worker.
            String workerClassName = getClassName();
            workerClass = Class.forName(workerClassName, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            workerClass = getClass();
        }
        return ((ParameterizedType) workerClass.getGenericSuperclass()).getActualTypeArguments()[0];
    }

    /**
     * <i>Convenience method to create an execution</i><br>
     * <br>
     * This creates a {@link Execution} object that gets added to the job engine with default options.
     * 
     * @param parameters needed parameters to do the job
     * @return job execution ID
     */
    public Long createExecution(T parameters) {
        return executionBuilder().build(parameters);
    }

    /**
     * This creates a batch to group a list of {@link Execution} to have them as a batch.
     * 
     * @param parametersList list of needed parameters to do the batch
     * @return batch ID
     */
    public Long createBatchExecutions(List<T> parametersList) {
        return executionBuilder().buildBatch(parametersList);
    }

    /**
     * This creates a chain of a list of {@link Execution} to have them execution one after another and to interrupt the execution of one fails.
     * 
     * @param parametersList list of needed parameters to do the job in the order of the execution chain
     * @return chain ID
     */
    public Long createChainedExecutions(List<T> parametersList) {
        return executionBuilder().buildChain(parametersList);
    }

    /**
     * Create a builder to instantiate attributes of an execution with parameters
     * 
     * <pre>
     * Example: {
     *     A params = new A();
     *     execution().buld(params);
     * 
     *     execution().prioritize().create(params);
     * 
     *     List<A> paramsList = new ArrayList<>();
     *     execution().delayedFor(3L, ChronoUnit.SECONDS).createBatch(paramsList);
     * }
     * </pre>
     * 
     * @return the builder
     */
    public ParameterExecutionBuilder executionBuilder() {
        return new ParameterExecutionBuilder();
    }

    public class ParameterExecutionBuilder extends BaseExecutionBuilder<ParameterExecutionBuilder> {

        /**
         * Builds an execution with the defined attributes.
         * 
         * @return execution ID
         */
        public Long build(T parameters) {
            return createExecution(parameters, priority, plannedFor, expiresAt, null, null).getId();
        }

        /**
         * This builds a batch of {@link Execution} objects
         * 
         * @param parametersList list of needed parameters to do the batch
         * @return batch ID
         */
        public Long buildBatch(List<T> parametersList) {

            Long batchId = null;

            for (T parameter : parametersList) {
                if (batchId == null) { // start of batch

                    Execution execution = createExecution(parameter, priority, plannedFor, expiresAt, -1L, null);
                    // Use the Id of the first added job execution in Batch as BatchId.
                    execution.setBatchId(execution.getId());
                    workhorseController.updateExecution(execution);

                    batchId = execution.getId();
                } else { // now that we have the batch id, all the beloning executions can have it!
                    createExecution(parameter, priority, plannedFor, expiresAt, batchId, null);
                }
            }
            return batchId;
        }

        /**
         * This builds a chain of {@link Execution} objects, so when the first one gets executed it will bring all its chained friends.
         * 
         * @param parametersList list of needed parameters to do the job in the order of the execution chain
         * @return chain ID
         */
        public Long buildChain(List<T> parametersList) {

            Long chainId = null;

            for (T parameter : parametersList) {
                if (chainId == null) { // start of chain

                    Execution execution = createExecution(parameter, priority, plannedFor, expiresAt, null, -1L);
                    execution.setChainId(execution.getId());
                    workhorseController.updateExecution(execution);

                    chainId = execution.getId();
                } else { // now that we have the chain id, all the beloning executions can have it!
                    createExecution(parameter, priority, plannedFor, expiresAt, null, chainId);
                }
            }
            return chainId;
        }

        /**
         * This creates a batch of {@link Execution} objects
         * 
         * @param parametersList list of needed parameters to do the batch
         * @return batch ID
         * @deprecated use the {@link #buildBatch()}
         */
        @Deprecated
        public Long createBatch(List<T> parametersList) {
            return buildBatch(parametersList);
        }

        /**
         * This creates a chain of {@link Execution} objects, so when the first one gets executed it will bring all its chained friends.
         * 
         * @param parametersList list of needed parameters to do the job in the order of the execution chain
         * @return chain ID
         * @deprecated use the {@link #buildBatch()}
         */
        @Deprecated
        public Long createChain(List<T> parametersList) {
            return buildChain(parametersList);
        }

        /**
         * Create an execution with the defined attributes.
         * 
         * @return execution ID
         * @deprecated use the {@link #build(Object)}
         */
        @Deprecated
        public Long create(T parameters) {
            return build(parameters);
        }
    }

    /**
     * Create a builder to instantiate attributes of an execution with parameters
     * 
     * <pre>
     * Example: {
     *     A params = new A();
     *     execution().buld(params);
     * 
     *     execution().prioritize().create(params);
     * 
     *     List<A> paramsList = new ArrayList<>();
     *     execution().delayedFor(3L, ChronoUnit.SECONDS).createBatch(paramsList);
     * }
     * </pre>
     * 
     * @return the builder
     * @deprecated use the {@link #executionBuilder()}
     */
    @Deprecated
    public ParameterExecutionBuilder execution() {
        return new ParameterExecutionBuilder();
    }

    /**
     * @deprecated please use the {@link WorkerWith#execution()}
     * 
     *             <i>Convenience method to create an execution.</i><br>
     *             <br>
     * 
     *             This creates a {@link Execution} object that gets added to the job engine to be executed as soon as possible.
     * 
     * @param parameters needed parameters to do the job
     * @param priority priority queuing
     * @param plannedFor specified time for the execution
     * @return execution ID
     */
    @Deprecated
    public Long createExecution(T parameters, Boolean priority, LocalDateTime plannedFor) {
        return createExecution(parameters, priority, plannedFor, null, null, null).getId();
    }

    /**
     * @deprecated please use the {@link WorkerWith#execution()}
     * 
     *             <i>Convenience method to create an execution</i><br>
     *             <br>
     * 
     *             This creates a {@link Execution} object that gets added to the job engine to be executed as soon as possible.
     * 
     * @param parameters needed parameters to do the job
     * @param priority priority queuing
     * @param delayValue time to wait
     * @param delayUnit what kind of time to wait
     * @return execution ID
     */
    @Deprecated
    public Long createExecution(T parameters, Boolean priority, Long delayValue, ChronoUnit delayUnit) {
        return createExecution(parameters, priority, WorkhorseUtil.delayToMaturity(delayValue, delayUnit), null, null, null).getId();
    }

    /**
     * @deprecated please use the {@link WorkerWith#execution()}
     * 
     *             <i>Convenience method to create an execution</i><br>
     *             <br>
     *             This creates a {@link Execution} object that gets added to the priority queue of the job engine to be treated first class.
     * 
     * @param parameters needed parameters to do the job
     * @return execution ID
     */
    @Deprecated
    public Long createPriorityExecution(T parameters) {
        return createExecution(parameters, true, null);
    }

    /**
     * @deprecated please use the {@link WorkerWith#execution()}
     * 
     *             <i>Convenience method to create an execution</i><br>
     *             <br>
     *             This creates a {@link Execution} object that gets added to the job engine after the given delay.
     * 
     * @param parameters needed parameters to do the job
     * @param delayValue time to wait
     * @param delayUnit what kind of time to wait
     * @return execution ID
     */
    @Deprecated
    public Long createDelayedExecution(T parameters, Long delayValue, ChronoUnit delayUnit) {
        return createExecution(parameters, false, delayValue, delayUnit);
    }

    /**
     * @deprecated please use the {@link WorkerWith#execution()}.
     * 
     *             <i>Convenience method to create an execution</i><br>
     *             <br>
     *             This creates a {@link Execution} object that gets added to the job engine at a specified time.
     * 
     * @param parameters needed parameters to do the job
     * @param plannedFor specified time for the execution
     * @return execution ID
     */
    @Deprecated
    public Long createPlannedExecution(T parameters, LocalDateTime plannedFor) {
        return createExecution(parameters, false, plannedFor);
    }

    /**
     * @deprecated please use the {@link WorkerWith#execution()} This creates a batch of {@link Execution} objects
     * 
     * @param parametersList list of needed parameters to do the batch
     * @param priority priority queuing
     * @param plannedFor specified time for the execution
     * @return batch ID
     */
    @Deprecated
    public Long createBatchExecutions(List<T> parametersList, Boolean priority, LocalDateTime plannedFor, LocalDateTime expiresAt) {

        Long batchId = null;

        for (T parameters : parametersList) {
            if (batchId == null) { // start of batch

                Execution execution = createExecution(parameters, priority, plannedFor, expiresAt, -1L, null);
                // Use the Id of the first added job execution in Batch as BatchId.
                execution.setBatchId(execution.getId());
                workhorseController.updateExecution(execution);

                batchId = execution.getId();
            } else { // now that we have the batch id, all the beloning executions can have it!
                createExecution(parameters, priority, plannedFor, expiresAt, batchId, null);
            }
        }
        return batchId;
    }

    /**
     * @deprecated please use the {@link WorkerWith#execution()} This creates a chain of {@link Execution} objects, so when the first one gets executed it will
     *             bring all its chained friends.
     * 
     * @param parametersList list of needed parameters to do the job in the order of the execution chain
     * @param priority priority queuing
     * @param plannedFor specified time for the execution
     * @param expiresAt specified time up to which the execution will be canceled
     * @return chain ID
     */
    @Deprecated
    public Long createChainedExecutions(List<T> parametersList, Boolean priority, LocalDateTime plannedFor, LocalDateTime expiresAt) {

        Long chainId = null;

        for (T parameter : parametersList) {
            if (chainId == null) { // start of chain

                Execution execution = createExecution(parameter, priority, plannedFor, expiresAt, null, -1L);
                execution.setChainId(execution.getId());
                workhorseController.updateExecution(execution);

                chainId = execution.getId();
            } else { // now that we have the chain id, all the beloning executions can have it!
                createExecution(parameter, priority, plannedFor, expiresAt, null, chainId);
            }
        }
        return chainId;
    }
}
