package io.coodoo.workhorse.api.dto;

import java.time.LocalDateTime;

import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.core.entity.JobStatus;
import io.coodoo.workhorse.core.entity.WorkhorseLog;

/**
 * A LogView defines a single {@link Log} which is joined with job information if it has a relation.
 */
public class LogView {

    public Long id;
    /**
     * General log message
     */
    public String message;

    /**
     * optional reference to the job and its current attributes
     */
    public Long jobId;

    public String jobName;

    public String jobDescription;

    public JobStatus jobStatus;

    public Integer jobFailRetries;

    public Integer jobThreads;

    /**
     * <code>true</code> if log was made by an user, <code>false</code> if log was made by the system
     */
    public boolean byUser = false;

    /**
     * Name of changed parameter
     */
    public String changeParameter;

    /**
     * Old value of that changed parameter
     */
    public String changeOld;

    /**
     * New value of that changed parameter
     */
    public String changeNew;

    /**
     * Host name of the current running system
     */
    public String hostName;

    /**
     * If available we record the exception stacktrace
     */
    public boolean stacktrace;

    public LocalDateTime createdAt;

    public LocalDateTime updatedAt;

    public LogView() {}

    public LogView(WorkhorseLog workhorseLog, Job job) {

        if (job != null) {
            this.jobName = job.getName();
            this.jobDescription = job.getDescription();
            this.jobStatus = job.getStatus();
            this.jobFailRetries = job.getFailRetries();
            this.jobThreads = job.getThreads();
        }

        this.id = workhorseLog.getId();
        this.createdAt = workhorseLog.getCreatedAt();
        this.updatedAt = workhorseLog.getUpdatedAt();
        this.message = workhorseLog.getMessage();
        this.jobId = workhorseLog.getJobId();
        this.byUser = workhorseLog.isByUser();
        this.changeParameter = workhorseLog.getChangeParameter();
        this.changeOld = workhorseLog.getChangeOld();
        this.changeNew = workhorseLog.getChangeNew();
        this.hostName = workhorseLog.getHostName();
        this.stacktrace = workhorseLog.getStacktrace() != null && !workhorseLog.getStacktrace().isEmpty();
    }

}
