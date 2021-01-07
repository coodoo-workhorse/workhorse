package io.coodoo.workhorse.log.entity;

import io.coodoo.workhorse.jobengine.entity.BaseEntity;
import io.coodoo.workhorse.jobengine.entity.JobStatus;

public class JobEngineLog extends BaseEntity {
    
    /**
     * General log message
     */
    private String message;

    /**
     * optional reference to the job
     */
    private Long jobId;

    /**
     * Job status at creation
     */
    private JobStatus jobStatus;

    /**
     * <code>true</code> if log was made by an user, <code>false</code> if log was made by the system
     */
    private boolean byUser = false;

    /**
     * Name of changed parameter
     */
    private String changeParameter;

    /**
     * Old value of that changed parameter
     */
    private String changeOld;

    /**
     * New value of that changed parameter
     */
    private String changeNew;

    /**
     * Host name of the current running system
     */
    private String hostName;

    /**
     * If available we record the exception stacktrace
     */
    private String stacktrace;

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

    public boolean isByUser() {
        return byUser;
    }

    public void setByUser(boolean byUser) {
        this.byUser = byUser;
    }

    public String getChangeParameter() {
        return changeParameter;
    }

    public void setChangeParameter(String changeParameter) {
        this.changeParameter = changeParameter;
    }

    public String getChangeOld() {
        return changeOld;
    }

    public void setChangeOld(String changeOld) {
        this.changeOld = changeOld;
    }

    public String getChangeNew() {
        return changeNew;
    }

    public void setChangeNew(String changeNew) {
        this.changeNew = changeNew;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getStacktrace() {
        return stacktrace;
    }

    public void setStacktrace(String stacktrace) {
        this.stacktrace = stacktrace;
    }
}
