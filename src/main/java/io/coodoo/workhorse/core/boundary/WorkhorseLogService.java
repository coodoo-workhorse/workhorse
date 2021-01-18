package io.coodoo.workhorse.core.boundary;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.coodoo.workhorse.config.entity.WorkhorseConfig;
import io.coodoo.workhorse.core.control.WorkhorseController;
import io.coodoo.workhorse.core.control.event.JobErrorEvent;
import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.core.entity.JobStatus;
import io.coodoo.workhorse.core.entity.WorkhorseLog;
import io.coodoo.workhorse.persistence.interfaces.LogPersistence;
import io.coodoo.workhorse.persistence.interfaces.qualifier.LogQualifier;
import io.coodoo.workhorse.util.WorkhorseUtil;

/**
 * Provides access to the job logs
 * 
 * @author coodoo GmbH (coodoo.io)
 */
@ApplicationScoped
public class WorkhorseLogService {

    private static final Logger log = Logger.getLogger(WorkhorseLogService.class);

    @Inject
    WorkhorseController workhorseController;

    @Inject
    WorkhorseConfig jobEngineConfig;

    @Inject
    @LogQualifier
    LogPersistence logPersistence;

    @Inject
    WorkhorseLogService jobLogService;

    public WorkhorseLog getLog(Long id) {
        return logPersistence.get(id);
    }

    public List<WorkhorseLog> getAllLogs(int limit) {
        return logPersistence.getAll(limit);
    }

    /**
     * create a log about an update in the job engine.
     * 
     * @param jobId
     * @param jobStatus
     * @param changeParameter
     * @param changeOld
     * @param changeNew
     * @param message
     * @return
     */
    public WorkhorseLog logChange(Long jobId, JobStatus jobStatus, String changeParameter, Object changeOld, Object changeNew, String message) {

        String co = changeOld == null ? "" : changeOld.toString();
        String cn = changeNew == null ? "" : changeNew.toString();

        if (message == null) {
            message = String.format(jobEngineConfig.getLogChange(), changeParameter, co, cn);
        }
        return createJobEngineLog(message, jobId, jobStatus, true, changeParameter, co, cn, null);
    }

    /**
     * Create and persist a log about an event in the job engine
     * 
     * @param message
     * @param jobId
     * @param jobStatus
     * @param byUser
     * @param changeParameter
     * @param changeOld
     * @param changeNew
     * @param stacktrace
     * @return
     */
    public WorkhorseLog createJobEngineLog(String message, Long jobId, JobStatus jobStatus, boolean byUser, String changeParameter, String changeOld,
                    String changeNew, String stacktrace) {

        WorkhorseLog jobEngineLog = new WorkhorseLog();
        jobEngineLog.setMessage(message);
        jobEngineLog.setJobId(jobId);
        jobEngineLog.setJobStatus(jobStatus);
        jobEngineLog.setByUser(byUser);
        jobEngineLog.setChangeParameter(changeParameter);
        jobEngineLog.setChangeOld(changeOld);
        jobEngineLog.setChangeNew(changeNew);
        jobEngineLog.setHostName(WorkhorseUtil.getHostName());
        jobEngineLog.setStacktrace(stacktrace);

        logPersistence.persist(jobEngineLog);
        log.info("Created JobEngineLog : " + log);
        return jobEngineLog;
    }

    /**
     * Delete all logs concerning the job of the given id
     * 
     * @param jobId Id of the job, whose logs have to be delete
     * @return
     */
    public int deleteAllByJobId(Long jobId) {
        return logPersistence.deleteByJobId(jobId);
    }

    /**
     * Observe the exceptions on jobs and create a log after recieving one.
     * 
     * @param jobErrorPayload payload of the event
     */
    public void logException(@Observes JobErrorEvent jobErrorPayload) {

        String message = jobErrorPayload.getMessage() != null ? jobErrorPayload.getMessage()
                        : WorkhorseUtil.getMessagesFromException(jobErrorPayload.getThrowable());

        createJobEngineLog(message, jobErrorPayload.getJobId(), jobErrorPayload.getJobStatus(), false, null, null, null,
                        WorkhorseUtil.stacktraceToString(jobErrorPayload.getThrowable()));
    }

    /**
     * Logs a text message
     * 
     * @param message text to log
     * @param jobId optional: belonging {@link Job}-ID
     * @param byUser <code>true</code> if author is a user, <code>false</code> if author is the system
     * @return the resulting log entry
     */
    public WorkhorseLog logMessage(String message, Long jobId, boolean byUser) {

        JobStatus jobStatus = null;
        if (jobId != null) {
            Job job = workhorseController.getJobById(jobId);
            if (job != null) {
                jobStatus = job.getStatus();
            }
        }
        return createJobEngineLog(message, jobId, jobStatus, byUser, null, null, null, null);
    }

}
