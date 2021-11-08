package io.coodoo.workhorse.core.entity;

/**
 * The class defines all types of messages that can be stored about an execution.
 * 
 * @author coodoo GmbH (coodoo.io)
 */
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

    public String getStacktrace() {
        return stacktrace;
    }

    public void setStacktrace(String stacktrace) {
        this.stacktrace = stacktrace;
    }

    /**
     * {@link ExecutionLog#toString()} is used in logging, so it is kept short
     */
    @Override
    public String toString() {
        return "ExecutionLog ID=" + id + ", Execution-ID=" + executionId + ", " + log;
    }

}
