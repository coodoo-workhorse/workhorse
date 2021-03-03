package io.coodoo.workhorse.core.entity;

/**
 * This enum describes all status that an failed execution can get
 * 
 * @author coodoo GmbH (coodoo.io)
 */
public enum ExecutionFailStatus {

    /**
     * No Fail
     */
    NONE,

    /**
     * The execution throws an exception during the processing
     */
    EXCEPTION,

    /**
     * The execution was running for a too long time
     */
    TIMEOUT,

    /**
     * The execution couldn't be executed within a given time window
     */
    EXPIRED,

    /**
     * The execution was set on FAIL by an user
     */
    MANUAL,

}
