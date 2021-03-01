package io.coodoo.workhorse.core.entity;

import java.time.LocalDateTime;

import javax.json.bind.annotation.JsonbDateFormat;

/**
 * <p>
 * A Exceuction defines a single job which will be excecuted by the job engine.
 * </p>
 * <p>
 * Every needed information to do a single job is stored with this entity.
 * </p>
 * 
 * @author coodoo GmbH (coodoo.io)
 */
public class Execution extends BaseEntity {

    /**
     * Id to refer to the concerned job
     */
    private Long jobId;

    /**
     * Status of the Execution. <code>QUEUED</code>, <code>RUNNING</code>,
     * <code>FINISHED</code>, <code>FAILED</code>, <code>ABORTED</code>
     */
    private ExecutionStatus status;

    /**
     * Timestamp of the begin of the processing
     */
    private LocalDateTime startedAt;

    /**
     * Timestamp of the end of the processing
     */
    private LocalDateTime endedAt;

    /**
     * Recorded duration of the execution
     */
    private Long duration;

    // TODO Execution Outomce als teil eines info-features: status zeit outcome

    /**
     * If a job exectution has the priority set to <code>true</code> it will be
     * executed before all jobs with priority <code>false</code>.
     */
    private Boolean priority;

    /**
     * If a maturity is given, the job execution will not be executed before this
     * time.
     */
    @JsonbDateFormat(value = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime plannedAt;

    /**
     * Id to refer to a group of executions to handle as a single entity.
     */
    private Long batchId;

    /**
     * Id to refer to a group of executions to process by an order.
     */
    private Long chainId;

    /**
     * Id to the previous execution to process, if the execution belong to a chained
     * Execution.
     */
    private Long chainedPreviousExecutionId;

    /**
     * Id to the next execution to process, if the execution belong to a chained
     * Execution.
     */
    private Long chainedNextExecutionId;

    /**
     * Parameters as JSON to process the execution
     */
    private String parameters;

    /**
     * Hash value of the parameters to check their value.
     */
    private Integer parametersHash;

    /**
     * Log info about the execution
     */
    private String log;

    /**
     * Number of retries
     */
    private int failRetry;

    /**
     * Id of the failed Execution that should be retry
     */
    private Long failRetryExecutionId;

    /**
     * The exception message, if the job execution ends in an exception.
     */
    private String failMessage;

    /**
     * The exception stacktrace, if the job execution ends in an exception.
     */
    private String failStacktrace;

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public Boolean getPriority() {
        return priority;
    }

    public void setPriority(Boolean priority) {
        this.priority = priority;
    }

    public LocalDateTime getPlannedAt() {
        return plannedAt;
    }

    public void setPlannedAt(LocalDateTime plannedAt) {
        this.plannedAt = plannedAt;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public Long getChainId() {
        return chainId;
    }

    public void setChainId(Long chainId) {
        this.chainId = chainId;
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public Long getChainedNextExecutionId() {
        return chainedNextExecutionId;
    }

    public void setChainedNextExecutionId(Long chainedNextExecutionId) {
        this.chainedNextExecutionId = chainedNextExecutionId;
    }

    public Integer getParametersHash() {
        return parametersHash;
    }

    public void setParametersHash(Integer parametersHash) {
        this.parametersHash = parametersHash;
    }

    public String getFailMessage() {
        return failMessage;
    }

    public void setFailMessage(String failMessage) {
        this.failMessage = failMessage;
    }

    public String getFailStacktrace() {
        return failStacktrace;
    }

    public void setFailStacktrace(String failStacktrace) {
        this.failStacktrace = failStacktrace;
    }

    public Long getChainedPreviousExecutionId() {
        return chainedPreviousExecutionId;
    }

    public void setChainedPreviousExecutionId(Long chainedPreviousExecutionId) {
        this.chainedPreviousExecutionId = chainedPreviousExecutionId;
    }

    public int getFailRetry() {
        return failRetry;
    }

    public void setFailRetry(int failRetry) {
        this.failRetry = failRetry;
    }

    public Long getFailRetryExecutionId() {
        return failRetryExecutionId;
    }

    public void setFailRetryExecutionId(Long failRetryExecutionId) {
        this.failRetryExecutionId = failRetryExecutionId;
    }

    @Override
    public String toString() {
        return "Execution [Id=" + id + ", batchId=" + batchId + ", chainId=" + chainId + ", jobId=" + jobId
                + ", maturity=" + plannedAt + ", parameters=" + parameters + ", priority=" + priority + ", status="
                + status + "]";
    }

}
