package io.coodoo.workhorse.core.control.event;

import io.coodoo.workhorse.core.entity.JobStatus;

/**
 * @author coodoo GmbH (coodoo.io)
 */
public class JobErrorEvent {

    private Throwable throwable;

    private String message;

    private Long jobId;

    private JobStatus jobStatus;

    public JobErrorEvent() {}

    public JobErrorEvent(Throwable throwable, String message, Long jobId, JobStatus jobStatus) {
        this.jobId = jobId;
        this.message = message;
        this.jobStatus = jobStatus;
        this.throwable = throwable;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public JobStatus getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(JobStatus jobStatus) {
        this.jobStatus = jobStatus;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("JobErrorEvent [message=");
        builder.append(message);
        builder.append(", jobId=");
        builder.append(jobId);
        builder.append(", jobStatus=");
        builder.append(jobStatus);
        builder.append("]");
        return builder.toString();
    }

}
