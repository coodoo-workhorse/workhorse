package io.coodoo.workhorse.core.entity;

/**
 * @author coodoo GmbH (coodoo.de)
 */
public enum JobStatus {
    /**
     * The Job can be process
     */
    ACTIVE,

    /**
     * The Job can not be process
     */
    INACTIVE,

    /**
     * No Worker-Class found for this Job
     */
    NO_WORKER,

    /**
     * Error occurred while processing the job
     */
    ERROR,

}
