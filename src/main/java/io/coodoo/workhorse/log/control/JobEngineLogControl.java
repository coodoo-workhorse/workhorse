package io.coodoo.workhorse.log.control;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.coodoo.workhorse.config.entity.JobEngineConfig;
import io.coodoo.workhorse.jobengine.control.events.JobErrorEvent;
import io.coodoo.workhorse.jobengine.entity.JobStatus;
import io.coodoo.workhorse.log.boundary.JobEngineLogService;
import io.coodoo.workhorse.log.entity.JobEngineLog;
import io.coodoo.workhorse.storage.persistenceInterface.JobEngineLogPersistence;
import io.coodoo.workhorse.storage.qualifiers.JobEngineLogDAO;
import io.coodoo.workhorse.util.JobEngineUtil;

@ApplicationScoped
public class JobEngineLogControl {

    private static final Logger log = Logger.getLogger(JobEngineLogControl.class);
    
    @Inject
    @JobEngineLogDAO
    JobEngineLogPersistence jobEngineLogPersistence;

    @Inject
    JobEngineLogService jobLogService;

    @Inject
    JobEngineConfig jobEngineConfig;

    public JobEngineLog get(Long id) {
        return jobEngineLogPersistence.get(id);
      }
  
      public List<JobEngineLog> getAllLogs(int limit) {
         return jobEngineLogPersistence.getAll(limit);
      }

      /**
       * create a log about an update in the job engine.
       * @param jobId
       * @param jobStatus
       * @param changeParameter
       * @param changeOld
       * @param changeNew
       * @param message
       * @return
       */
      public JobEngineLog logChange(Long jobId, JobStatus jobStatus, String changeParameter, Object changeOld, Object changeNew, String message) {

        String co = changeOld == null ? "" : changeOld.toString();
        String cn = changeNew == null ? "" : changeNew.toString();

        if (message == null) {
            message = String.format(jobEngineConfig.getLogChange(), changeParameter, co, cn);
        }
        return createJobEngineLog(message, jobId, jobStatus, true, changeParameter, co, cn, null);
    }
  
      /**
       * Create and persist a log about an event in the job engine
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
      public JobEngineLog createJobEngineLog(String message, Long jobId, JobStatus jobStatus, boolean byUser, String changeParameter, String changeOld, String changeNew,
      String stacktrace) {
  
          JobEngineLog jobEngineLog = new JobEngineLog();
          jobEngineLog.setMessage(message);
          jobEngineLog.setJobId(jobId);
          jobEngineLog.setJobStatus(jobStatus);
          jobEngineLog.setByUser(byUser);
          jobEngineLog.setChangeParameter(changeParameter);
          jobEngineLog.setChangeOld(changeOld);
          jobEngineLog.setChangeNew(changeNew);
          jobEngineLog.setHostName(JobEngineUtil.getHostName());
          jobEngineLog.setStacktrace(stacktrace);
  
          jobEngineLogPersistence.persist(jobEngineLog);
          log.info("Created JobEngineLog : " + log);
          return jobEngineLog;
      }
  
      public int deleteAllByJobId(Long jobId) {
         return jobEngineLogPersistence.deleteByJobId(jobId);
      }

      /**
       * Observe the exceptions on jobs and create a log after recieving one.
       * @param jobErrorPayload payload of the event
       */
      public void logException(@Observes JobErrorEvent jobErrorPayload) {
        createJobEngineLog(jobErrorPayload.getMessage() != null ? jobErrorPayload.getMessage() : JobEngineUtil.getMessagesFromException(jobErrorPayload.getE()), jobErrorPayload.getJobId(), jobErrorPayload.getJobStatus(), false, null, null, null,
                        JobEngineUtil.stacktraceToString(jobErrorPayload.getE()));
    }
}
