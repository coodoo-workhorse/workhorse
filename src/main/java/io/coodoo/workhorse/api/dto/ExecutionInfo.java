package io.coodoo.workhorse.api.dto;

import java.time.LocalDateTime;

import io.coodoo.workhorse.core.entity.ExecutionStatus;

/**
 * @author coodoo GmbH (coodoo.io)
 */
public class ExecutionInfo {

    private Long id;

    private ExecutionStatus status;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private Long duration;

    private Long failRetryExecutionId;

    public ExecutionInfo() {}

    public ExecutionInfo(Long id, ExecutionStatus status, LocalDateTime startedAt, LocalDateTime endedAt, Long duration, Long failRetryExecutionId) {
        super();
        this.id = id;
        this.status = status;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.duration = duration;
        this.failRetryExecutionId = failRetryExecutionId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public Long getFailRetryExecutionId() {
        return failRetryExecutionId;
    }

    public void setFailRetryExecutionId(Long failRetryExecutionId) {
        this.failRetryExecutionId = failRetryExecutionId;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("JobExecutionInfo [id=");
        builder.append(id);
        builder.append(", status=");
        builder.append(status);
        builder.append(", startedAt=");
        builder.append(startedAt);
        builder.append(", endedAt=");
        builder.append(endedAt);
        builder.append(", duration=");
        builder.append(duration);
        builder.append(", failRetryExecutionId=");
        builder.append(failRetryExecutionId);
        builder.append("]");
        return builder.toString();
    }

}
