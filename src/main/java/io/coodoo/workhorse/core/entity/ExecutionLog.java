package io.coodoo.workhorse.core.entity;

public class ExecutionLog extends BaseEntity {

    /**
     * Reference to the corresponding {@link Execution}
     */
    private Long executionId;

    /**
     * Log info about the execution
     */
    private String log;

    /**
     * The exception message, if the job execution ends in an exception.
     */
    private String exception;

    /**
     * The exception stacktrace, if the job execution ends in an exception.
     */
    private String stacktrace;

    public Long getExecutionId() {
        return executionId;
    }

    public void setExecutionId(Long executionId) {
        this.executionId = executionId;
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
    }

    public String getException() {
        return exception;
    }

    public void setException(String failMessage) {
        this.exception = failMessage;
    }

    public String getStacktrace() {
        return stacktrace;
    }

    public void setStacktrace(String failStacktrace) {
        this.stacktrace = failStacktrace;
    }
}
