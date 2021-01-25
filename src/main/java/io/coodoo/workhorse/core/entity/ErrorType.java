package io.coodoo.workhorse.core.entity;

public enum ErrorType {
    
    JOB_THREAD_ERROR("Error in job thread"),
  
    NO_JOB_WORKER_FOUND("No Worker class found"),

    ERROR_BY_FOUND_JOB_WORKER("Can't handle Worker class found"),

    INVALID_SCHEDULE("Invalid schedule found ");

    String message;

    private ErrorType(String message) {
       this.message = message;
    }

    public String getMessage() {
        return message;
    }

}
