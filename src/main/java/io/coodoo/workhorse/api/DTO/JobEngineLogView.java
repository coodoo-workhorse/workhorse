package io.coodoo.workhorse.api.DTO;

import io.coodoo.workhorse.jobengine.entity.Job;
import io.coodoo.workhorse.jobengine.entity.JobStatus;
import io.coodoo.workhorse.log.entity.JobEngineLog;



/**
 * A LogView defines a single {@link Log} which is joined with job information if it has a relation.
 */

public class JobEngineLogView {

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

    public JobEngineLogView() {
    }

    public JobEngineLogView(JobEngineLog jobEngineLog, Job job) {

        if (job != null) {
            this.jobName = job.getName();
            this.jobDescription = job.getDescription();
            this.jobStatus = job.getStatus();
            this.jobFailRetries = job.getFailRetries();
            this.jobThreads = job.getThreads();
        }

        this.message = jobEngineLog.getMessage();
        this.jobId = jobEngineLog.getJobId();
        this.byUser = jobEngineLog.isByUser();
        this.changeParameter = jobEngineLog.getChangeParameter();
        this.changeOld = jobEngineLog.getChangeOld();
        this.changeNew = jobEngineLog.getChangeNew();
        this.hostName = jobEngineLog.getHostName();
        this.stacktrace = jobEngineLog.getStacktrace() != null && !jobEngineLog.getStacktrace().isEmpty();
    }
    

    

}
