package io.coodoo.workhorse.core.control;

import java.time.ZoneId;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.coodoo.workhorse.core.boundary.WorkhorseLogService;
import io.coodoo.workhorse.core.entity.WorkhorseConfig;
import io.coodoo.workhorse.persistence.interfaces.ConfigPersistence;
import io.coodoo.workhorse.persistence.interfaces.qualifier.ConfigQualifier;

/**
 * @author coodoo GmbH (coodoo.io)
 */
@ApplicationScoped
public class WorkhorseConfigController {

    private static final Logger log = LoggerFactory.getLogger(WorkhorseConfigController.class);

    @Inject
    @ConfigQualifier
    ConfigPersistence configPersistence;

    @Inject
    WorkhorseLogService workhorseLogService;

    @Inject
    Workhorse workhorse;

    /**
     * Get the WorkhorseConfig
     * 
     * @return WorkhorseConfig
     */
    public WorkhorseConfig getWorkhorseConfig() {

        WorkhorseConfig workhorseConfig = configPersistence.get();

        if (workhorseConfig == null) {
            workhorseConfig = new WorkhorseConfig();

            configPersistence.update(workhorseConfig);

            log.info(" Created: {}", workhorseConfig);
        }
        return workhorseConfig;
    }

    /**
     * Update the configuration of the job engine
     * 
     * @param newWorkhorseConfig New configuration to set
     * @return WorkhorseConfig
     */
    public WorkhorseConfig updateWorkhorseConfig(WorkhorseConfig newWorkhorseConfig) {

        WorkhorseConfig workhorseConfig = getWorkhorseConfig();

        updateBufferPollInterval(workhorseConfig, newWorkhorseConfig.getBufferPollInterval());
        updateBufferPushFallbackPollInterval(workhorseConfig, newWorkhorseConfig.getBufferPushFallbackPollInterval());
        updateBufferMax(workhorseConfig, newWorkhorseConfig.getBufferMax());
        updateBufferMin(workhorseConfig, newWorkhorseConfig.getBufferMin());
        updateLogChange(workhorseConfig, newWorkhorseConfig.getLogChange());
        updateLogTimeFormatter(workhorseConfig, newWorkhorseConfig.getLogTimeFormat());
        updateTimeZone(workhorseConfig, newWorkhorseConfig.getTimeZone());
        updateLogInfoMarker(workhorseConfig, newWorkhorseConfig.getLogInfoMarker());
        updateLogWarnMarker(workhorseConfig, newWorkhorseConfig.getLogWarnMarker());
        updateLogErrorMarker(workhorseConfig, newWorkhorseConfig.getLogErrorMarker());

        configPersistence.update(workhorseConfig);
        log.info("Updated: {}", workhorseConfig);
        return workhorseConfig;
    }

    /**
     * Initialize the static config object used intern by Workhorse
     * 
     * @return WorkhorseConfig
     */
    public WorkhorseConfig initializeStaticConfig() {

        WorkhorseConfig workhorseConfig = getWorkhorseConfig();

        StaticConfig.TIME_ZONE = workhorseConfig.getTimeZone();
        StaticConfig.BUFFER_MAX = workhorseConfig.getBufferMax();
        StaticConfig.BUFFER_MIN = workhorseConfig.getBufferMin();
        StaticConfig.BUFFER_POLL_INTERVAL = workhorseConfig.getBufferPollInterval();
        StaticConfig.BUFFER_PUSH_FALL_BACK_POLL_INTERVAL = workhorseConfig.getBufferPushFallbackPollInterval();
        StaticConfig.EXECUTION_TIMEOUT = workhorseConfig.getExecutionTimeout();
        StaticConfig.EXECUTION_TIMEOUT_STATUS = workhorseConfig.getExecutionTimeoutStatus();
        StaticConfig.LOG_CHANGE = workhorseConfig.getLogChange();
        StaticConfig.LOG_TIME_FORMATTER = workhorseConfig.getLogTimeFormat();
        StaticConfig.LOG_INFO_MARKER = workhorseConfig.getLogInfoMarker();
        StaticConfig.LOG_WARN_MARKER = workhorseConfig.getLogWarnMarker();
        StaticConfig.LOG_ERROR_MARKER = workhorseConfig.getLogErrorMarker();

        log.info("Initialized: {}", workhorseConfig);
        workhorseLogService.logMessage("Initial config set: " + workhorseConfig, null, false);
        return workhorseConfig;
    }

    protected void updateBufferPollInterval(WorkhorseConfig workhorseConfig, int bufferPollInterval) {

        if (bufferPollInterval < 1 || bufferPollInterval > 60) {
            throw new RuntimeException("The buffer poller interval must be between 1 and 60!");
        }
        if (workhorseConfig.getBufferPollInterval() != bufferPollInterval) {

            StaticConfig.BUFFER_POLL_INTERVAL = bufferPollInterval;
            workhorseLogService.logChange(null, null, "Buffer poller interval", workhorseConfig.getBufferPollInterval(),
                    bufferPollInterval, null);
            workhorseConfig.setBufferPollInterval(bufferPollInterval);

            if (workhorse.isRunning()) {
                workhorse.start();
            }
        }
    }

    protected void updateBufferPushFallbackPollInterval(WorkhorseConfig workhorseConfig,
            int bufferPushFallbackPollInterval) {

        if (bufferPushFallbackPollInterval < 1) {
            throw new RuntimeException("The buffer push fallback poller interval must be higher than 0!");
        }

        if (workhorseConfig.getBufferPushFallbackPollInterval() != bufferPushFallbackPollInterval) {

            StaticConfig.BUFFER_PUSH_FALL_BACK_POLL_INTERVAL = bufferPushFallbackPollInterval;
            workhorseLogService.logChange(null, null, "Buffer PusherPoll interval",
                    workhorseConfig.getBufferPushFallbackPollInterval(), bufferPushFallbackPollInterval, null);
            workhorseConfig.setBufferPushFallbackPollInterval(bufferPushFallbackPollInterval);

            if (workhorse.isRunning()) {
                workhorse.start();
            }
        }

    }

