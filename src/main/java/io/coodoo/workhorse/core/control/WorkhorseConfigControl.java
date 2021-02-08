package io.coodoo.workhorse.core.control;

import java.time.ZoneId;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.coodoo.workhorse.core.boundary.Config;
import io.coodoo.workhorse.core.boundary.WorkhorseLogService;
import io.coodoo.workhorse.core.entity.WorkhorseConfig;
import io.coodoo.workhorse.persistence.interfaces.ConfigPersistence;
import io.coodoo.workhorse.persistence.interfaces.qualifier.ConfigQualifier;

@ApplicationScoped
public class WorkhorseConfigControl {

    private static final Logger log = LoggerFactory.getLogger(WorkhorseConfigControl.class);

    @Inject
    @ConfigQualifier
    ConfigPersistence configPersistence;

    @Inject
    WorkhorseLogService workhorseLogService;

    @Inject
    Workhorse workhorse;

    public WorkhorseConfig getWorkhorseConfig() {

        WorkhorseConfig workhorseConfig = configPersistence.get();

        if (workhorseConfig == null) {
            workhorseConfig = new WorkhorseConfig();

            configPersistence.update(workhorseConfig);

            log.info(" Created: {}", workhorseConfig);
            workhorseLogService.logMessage("Initial config set: " + workhorseConfig, null, false);
        }
        return workhorseConfig;
    }

    public WorkhorseConfig initializeStaticConfig() {

        WorkhorseConfig workhorseConfig = getWorkhorseConfig();

        Config.TIME_ZONE = workhorseConfig.getTimeZone();
        Config.BUFFER_MAX = workhorseConfig.getJobQueueMax();
        Config.BUFFER_MIN = workhorseConfig.getJobQueueMin();
        Config.BUFFER_POLL_INTERVAL = workhorseConfig.getJobQueuePusherPoll();
        Config.BUFFER_PUSH_FALL_BACK_POLL_INTERVAL = workhorseConfig.getJobQueuePollerInterval();
        Config.EXECUTION_TIMEOUT = workhorseConfig.getZombieRecognitionTime();
        Config.EXECUTION_TIMEOUT_STATUS = workhorseConfig.getZombieCureStatus();
        Config.LOG_CHANGE = workhorseConfig.getLogChange();
        Config.LOG_TIME_FORMATTER = workhorseConfig.getLogTimeFormat();
        Config.LOG_INFO_MARKER = workhorseConfig.getLogInfoMarker();
        Config.LOG_WARN_MARKER = workhorseConfig.getLogWarnMarker();
        Config.LOG_ERROR_MARKER = workhorseConfig.getLogErrorMarker();

        log.info("Initialized: {}", workhorseConfig);
        return workhorseConfig;
    }

    /**
     * 
     * @param newWorkhorseConfig
     */
    public void initialize(WorkhorseConfig newWorkhorseConfig) {

        initializeStaticConfig();
        updateWorkhorseConfig(newWorkhorseConfig);
    }

    /**
     * Update the configuration of the job engine
     * 
     * @param newWorkhorseConfig
     * @return
     */
    public WorkhorseConfig updateWorkhorseConfig(WorkhorseConfig newWorkhorseConfig) {

        WorkhorseConfig workhorseConfig = getWorkhorseConfig();

        updateJobQueuePollerInterval(workhorseConfig, newWorkhorseConfig.getJobQueuePollerInterval());
        updateJobQueuePusherPoll(workhorseConfig, newWorkhorseConfig.getJobQueuePusherPoll());
        updateJobQueueMax(workhorseConfig, newWorkhorseConfig.getJobQueueMax());
        updateJobQueueMin(workhorseConfig, newWorkhorseConfig.getJobQueueMin());
        updateLogChange(workhorseConfig, newWorkhorseConfig.getLogChange());
        updateLogTimeFormatter(workhorseConfig, newWorkhorseConfig.getLogTimeFormat());
        updateTimeZone(workhorseConfig, newWorkhorseConfig.getTimeZone());
        updateLogInfoMarker(workhorseConfig, newWorkhorseConfig.getLogInfoMarker());
        updateLogWarnMarker(workhorseConfig, newWorkhorseConfig.getLogWarnMarker());
        updateLogErrorMarker(workhorseConfig, newWorkhorseConfig.getLogErrorMarker());

        configPersistence.update(newWorkhorseConfig);
        log.info("Updated: {}", newWorkhorseConfig);
        return newWorkhorseConfig;
    }

    public void update(WorkhorseConfig workhorseConfig) {

        configPersistence.update(workhorseConfig);
    }

    public void updateJobQueuePollerInterval(WorkhorseConfig workhorseConfig, int jobQueuePollerInterval) {

        if (jobQueuePollerInterval < 1 || jobQueuePollerInterval > 60) {
            throw new RuntimeException("The job queue poller interval must be between 1 and 60!");
        }
        if (workhorseConfig.getJobQueuePollerInterval() != jobQueuePollerInterval) {

            Config.BUFFER_POLL_INTERVAL = jobQueuePollerInterval;
            workhorseLogService.logChange(null, null, "Job queue poller interval",
                    workhorseConfig.getJobQueuePollerInterval(), jobQueuePollerInterval, null);
            workhorseConfig.setJobQueuePollerInterval(jobQueuePollerInterval);

            if (workhorse.isRunning()) {
                workhorse.start();
            }

        }
    }

