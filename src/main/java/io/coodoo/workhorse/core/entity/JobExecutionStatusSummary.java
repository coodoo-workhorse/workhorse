package io.coodoo.workhorse.core.entity;

/**
 * @author coodoo GmbH (coodoo.de)
 */
public class JobExecutionStatusSummary {

    private ExecutionStatus status;
    private Long count;
    private Job job;

    public JobExecutionStatusSummary() {}

    public JobExecutionStatusSummary(ExecutionStatus status, Long count, Job job) {
        this.status = status;
        this.count = count;
        this.job = job;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    @Override
    public String toString() {
        return "JobExecutionStatusSummary [status=" + status + ", count=" + count + ", job=" + job + "]";
    }

}
