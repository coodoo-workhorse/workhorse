package io.coodoo.workhorse.core.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author coodoo GmbH (coodoo.de)
 */
public class WorkhorseInfo {

    private Long jobId;
    private int queuedExecutions = 0;
    private int queuedPriorityExecutions = 0;
    private List<Execution> runningExecutions = new ArrayList<>();
    private int threadCount = 0;
    private LocalDateTime threadStartTime;
    private boolean paused;

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public int getQueuedExecutions() {
        return queuedExecutions;
    }

    public void setQueuedExecutions(int queuedExecutions) {
        this.queuedExecutions = queuedExecutions;
    }

    public int getQueuedPriorityExecutions() {
        return queuedPriorityExecutions;
    }

    public void setQueuedPriorityExecutions(int queuedPriorityExecutions) {
        this.queuedPriorityExecutions = queuedPriorityExecutions;
    }

    public List<Execution> getRunningExecutions() {
        return runningExecutions;
    }

    public void setRunningExecutions(List<Execution> runningExecutions) {
        this.runningExecutions = runningExecutions;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public LocalDateTime getThreadStartTime() {
        return threadStartTime;
    }

    public void setThreadStartTime(LocalDateTime threadStartTime) {
        this.threadStartTime = threadStartTime;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    @Override
    public String toString() {
        return "WorkhorseInfo [jobId=" + jobId + ", queuedExecutions=" + queuedExecutions + ", queuedPriorityExecutions=" + queuedPriorityExecutions
                        + ", runningExecutions=" + runningExecutions + ", threadCount=" + threadCount + ", threadStartTime=" + threadStartTime + ", paused="
                        + paused + "]";
    }

}
