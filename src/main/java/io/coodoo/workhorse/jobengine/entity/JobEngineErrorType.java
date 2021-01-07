package io.coodoo.workhorse.jobengine.entity;

public enum JobEngineErrorType {
    
    JOB_THREAD_ERROR("Error in job thread"),
  
    NO_JOB_WORKER_FOUND("No JobWorker class found"),

    ERROR_BY_FOUND_JOB_WORKER("Can't handle JobWorker class found"),

    INVALID_SCHEDULE("Invalid schedule found ");

    String message;

    private JobEngineErrorType(String message) {
       this.message = message;
    }

    public String getMessage() {
        return message;
    }

}
