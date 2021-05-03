package io.coodoo.workhorse.api.dto;

import io.coodoo.workhorse.core.control.JobThread;

/**
 * Snapshot of an {@link JobThread} object.
 * 
 * @author coodoo GmbH (coodoo.io)
 */
public class JobThreadDTO {

    public Long jobId;
    public String jobName;
    public String thread;
    public Long runningExecution;
    public boolean stopThread;

    public JobThreadDTO() {}

    public JobThreadDTO(JobThread jobThread) {

        this.jobId = jobThread.getJob().getId();
        this.jobName = jobThread.getJob().getName();
        this.thread = jobThread.getThread().getName();
        this.runningExecution = jobThread.getRunningExecution().getId();
        this.stopThread = jobThread.isStopMe();
    }
}
