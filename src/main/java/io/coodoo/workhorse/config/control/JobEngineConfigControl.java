package io.coodoo.workhorse.config.control;

import java.time.ZoneId;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.coodoo.workhorse.config.entity.JobEngineConfig;
import io.coodoo.workhorse.jobengine.control.events.RestartTheJobEngine;
import io.coodoo.workhorse.log.control.JobEngineLogControl;
import io.coodoo.workhorse.persistence.interfaces.JobEngineConfigPersistence;
import io.coodoo.workhorse.persistence.interfaces.PersistenceTyp;
import io.coodoo.workhorse.persistence.interfaces.qualifiers.JobEngineConfigDAO;

@ApplicationScoped
public class JobEngineConfigControl {

    private static final Logger log = Logger.getLogger(JobEngineConfigControl.class);

    @Inject
    @JobEngineConfigDAO
    JobEngineConfigPersistence jobEngineConfigPersistence;

    @Inject
    JobEngineLogControl jobEngineLogControl;

    @Inject
    JobEngineConfig jobEngineConfig;

    @Inject
    Event<RestartTheJobEngine> restartEvent;

    public JobEngineConfig getJobEngineConfig() {
        JobEngineConfig config = jobEngineConfigPersistence.get();

        if (config == null) {
            config = new JobEngineConfig();
        }

        return config;
    }

    public JobEngineConfig updateJobEngineConfig(JobEngineConfig jobEngineConfig) {

        if ( jobEngineConfig != null ) {
          return  jobEngineConfigPersistence.update(jobEngineConfig);
        }

        return null;
    }


    public void updateJobQueuePollerInterval(int jobQueuePollerInterval) {

        if (jobQueuePollerInterval < 1 || jobQueuePollerInterval > 60) {
            throw new RuntimeException("The job queue poller interval must be between 1 and 60!");
        }
        if (jobEngineConfig.getJobQueuePollerInterval() != jobQueuePollerInterval) {

            jobEngineLogControl.logChange(null, null, "Job queue poller interval",
                    jobEngineConfig.getJobQueuePollerInterval(), jobQueuePollerInterval, null);
            jobEngineConfig.setJobQueuePollerInterval(jobQueuePollerInterval);
        }
    }

    public void updateJobQueuePusherPoll(int jobQueuePusherPoll) {

        if (jobEngineConfig.getJobQueuePusherPoll() != jobQueuePusherPoll) {

            jobEngineLogControl.logChange(null, null, "Job queue PusherPoll interval",
                    jobEngineConfig.getJobQueuePusherPoll(), jobQueuePusherPoll, null);
            jobEngineConfig.setJobQueuePusherPoll(jobQueuePusherPoll);
        }

    }

    public void updatePersistenceTyp(PersistenceTyp persistenceTyp) {
        if (persistenceTyp == null) {
            throw new RuntimeException("The persistenceTyp can't be null");
        }
        if (!jobEngineConfig.getPersistenceTyp().equals(persistenceTyp)) {

            jobEngineLogControl.logChange(null, null, "Persistence typ",
                    jobEngineConfig.getPersistenceTyp(), persistenceTyp, null);
            jobEngineConfig.setPersistenceTyp(persistenceTyp);
            log.info(" The persistence have been changed. The event will be propagate ");

            restartEvent.fireAsync(new RestartTheJobEngine(null, jobEngineConfig));
        }

    }

    public void updateJobQueueMax(Long jobQueueMax) {

        if (jobQueueMax < 1) {
            throw new RuntimeException(
                    "The max amount of executions to load into the memory queue per job must be higher than 0!");
        }
        if (jobEngineConfig.getJobQueueMax() != jobQueueMax) {

            jobEngineLogControl.logChange(null, null, "Max amount of executions to load into the memory queue per job",
                    jobEngineConfig.getJobQueueMax(), jobQueueMax, null);
            jobEngineConfig.setJobQueueMax(jobQueueMax);
        }
    }

