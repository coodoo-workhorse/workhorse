package io.coodoo.workhorse.core.boundary;

import io.coodoo.workhorse.core.control.BaseWorker;

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

    /**
     * The job engine will call this callback method after the job execution is finished. <br>
     * <i>If needed, this method can be overwritten to react on a finished job execution.</i>
     * 
     * @param executionId ID of current job execution that is finished
     * @param summary the message that summarizes the execution
     */
    public void onFinished(Long executionId, String summary) {
        super.onFinished(executionId);
    }

    /**
     * The job engine will call this callback method after the job execution has failed and there will be a retry of the failed job execution. <br>
     * <i>If needed, this method can be overwritten to react on a retry job execution.</i>
     * 
     * @param failedExecutionId ID of current job execution that has failed
     * @param retryExecutionId ID of new job execution that that will retry the failed one
     * @param throwable Cause of the failure
     */
    public void onRetry(Long failedExecutionId, Long retryExecutionId, Throwable throwable) {
        super.onRetry(failedExecutionId, retryExecutionId);
    }

    /**
     * The job engine will call this callback method after the job execution has failed. <br>
     * <i>If needed, this method can be overwritten to react on a failed job execution.</i>
     * 
     * @param executionId ID of current job execution that has failed
     * @param throwable Cause of the failure
     */
    public void onFailed(Long executionId, Throwable throwable) {
        super.onFailed(executionId);
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
