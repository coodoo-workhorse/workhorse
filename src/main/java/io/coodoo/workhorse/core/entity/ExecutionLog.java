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
    private String error;

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

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getStacktrace() {
        return stacktrace;
    }

    public void setStacktrace(String stacktrace) {
        this.stacktrace = stacktrace;
    }

    @Override
    public String toString() {
        return "ExecutionLog [executionId=" + executionId + ", log=" + log + ", error=" + error + ", stacktrace=" + stacktrace + "]";
    }

}