    public void updateJobQueueMin(int jobQueueMin) {

        if (jobQueueMin < 1) {
            throw new RuntimeException(
                    "The min amount of executions in memory queue before the poller gets to add more must be higher than 0!");
        }
        if (jobEngineConfig.getJobQueueMin() != jobQueueMin) {

            jobEngineLogControl.logChange(null, null,
                    "Min amount of executions in memory queue before the poller gets to add more",
                    jobEngineConfig.getJobQueueMin(), jobQueueMin, null);
            jobEngineConfig.setJobQueueMin(jobQueueMin);
        }
    }

    public void updateLogChange(String logChange) {

        if (logChange == null) {
            throw new RuntimeException("The log change pattern is needed!");
        }
        if (("_" + logChange + "_").split("%s", -1).length - 1 != 3) {
            throw new RuntimeException("The log change pattern needs the placeholder '%s' three times!");
        }
        if (!Objects.equals(jobEngineConfig.getLogChange(), logChange)) {

            jobEngineLogControl.logChange(null, null, "Log change pattern", jobEngineConfig.getLogChange(), logChange,
                    null);
            jobEngineConfig.setLogChange(logChange);
        }
    }

    public void updateLogTimeFormatter(String logTimeFormatter) {
        if (logTimeFormatter == null) {
            throw new RuntimeException("The execution log timestamp pattern is needed!");
        }
        if (!Objects.equals(jobEngineConfig.getLogTimeFormatter(), logTimeFormatter)) {

            jobEngineLogControl.logChange(null, null, "Execution log timestamp pattern",
                    jobEngineConfig.getLogTimeFormatter(), logTimeFormatter, null);
            jobEngineConfig.setLogTimeFormatter(logTimeFormatter);
        }
    }

    public void updateTimeZone(String timeZone) {

        if (timeZone != null && !ZoneId.getAvailableZoneIds().contains(timeZone)) {
            throw new RuntimeException("Time zone '" + timeZone + "' is not available!");
        }
        if (!Objects.equals(jobEngineConfig.getTimeZone(), timeZone)) {
            ZoneId systemDefault = ZoneId.systemDefault();
            if (timeZone == null || systemDefault.getId().equals(timeZone)) {

                jobEngineLogControl.logChange(null, null, "Time zone", jobEngineConfig.getTimeZone(),
                        systemDefault.getId(), "System default time-zone is used: " + systemDefault);
                jobEngineConfig.setTimeZone(systemDefault.getId());
            } else {

                jobEngineLogControl.logChange(null, null, "Time zone", jobEngineConfig.getTimeZone(), timeZone, null);
                jobEngineConfig.setTimeZone(timeZone);
            }
            
        }
    }

    public void updateLogInfoMarker(String logInfoMarker) {
        if (!Objects.equals(jobEngineConfig.getLogInfoMarker(), logInfoMarker)) {

            jobEngineLogControl.logChange(null, null, "Execution log info marker", jobEngineConfig.getLogInfoMarker(),
                    logInfoMarker, null);
            jobEngineConfig.setLogInfoMarker(logInfoMarker);
        }
    }

    public void updateLogWarnMarker(String logWarnMarker) {
        if (!Objects.equals(jobEngineConfig.getLogWarnMarker(), logWarnMarker)) {

            jobEngineLogControl.logChange(null, null, "Execution log warn marker", jobEngineConfig.getLogWarnMarker(),
                    logWarnMarker, null);
            jobEngineConfig.setLogWarnMarker(logWarnMarker);
        }
    }

    public void updateLogErrorMarker(String logErrorMarker) {
        if (!Objects.equals(jobEngineConfig.getLogErrorMarker(), logErrorMarker)) {

            jobEngineLogControl.logChange(null, null, "Execution log error marker", jobEngineConfig.getLogErrorMarker(),
                    logErrorMarker, null);
            jobEngineConfig.setLogErrorMarker(logErrorMarker);
        }
    }

}
