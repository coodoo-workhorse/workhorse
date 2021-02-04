package io.coodoo.workhorse.core.control;

import java.time.ZoneId;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.coodoo.workhorse.core.boundary.WorkhorseLogService;
import io.coodoo.workhorse.core.control.event.RestartWorkhorseEvent;
import io.coodoo.workhorse.core.entity.WorkhorseConfig;
import io.coodoo.workhorse.persistence.interfaces.ConfigPersistence;
import io.coodoo.workhorse.persistence.interfaces.PersistenceTyp;
import io.coodoo.workhorse.persistence.interfaces.qualifier.ConfigQualifier;

@ApplicationScoped
public class WorkhorseConfigControl {

    private static final Logger log = Logger.getLogger(WorkhorseConfigControl.class);

    @Inject
    @ConfigQualifier
    ConfigPersistence configPersistence;

    @Inject
    WorkhorseLogService workhorseLogService;

    @Inject
    WorkhorseConfig workhorseConfig;

    @Inject
    Event<RestartWorkhorseEvent> restartEvent;

    public WorkhorseConfig getWorkhorseConfig() {
        WorkhorseConfig config = configPersistence.get();

        if (config == null) {
            config = new WorkhorseConfig();
        }
        return config;
    }

    public WorkhorseConfig updateWorkhorseConfig(WorkhorseConfig workhorseConfig) {
        if (workhorseConfig != null) {
            return configPersistence.update(workhorseConfig);
        }
        return null;
    }

    public void updateJobQueuePollerInterval(int jobQueuePollerInterval) {

        if (jobQueuePollerInterval < 1 || jobQueuePollerInterval > 60) {
            throw new RuntimeException("The job queue poller interval must be between 1 and 60!");
        }
        if (workhorseConfig.getJobQueuePollerInterval() != jobQueuePollerInterval) {

            workhorseLogService.logChange(null, null, "Job queue poller interval", workhorseConfig.getJobQueuePollerInterval(), jobQueuePollerInterval, null);
            workhorseConfig.setJobQueuePollerInterval(jobQueuePollerInterval);
        }
    }

    public void updateJobQueuePusherPoll(int jobQueuePusherPoll) {

        if (workhorseConfig.getJobQueuePusherPoll() != jobQueuePusherPoll) {

            workhorseLogService.logChange(null, null, "Job queue PusherPoll interval", workhorseConfig.getJobQueuePusherPoll(), jobQueuePusherPoll, null);
            workhorseConfig.setJobQueuePusherPoll(jobQueuePusherPoll);
        }

    }

    public void updatePersistenceTyp(PersistenceTyp persistenceTyp) {
        if (persistenceTyp == null) {
            throw new RuntimeException("The persistenceTyp can't be null");
        }
        if (!workhorseConfig.getPersistenceTyp().equals(persistenceTyp)) {

            workhorseLogService.logChange(null, null, "Persistence typ", workhorseConfig.getPersistenceTyp(), persistenceTyp, null);
            workhorseConfig.setPersistenceTyp(persistenceTyp);
            log.info(" The persistence have been changed. The event will be propagate ");

            restartEvent.fireAsync(new RestartWorkhorseEvent(null, workhorseConfig));
        }

    }

    public void updateJobQueueMax(Long jobQueueMax) {

        if (jobQueueMax < 1) {
            throw new RuntimeException("The max amount of executions to load into the memory queue per job must be higher than 0!");
        }
        if (workhorseConfig.getJobQueueMax() != jobQueueMax) {

            workhorseLogService.logChange(null, null, "Max amount of executions to load into the memory queue per job", workhorseConfig.getJobQueueMax(),
                            jobQueueMax, null);
            workhorseConfig.setJobQueueMax(jobQueueMax);
        }
    }

    public void updateJobQueueMin(int jobQueueMin) {

        if (jobQueueMin < 1) {
            throw new RuntimeException("The min amount of executions in memory queue before the poller gets to add more must be higher than 0!");
        }
        if (workhorseConfig.getJobQueueMin() != jobQueueMin) {

            workhorseLogService.logChange(null, null, "Min amount of executions in memory queue before the poller gets to add more",
                            workhorseConfig.getJobQueueMin(), jobQueueMin, null);
            workhorseConfig.setJobQueueMin(jobQueueMin);
        }
    }

    public void updateLogChange(String logChange) {

        if (logChange == null) {
            throw new RuntimeException("The log change pattern is needed!");
        }
        if (("_" + logChange + "_").split("%s", -1).length - 1 != 3) {
            throw new RuntimeException("The log change pattern needs the placeholder '%s' three times!");
        }
        if (!Objects.equals(workhorseConfig.getLogChange(), logChange)) {

            workhorseLogService.logChange(null, null, "Log change pattern", workhorseConfig.getLogChange(), logChange, null);
            workhorseConfig.setLogChange(logChange);
        }
    }

    public void updateLogTimeFormatter(String logTimeFormatter) {
        if (logTimeFormatter == null) {
            throw new RuntimeException("The execution log timestamp pattern is needed!");
        }
        if (!Objects.equals(workhorseConfig.getLogTimeFormat(), logTimeFormatter)) {

            workhorseLogService.logChange(null, null, "Execution log timestamp pattern", workhorseConfig.getLogTimeFormat(), logTimeFormatter, null);
            workhorseConfig.setLogTimeFormat(logTimeFormatter);
        }
    }

    public void updateTimeZone(String timeZone) {

        if (timeZone != null && !ZoneId.getAvailableZoneIds().contains(timeZone)) {
            throw new RuntimeException("Time zone '" + timeZone + "' is not available!");
        }
        if (!Objects.equals(workhorseConfig.getTimeZone(), timeZone)) {
            ZoneId systemDefault = ZoneId.systemDefault();
            if (timeZone == null || systemDefault.getId().equals(timeZone)) {

                workhorseLogService.logChange(null, null, "Time zone", workhorseConfig.getTimeZone(), systemDefault.getId(),
                                "System default time-zone is used: " + systemDefault);
                workhorseConfig.setTimeZone(systemDefault.getId());
            } else {

                workhorseLogService.logChange(null, null, "Time zone", workhorseConfig.getTimeZone(), timeZone, null);
                workhorseConfig.setTimeZone(timeZone);
            }

        }
    }

    public void updateLogInfoMarker(String logInfoMarker) {
        if (!Objects.equals(workhorseConfig.getLogInfoMarker(), logInfoMarker)) {

            workhorseLogService.logChange(null, null, "Execution log info marker", workhorseConfig.getLogInfoMarker(), logInfoMarker, null);
            workhorseConfig.setLogInfoMarker(logInfoMarker);
        }
    }

    public void updateLogWarnMarker(String logWarnMarker) {
        if (!Objects.equals(workhorseConfig.getLogWarnMarker(), logWarnMarker)) {

            workhorseLogService.logChange(null, null, "Execution log warn marker", workhorseConfig.getLogWarnMarker(), logWarnMarker, null);
            workhorseConfig.setLogWarnMarker(logWarnMarker);
        }
    }

    public void updateLogErrorMarker(String logErrorMarker) {
        if (!Objects.equals(workhorseConfig.getLogErrorMarker(), logErrorMarker)) {

            workhorseLogService.logChange(null, null, "Execution log error marker", workhorseConfig.getLogErrorMarker(), logErrorMarker, null);
            workhorseConfig.setLogErrorMarker(logErrorMarker);
        }
    }

}
