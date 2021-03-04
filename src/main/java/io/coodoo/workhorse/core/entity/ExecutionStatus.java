package io.coodoo.workhorse.core.entity;

/**
 * This enum describes all status that an execution can get
 * 
 * @author coodoo GmbH (coodoo.io)
 */
public enum ExecutionStatus {

    /**
     * Execution is planned to be processed in the future.
     */
    PLANNED,

    /**
     * Execution is queued for processing.
     */
    QUEUED,

    /**
     * Execution is currently running, the doWork() Method is called.
     */
    RUNNING,

    /**
     * Execution finished without any error.
     */
    FINISHED,

    /**
     * The Execution failed because of an error.
     * 
     * TODO Secondary/Fail status (TIMEOUT, EXCEPTION, MANUAL, CHAIN)
     */
    FAILED,

    /**
     * The Execution was aborted by an user. The engine never set this status.
     */
    ABORTED

}
