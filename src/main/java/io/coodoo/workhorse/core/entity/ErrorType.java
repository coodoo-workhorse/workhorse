package io.coodoo.workhorse.core.entity;

public enum ErrorType {

    JOB_THREAD_ERROR("Error in job thread"),

    JOB_THREAD_CANCELLED("Job thread cancelled"),

    NO_JOB_WORKER_FOUND("No Worker class found"),

    ERROR_BY_FOUND_JOB_WORKER("Can't handle Worker class found"),

    INVALID_SCHEDULE("Invalid schedule found"),

    FAILED_SCHEDULE("Failed to trigger schedule"),

    ERROR_BY_JOB_PERSIST("Can't persist job"),

    ERROR_BY_EXECUTION_PERSIST("Can't persist execution");

    String message;

    private ErrorType(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

}
