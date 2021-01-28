package io.coodoo.workhorse.core.boundary;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import io.coodoo.workhorse.core.control.BaseWorker;
import io.coodoo.workhorse.core.entity.Execution;

public abstract class Worker extends BaseWorker {

    public abstract void doWork() throws Exception;

    @Override
    public void doWork(Execution jobExecution) throws Exception {
        init(jobExecution);
        doWork();
    }

    /**
     * <i>This is an access point to get the job engine started with a new job execution.</i><br>
     * <br>
     * 
     * This creates a {@link Execution} object that gets added to the job engine to be executed as soon as possible.
     * 
     * @param priority priority queuing
     * @param maturity specified time for the execution
     * @return job execution ID
     */
    public Long createJobExecution(Boolean priority, LocalDateTime maturity) {
        return createJobExecution(null, priority, maturity, null, null, null).getId();
  
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
     * @return job execution ID
     */
    public Long createJobExecution(Boolean priority, Long delayValue, ChronoUnit delayUnit) {
        return createJobExecution(null, priority, delayToMaturity(delayValue, delayUnit), null, null, null).getId();

    }

    public Long createPriorityJobExecution() {
        return createJobExecution(null, true, null, null, null, null).getId();
    }

     /**
     * <i>Convenience method to create a job execution</i><br>
     * <br>
     * This creates a {@link Execution} object that gets added to the job engine after the given delay.
     * 
     * @param delayValue time to wait
     * @param delayUnit what kind of time to wait
     * @return job execution ID
     */
    public Long createDelayedJobExecution(Long delayValue, ChronoUnit delayUnit) {
        return createJobExecution(null, false, delayToMaturity(delayValue, delayUnit), null, null, null).getId();
    }

    /**
     * <i>Convenience method to create a job execution</i><br>
     * <br>
     * This creates a {@link Execution} object that gets added to the job engine at a specified time.
     * 
     * @param maturity specified time for the execution
     * @return job execution ID
     */
    public Long createPlannedJobExecution(LocalDateTime maturity) {
        return createJobExecution(null, false, maturity, null, null, null).getId();
    }
}