    public void updateJobQueuePusherPoll(WorkhorseConfig workhorseConfig, int jobQueuePusherPoll) {

        if (workhorseConfig.getJobQueuePusherPoll() != jobQueuePusherPoll) {

            Config.BUFFER_PUSH_FALL_BACK_POLL_INTERVAL = jobQueuePusherPoll;
            workhorseLogService.logChange(null, null, "Job queue PusherPoll interval",
                    workhorseConfig.getJobQueuePusherPoll(), jobQueuePusherPoll, null);
            workhorseConfig.setJobQueuePusherPoll(jobQueuePusherPoll);

            if (workhorse.isRunning()) {
                workhorse.start();
            }
        }

    }

    public void updateJobQueueMax(WorkhorseConfig workhorseConfig, Long jobQueueMax) {

        if (jobQueueMax < 1) {
            throw new RuntimeException(
                    "The max amount of executions to load into the memory queue per job must be higher than 0!");
        }
        if (workhorseConfig.getJobQueueMax() != jobQueueMax) {

            Config.BUFFER_MAX = jobQueueMax;
            workhorseLogService.logChange(null, null, "Max amount of executions to load into the memory queue per job",
                    workhorseConfig.getJobQueueMax(), jobQueueMax, null);
            workhorseConfig.setJobQueueMax(jobQueueMax);
        }
    }

    public void updateJobQueueMin(WorkhorseConfig workhorseConfig, int jobQueueMin) {

        if (jobQueueMin < 1) {
            throw new RuntimeException(
                    "The min amount of executions in memory queue before the poller gets to add more must be higher than 0!");
        }
        if (workhorseConfig.getJobQueueMin() != jobQueueMin) {

            Config.BUFFER_MIN = jobQueueMin;
            workhorseLogService.logChange(null, null,
                    "Min amount of executions in memory queue before the poller gets to add more",
                    workhorseConfig.getJobQueueMin(), jobQueueMin, null);
            workhorseConfig.setJobQueueMin(jobQueueMin);
        }
    }

    public void updateLogChange(WorkhorseConfig workhorseConfig, String logChange) {

        if (logChange == null) {
            throw new RuntimeException("The log change pattern is needed!");
        }
        if (("_" + logChange + "_").split("%s", -1).length - 1 != 3) {
            throw new RuntimeException("The log change pattern needs the placeholder '%s' three times!");
        }
        if (!Objects.equals(workhorseConfig.getLogChange(), logChange)) {

            Config.LOG_CHANGE = logChange;
            workhorseLogService.logChange(null, null, "Log change pattern", workhorseConfig.getLogChange(), logChange,
                    null);
            workhorseConfig.setLogChange(logChange);
        }
    }

    public void updateLogTimeFormatter(WorkhorseConfig workhorseConfig, String logTimeFormatter) {
        if (logTimeFormatter == null) {
            throw new RuntimeException("The execution log timestamp pattern is needed!");
        }
        if (!Objects.equals(workhorseConfig.getLogTimeFormat(), logTimeFormatter)) {

            Config.LOG_TIME_FORMATTER = logTimeFormatter;
            workhorseLogService.logChange(null, null, "Execution log timestamp pattern",
                    workhorseConfig.getLogTimeFormat(), logTimeFormatter, null);
            workhorseConfig.setLogTimeFormat(logTimeFormatter);
        }
    }

    public void updateTimeZone(WorkhorseConfig workhorseConfig, String timeZone) {

        if (timeZone != null && !ZoneId.getAvailableZoneIds().contains(timeZone)) {
            throw new RuntimeException("Time zone '" + timeZone + "' is not available!");
        }
        if (!Objects.equals(workhorseConfig.getTimeZone(), timeZone)) {
            ZoneId systemDefault = ZoneId.systemDefault();
            if (timeZone == null || systemDefault.getId().equals(timeZone)) {

                Config.TIME_ZONE = systemDefault.getId();
                workhorseLogService.logChange(null, null, "Time zone", workhorseConfig.getTimeZone(),
                        systemDefault.getId(), "System default time-zone is used: " + systemDefault);
                workhorseConfig.setTimeZone(systemDefault.getId());
            } else {

                Config.TIME_ZONE = timeZone;
                workhorseLogService.logChange(null, null, "Time zone", workhorseConfig.getTimeZone(), timeZone, null);
                workhorseConfig.setTimeZone(timeZone);
            }

        }
    }

    public void updateLogInfoMarker(WorkhorseConfig workhorseConfig, String logInfoMarker) {
        if (!Objects.equals(workhorseConfig.getLogInfoMarker(), logInfoMarker)) {

            Config.LOG_INFO_MARKER = logInfoMarker;
            workhorseLogService.logChange(null, null, "Execution log info marker", workhorseConfig.getLogInfoMarker(),
                    logInfoMarker, null);
            workhorseConfig.setLogInfoMarker(logInfoMarker);
        }
    }

    public void updateLogWarnMarker(WorkhorseConfig workhorseConfig, String logWarnMarker) {
        if (!Objects.equals(workhorseConfig.getLogWarnMarker(), logWarnMarker)) {

            Config.LOG_WARN_MARKER = logWarnMarker;
            workhorseLogService.logChange(null, null, "Execution log warn marker", workhorseConfig.getLogWarnMarker(),
                    logWarnMarker, null);
            workhorseConfig.setLogWarnMarker(logWarnMarker);
        }
    }

    public void updateLogErrorMarker(WorkhorseConfig workhorseConfig, String logErrorMarker) {
        if (!Objects.equals(workhorseConfig.getLogErrorMarker(), logErrorMarker)) {

            Config.LOG_ERROR_MARKER = logErrorMarker;
            workhorseLogService.logChange(null, null, "Execution log error marker", workhorseConfig.getLogErrorMarker(),
                    logErrorMarker, null);
            workhorseConfig.setLogErrorMarker(logErrorMarker);
        }
    }

}
