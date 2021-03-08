package io.coodoo.workhorse.core.boundary;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import io.coodoo.workhorse.core.control.BaseWorker;
import io.coodoo.workhorse.core.entity.Execution;

/**
 * Worker class to define the creation and processing of execution. Your job needs parameters? See {@link WorkerWith}!
 * 
 * @author coodoo GmbH (coodoo.io)
 */
public abstract class Worker extends BaseWorker {

    public abstract void doWork() throws Exception;

    @Override
    public void doWork(Execution execution) throws Exception {

        this.executionContext.init(execution);

        doWork();
    }

    /**
     * <i>This is an access point to get the job engine started with a new job execution.</i><br>
     * <br>
     * 
     * This creates a {@link Execution} object that gets added to the job engine to be executed as soon as possible.
     * 
     * @param priority priority queuing
     * @param plannedFor specified time for the execution
     * @return execution ID
     */
    public Long createExecution(Boolean priority, LocalDateTime plannedFor) {
        return createExecution(null, priority, plannedFor, null, null, null, null).getId();

    }

    /**
     * <i>This is an access point to get the job engine started with a new job execution.</i><br>
     * <br>
     * 
     * This creates a {@link Execution} object that gets added to the job engine to be executed as soon as possible.
     * 
     * @param priority priority queuing
     * @param delayValue time to wait
     * @param delayUnit what kind of time to wait
     * @return execution ID
     */
    public Long createExecution(Boolean priority, Long delayValue, ChronoUnit delayUnit) {
        return createExecution(null, priority, delayToMaturity(delayValue, delayUnit), null, null, null, null).getId();

    }

    public Long createPriorityExecution() {
        return createExecution(null, true, null, null, null, null, null).getId();
    }

    /**
     * <i>Convenience method to create an execution</i><br>
     * <br>
     * This creates a {@link Execution} object that gets added to the job engine after the given delay.
     * 
     * @param delayValue time to wait
     * @param delayUnit what kind of time to wait
     * @return execution ID
     */
    public Long createDelayedExecution(Long delayValue, ChronoUnit delayUnit) {
        return createExecution(null, false, delayToMaturity(delayValue, delayUnit), null, null, null, null).getId();
    }

    /**
     * <i>Convenience method to create an execution</i><br>
     * <br>
     * This creates a {@link Execution} object that gets added to the job engine at a specified time.
     * 
     * @param plannedFor specified time for the execution
     * @return execution ID
     */
    public Long createPlannedExecution(LocalDateTime plannedFor) {
        return createExecution(null, false, plannedFor, null, null, null, null).getId();
    }

    /**
     * <i>Convenience method to create an execution</i><br>
     * <br>
     * This creates a {@link Execution} object that will expire (cancel), if it is not being processed until the given time.
     * 
     * @param expiresAt specified time to cancel the execution
     * @return execution ID
     */
    public Long createToExpireExecution(LocalDateTime expiresAt) {
        return createExecution(null, false, null, expiresAt, null, null, null).getId();
    }
}
