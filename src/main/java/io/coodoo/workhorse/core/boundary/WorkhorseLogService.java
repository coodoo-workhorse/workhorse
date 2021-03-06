package io.coodoo.workhorse.core.boundary;

import java.util.List;
import java.util.concurrent.CancellationException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.coodoo.workhorse.core.control.StaticConfig;
import io.coodoo.workhorse.core.control.WorkhorseController;
import io.coodoo.workhorse.core.control.event.JobErrorEvent;
import io.coodoo.workhorse.core.entity.ErrorType;
import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.core.entity.JobStatus;
import io.coodoo.workhorse.core.entity.WorkhorseLog;
import io.coodoo.workhorse.persistence.interfaces.LogPersistence;
import io.coodoo.workhorse.persistence.interfaces.listing.ListingParameters;
import io.coodoo.workhorse.persistence.interfaces.listing.ListingResult;
import io.coodoo.workhorse.persistence.interfaces.qualifier.LogQualifier;
import io.coodoo.workhorse.util.WorkhorseUtil;

/**
 * Provides access to the job logs
 * 
 * @author coodoo GmbH (coodoo.io)
 */
@ApplicationScoped
public class WorkhorseLogService {

    private static final Logger log = LoggerFactory.getLogger(WorkhorseLogService.class);

    private static final String JOB_THREAD_CANCELLED_MESSAGE = "This job thread has been cancelled due to an error in another job thread of this job.";

    @Inject
    WorkhorseController workhorseController;

    @Inject
    @LogQualifier
    LogPersistence logPersistence;

    public WorkhorseLog getLog(Long id) {
        return logPersistence.get(id);
    }

    public List<WorkhorseLog> getAllLogs(int limit) {
        return logPersistence.getAll(limit);
    }

    /**
     * Get the listing result of logs
     * 
     * @param listingParameters defines the listing queue. It contains optional query parameters as described above
     * @return list of logs
     */
    public ListingResult<WorkhorseLog> getWorkhorseLogListing(ListingParameters listingParameters) {
        if (listingParameters.getSortAttribute() == null || listingParameters.getSortAttribute().isEmpty()) {
            listingParameters.setSortAttribute("-createdAt");
        }

        return logPersistence.getWorkhorseLogListing(listingParameters);
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
            message = String.format(StaticConfig.LOG_CHANGE, changeParameter, co, cn);
        }
        return createLog(message, jobId, jobStatus, true, changeParameter, co, cn, null);
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
    public WorkhorseLog createLog(String message, Long jobId, JobStatus jobStatus, boolean byUser, String changeParameter, String changeOld, String changeNew,
                    String stacktrace) {

        WorkhorseLog workhorseLog = new WorkhorseLog();
        workhorseLog.setMessage(message);
        workhorseLog.setJobId(jobId);
        workhorseLog.setJobStatus(jobStatus);
        workhorseLog.setByUser(byUser);
        workhorseLog.setChangeParameter(changeParameter);
        workhorseLog.setChangeOld(changeOld);
        workhorseLog.setChangeNew(changeNew);
        workhorseLog.setHostName(WorkhorseUtil.getHostName());
        workhorseLog.setStacktrace(stacktrace);

        WorkhorseLog persistedWorkhorseLog = logPersistence.persist(workhorseLog);
        if (persistedWorkhorseLog == null || persistedWorkhorseLog.getId() == null) {
            throw new RuntimeException("The workhorseLog " + workhorseLog + " couldn't be persisited by the persisitence.");
        }

        if (message != null && jobId == null) {
            log.info("{}", message);
        } else {
            log.info("{}", workhorseLog);
        }

        return persistedWorkhorseLog;
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

        String message;
        String stacktrace;
        if (jobErrorPayload.getThrowable().getClass().equals(CancellationException.class)) {
            // A cancellationException means that this job thread has been cancelled due to an error in another job thread of this job
            // It is the reason why the message is different.
            log.warn(ErrorType.JOB_THREAD_CANCELLED.getMessage());
            message = ErrorType.JOB_THREAD_CANCELLED.getMessage();
            stacktrace = JOB_THREAD_CANCELLED_MESSAGE + System.lineSeparator() + WorkhorseUtil.stacktraceToString(jobErrorPayload.getThrowable());

        } else {
            message = jobErrorPayload.getMessage() != null ? jobErrorPayload.getMessage()
                            : WorkhorseUtil.getMessagesFromException(jobErrorPayload.getThrowable());

            stacktrace = WorkhorseUtil.stacktraceToString(jobErrorPayload.getThrowable());
        }

        createLog(message, jobErrorPayload.getJobId(), jobErrorPayload.getJobStatus(), false, null, null, null, stacktrace);
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
        return createLog(message, jobId, jobStatus, byUser, null, null, null, null);
    }

}
