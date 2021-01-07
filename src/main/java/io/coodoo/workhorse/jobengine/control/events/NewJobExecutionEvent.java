package io.coodoo.workhorse.jobengine.control.events;

public class NewJobExecutionEvent {
    
    public Long jobId;
    public Long jobExecutionId;

    public NewJobExecutionEvent(Long jobId, Long jobExecutionId) {
        this.jobId = jobId;
        this.jobExecutionId = jobExecutionId;
    }

    @Override
    public String toString() {
        return "NewJobExecutionEvent [jobExecutionId=" + jobExecutionId + ", jobId=" + jobId + "]";
    }

}