    protected void updateBufferMax(WorkhorseConfig workhorseConfig, Long bufferMax) {

        if (bufferMax < 1) {
            throw new RuntimeException(
                    "The max amount of executions to load into the memory buffer per job must be higher than 0!");
        }
        if (workhorseConfig.getBufferMax() != bufferMax) {

            StaticConfig.BUFFER_MAX = bufferMax;
            workhorseLogService.logChange(null, null, "Max amount of executions to load into the memory buffer per job",
                    workhorseConfig.getBufferMax(), bufferMax, null);
            workhorseConfig.setBufferMax(bufferMax);
        }
    }

    protected void updateBufferMin(WorkhorseConfig workhorseConfig, int bufferMin) {

        if (bufferMin < 1) {
            throw new RuntimeException(
                    "The min amount of executions in memory buffer before the poller gets to add more must be higher than 0!");
        }
        if (workhorseConfig.getBufferMin() != bufferMin) {

            StaticConfig.BUFFER_MIN = bufferMin;
            workhorseLogService.logChange(null, null,
                    "Min amount of executions in memory buffer before the poller gets to add more",
                    workhorseConfig.getBufferMin(), bufferMin, null);
            workhorseConfig.setBufferMin(bufferMin);
        }
    }

    protected void updateLogChange(WorkhorseConfig workhorseConfig, String logChange) {

        if (logChange == null) {
            throw new RuntimeException("The log change pattern is needed!");
        }
        if (("_" + logChange + "_").split("%s", -1).length - 1 != 3) {
            throw new RuntimeException("The log change pattern needs the placeholder '%s' three times!");
        }
        if (!Objects.equals(workhorseConfig.getLogChange(), logChange)) {

            StaticConfig.LOG_CHANGE = logChange;
            workhorseLogService.logChange(null, null, "Log change pattern", workhorseConfig.getLogChange(), logChange,
                    null);
            workhorseConfig.setLogChange(logChange);
        }
    }

    protected void updateLogTimeFormatter(WorkhorseConfig workhorseConfig, String logTimeFormatter) {

        if (logTimeFormatter == null) {
            throw new RuntimeException("The execution log timestamp pattern is needed!");
        }
        if (!Objects.equals(workhorseConfig.getLogTimeFormat(), logTimeFormatter)) {

            StaticConfig.LOG_TIME_FORMATTER = logTimeFormatter;
            workhorseLogService.logChange(null, null, "Execution log timestamp pattern",
                    workhorseConfig.getLogTimeFormat(), logTimeFormatter, null);
            workhorseConfig.setLogTimeFormat(logTimeFormatter);
        }
    }

    protected void updateTimeZone(WorkhorseConfig workhorseConfig, String timeZone) {

        if (timeZone != null && !ZoneId.getAvailableZoneIds().contains(timeZone)) {
            throw new RuntimeException("Time zone '" + timeZone + "' is not available!");
        }
        if (!Objects.equals(workhorseConfig.getTimeZone(), timeZone)) {
            ZoneId systemDefault = ZoneId.systemDefault();
            if (timeZone == null || systemDefault.getId().equals(timeZone)) {

                StaticConfig.TIME_ZONE = systemDefault.getId();
                workhorseLogService.logChange(null, null, "Time zone", workhorseConfig.getTimeZone(),
                        systemDefault.getId(), "System default time-zone is used: " + systemDefault);
                workhorseConfig.setTimeZone(systemDefault.getId());
            } else {

                StaticConfig.TIME_ZONE = timeZone;
                workhorseLogService.logChange(null, null, "Time zone", workhorseConfig.getTimeZone(), timeZone, null);
                workhorseConfig.setTimeZone(timeZone);
            }
        }
    }

    protected void updateLogInfoMarker(WorkhorseConfig workhorseConfig, String logInfoMarker) {

        if (!Objects.equals(workhorseConfig.getLogInfoMarker(), logInfoMarker)) {

            StaticConfig.LOG_INFO_MARKER = logInfoMarker;
            workhorseLogService.logChange(null, null, "Execution log info marker", workhorseConfig.getLogInfoMarker(),
                    logInfoMarker, null);
            workhorseConfig.setLogInfoMarker(logInfoMarker);
        }
    }

    protected void updateLogWarnMarker(WorkhorseConfig workhorseConfig, String logWarnMarker) {

        if (!Objects.equals(workhorseConfig.getLogWarnMarker(), logWarnMarker)) {

            StaticConfig.LOG_WARN_MARKER = logWarnMarker;
            workhorseLogService.logChange(null, null, "Execution log warn marker", workhorseConfig.getLogWarnMarker(),
                    logWarnMarker, null);
            workhorseConfig.setLogWarnMarker(logWarnMarker);
        }
    }

    protected void updateLogErrorMarker(WorkhorseConfig workhorseConfig, String logErrorMarker) {

        if (!Objects.equals(workhorseConfig.getLogErrorMarker(), logErrorMarker)) {

            StaticConfig.LOG_ERROR_MARKER = logErrorMarker;
            workhorseLogService.logChange(null, null, "Execution log error marker", workhorseConfig.getLogErrorMarker(),
                    logErrorMarker, null);
            workhorseConfig.setLogErrorMarker(logErrorMarker);
        }
    }

}
