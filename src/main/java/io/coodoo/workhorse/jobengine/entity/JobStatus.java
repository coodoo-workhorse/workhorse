package io.coodoo.workhorse.jobengine.entity;

public enum JobStatus {
    /** 
     * The Job  can be process
    */
    ACTIVE,

    /** 
     * The Job  can not be process 
     * */
    INACTIVE,

    /** 
     * No JobWorker-Class found for this Job
     * */
    NO_WORKER,
 
    /**
     * Error occurred while processing the job
     */
    ERROR
}
