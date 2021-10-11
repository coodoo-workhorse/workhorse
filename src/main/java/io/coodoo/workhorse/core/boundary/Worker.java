package io.coodoo.workhorse.core.boundary;

import io.coodoo.workhorse.core.control.BaseWorker;
import io.coodoo.workhorse.core.entity.Execution;

/**
 * Worker class to define the creation and processing of execution. Your job needs parameters? See {@link WorkerWith}!
 * 
 * @author coodoo GmbH (coodoo.io)
 */
public abstract class Worker extends BaseWorker {

    /**
     * Process the execution
     * 
     * @return a message to summarize the execution
     * @throws Exception
     */
    public abstract String doWork() throws Exception;

    @Override
    public String doWork(Execution execution) throws Exception {

        this.executionContext.init(execution);

        return doWork();
    }

    /**
     * Create a builder to instantiate attributes of an execution
     * 
     * <pre>
     * Examples:
     * {@code 
     * 
     * createExecution().build();
     * 
     * createExecution().prioritize().expiresAtHours(3).build();
     * }
     * </pre>
     * 
     * @return the builder
     */
    public ExecutionBuilder executionBuilder() {
        return new ExecutionBuilder();
    }

    public class ExecutionBuilder extends BaseExecutionBuilder<ExecutionBuilder> {
    }

}
