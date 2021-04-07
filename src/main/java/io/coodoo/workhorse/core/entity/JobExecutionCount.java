package io.coodoo.workhorse.core.entity;

import java.time.LocalDateTime;

/**
 * <p>
 * A JobExecutionCount defines the counts of {@link Execution} by status for a specific job or for all jobs between a time interval
 * </p>
 */
public class JobExecutionCount {
    private Long jobId;
    private LocalDateTime from;
    private LocalDateTime to;
    private long total;
    private long planned;
    private long queued;
    private long running;
    private long finished;
    private long failed;
    private long aborted;

    public JobExecutionCount() {}

    public JobExecutionCount(Long jobId, LocalDateTime from, LocalDateTime to, long planned, long queued, long running, long finished, long failed,
                    long aborted) {
        this.jobId = jobId;
        this.from = from;
        this.to = to;
        this.planned = planned;
        this.queued = queued;
        this.running = running;
        this.finished = finished;
        this.failed = failed;
        this.aborted = aborted;

        this.total = planned + queued + running + finished + failed + aborted;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public LocalDateTime getFrom() {
        return from;
    }

    public void setFrom(LocalDateTime from) {
        this.from = from;
    }

    public LocalDateTime getTo() {
        return to;
    }

    public void setTo(LocalDateTime to) {
        this.to = to;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getPlanned() {
        return planned;
    }

    public void setPlanned(long planned) {
        this.planned = planned;
    }

    public long getQueued() {
        return queued;
    }

    public void setQueued(long queued) {
        this.queued = queued;
    }

    public long getRunning() {
        return running;
    }

    public void setRunning(long running) {
        this.running = running;
    }

    public long getFinished() {
        return finished;
    }

    public void setFinished(long finished) {
        this.finished = finished;
    }

    public long getFailed() {
        return failed;
    }

    public void setFailed(long failed) {
        this.failed = failed;
    }

    public long getAborted() {
        return aborted;
    }

    public void setAborted(long aborted) {
        this.aborted = aborted;
    }

}
