package io.coodoo.workhorse.log.boundary;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import io.coodoo.workhorse.config.entity.JobEngineConfig;
import io.coodoo.workhorse.jobengine.control.JobEngineController;
import io.coodoo.workhorse.jobengine.entity.Job;
import io.coodoo.workhorse.jobengine.entity.JobStatus;
import io.coodoo.workhorse.log.control.JobEngineLogControl;
import io.coodoo.workhorse.log.entity.JobEngineLog;

@RequestScoped
public class JobEngineLogService {

    @Inject
    JobEngineLogControl jobEngineLogControl;

    @Inject
    JobEngineController jobEngineController;

    @Inject
    JobEngineConfig jobEngineConfig;

    public JobEngineLog getLog(Long id) {
        return jobEngineLogControl.get(id);
    }

    public List<JobEngineLog> getAllLogs(int limit) {

        return jobEngineLogControl.getAllLogs(limit);

    }

    /**
     * Logs a text message
     * 
     * @param message text to log
     * @param jobId   optional: belonging {@link Job}-ID
     * @param byUser  <code>true</code> if author is a user, <code>false</code> if
     *                author is the system
     * @return the resulting log entry
     */
    public JobEngineLog logMessage(String message, Long jobId, boolean byUser) {

        JobStatus jobStatus = null;
        if (jobId != null) {
            Job job = jobEngineController.getJobById(jobId);
            if (job != null) {
                jobStatus = job.getStatus();
            }
        }
        return jobEngineLogControl.createJobEngineLog(message, jobId, jobStatus, byUser, null, null, null, null);
    }

    /**
     * Delete all logs concerning the job of the given id
     * @param jobId Id of the job, whose logs have to be delete
     * @return
     */
    public int deleteAllByJobId(Long jobId) {
        return jobEngineLogControl.deleteAllByJobId(jobId);
    }

}
