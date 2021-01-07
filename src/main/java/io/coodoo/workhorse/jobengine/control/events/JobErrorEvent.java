package io.coodoo.workhorse.jobengine.control.events;

import io.coodoo.workhorse.jobengine.entity.JobStatus;

public class JobErrorEvent {

    private Long jobId;

    private String message;

    private JobStatus jobStatus;
    
    private Throwable e;

    public JobErrorEvent() {}

    public JobErrorEvent(Throwable e, String message, Long jobId, JobStatus jobStatus) {
        this.jobId = jobId;
        this.message = message;
        this.jobStatus = jobStatus;
        this.e = e;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public JobStatus getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(JobStatus jobStatus) {
        this.jobStatus = jobStatus;
    }

    public Throwable getE() {
        return e;
    }

    public void setE(Exception e) {
        this.e = e;
    }
}
