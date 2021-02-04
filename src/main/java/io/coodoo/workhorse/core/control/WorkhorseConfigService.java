package io.coodoo.workhorse.core.control;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import io.coodoo.workhorse.core.control.event.RestartWorkhorseEvent;
import io.coodoo.workhorse.core.entity.WorkhorseConfig;
import io.coodoo.workhorse.persistence.interfaces.PersistenceTyp;

// TODO alle methoden in controll migrien, prüfen ob noch notwendig
@ApplicationScoped
@Deprecated
public class WorkhorseConfigService {

    @Inject
    WorkhorseConfigControl workhorseConfigControl;

    @Inject
    WorkhorseConfig workhorseConfig;

    @Inject
    Event<RestartWorkhorseEvent> restartEvent;

    /**
     * Initialize the config of the job engine
     * 
     * @param persistenceTyp
     */
    public void initializeConfig() {

        WorkhorseConfig config = workhorseConfigControl.getWorkhorseConfig();

        workhorseConfig.setJobQueueMax(config.getJobQueueMax());
        workhorseConfig.setJobQueueMin(config.getJobQueueMin());
        workhorseConfig.setJobQueuePollerInterval(config.getJobQueuePollerInterval());
        workhorseConfig.setJobQueuePusherPoll(config.getJobQueuePusherPoll());
        workhorseConfig.setTimeZone(config.getTimeZone());
        workhorseConfig.setLogChange(config.getLogChange());
        workhorseConfig.setPersistenceTyp(config.getPersistenceTyp());
        workhorseConfig.setLogWarnMarker(config.getLogWarnMarker());
        workhorseConfig.setLogInfoMarker(config.getLogInfoMarker());
        workhorseConfig.setLogTimeFormat(config.getLogTimeFormat());
        workhorseConfig.setZombieCureStatus(config.getZombieCureStatus());
        workhorseConfig.setZombieRecognitionTime(config.getZombieRecognitionTime());

        workhorseConfigControl.updateWorkhorseConfig(workhorseConfig);

    }

    /**
     * initialize the config of the job engine with given parameter
     * 
     * @param timeZone
     * @param jobQueuePollerInterval
     * @param jobQueuePusherPoll
     * @param jobQueueMax
     * @param jobQueueMin
     * @param persistenceTyp
     */
    public void initializeConfig(String timeZone, int jobQueuePollerInterval, int jobQueuePusherPoll, Long jobQueueMax, int jobQueueMin,
                    PersistenceTyp persistenceTyp) {

        WorkhorseConfig newWorkhorseConfig = workhorseConfigControl.getWorkhorseConfig();

        if (jobQueueMax > 0L) {
            workhorseConfig.setJobQueueMax(jobQueueMax);
        } else {
            workhorseConfig.setJobQueueMax(newWorkhorseConfig.getJobQueueMax());
        }

        if (jobQueueMin > 0) {
            workhorseConfig.setJobQueueMin(jobQueueMin);

        } else {
            workhorseConfig.setJobQueueMin(newWorkhorseConfig.getJobQueueMin());
        }

        if (jobQueuePollerInterval > 0) {
            workhorseConfig.setJobQueuePollerInterval(jobQueuePollerInterval);

        } else {
            workhorseConfig.setJobQueuePollerInterval(newWorkhorseConfig.getJobQueuePollerInterval());
        }

        if (jobQueuePusherPoll > 0) {
            workhorseConfig.setJobQueuePusherPoll(jobQueuePusherPoll);

        } else {
            workhorseConfig.setJobQueuePusherPoll(newWorkhorseConfig.getJobQueuePusherPoll());
        }
        if (timeZone != null) {
            workhorseConfig.setTimeZone(timeZone);

        } else {
            workhorseConfig.setTimeZone(newWorkhorseConfig.getTimeZone());
        }

        if (persistenceTyp != null) {
            workhorseConfig.setPersistenceTyp(persistenceTyp);

        } else {
            workhorseConfig.setPersistenceTyp(newWorkhorseConfig.getPersistenceTyp());
        }

        workhorseConfig.setLogChange(newWorkhorseConfig.getLogChange());
        workhorseConfig.setLogWarnMarker(newWorkhorseConfig.getLogWarnMarker());
        workhorseConfig.setLogInfoMarker(newWorkhorseConfig.getLogInfoMarker());
        workhorseConfig.setLogTimeFormat(newWorkhorseConfig.getLogTimeFormat());
        workhorseConfig.setZombieCureStatus(newWorkhorseConfig.getZombieCureStatus());
        workhorseConfig.setZombieRecognitionTime(newWorkhorseConfig.getZombieRecognitionTime());

        workhorseConfigControl.updateWorkhorseConfig(workhorseConfig);
    }

    /**
     * Update the configuration of the job engine
     * 
     * @param newWorkhorseConfig
     * @return
     */
    public WorkhorseConfig updateWorkhorseConfig(WorkhorseConfig newWorkhorseConfig) {

        workhorseConfigControl.updateJobQueuePollerInterval(newWorkhorseConfig.getJobQueuePollerInterval());
        workhorseConfigControl.updateJobQueuePusherPoll(newWorkhorseConfig.getJobQueuePusherPoll());
        workhorseConfigControl.updateJobQueueMax(newWorkhorseConfig.getJobQueueMax());
        workhorseConfigControl.updateJobQueueMin(newWorkhorseConfig.getJobQueueMin());
        workhorseConfigControl.updateLogChange(newWorkhorseConfig.getLogChange());
        workhorseConfigControl.updateLogTimeFormatter(newWorkhorseConfig.getLogTimeFormat());
        workhorseConfigControl.updateTimeZone(newWorkhorseConfig.getTimeZone());
        workhorseConfigControl.updateLogInfoMarker(newWorkhorseConfig.getLogInfoMarker());
        workhorseConfigControl.updateLogWarnMarker(newWorkhorseConfig.getLogWarnMarker());
        workhorseConfigControl.updateLogErrorMarker(newWorkhorseConfig.getLogErrorMarker());

        workhorseConfigControl.updateWorkhorseConfig(workhorseConfig);

        // have to be the last update to start
        workhorseConfigControl.updatePersistenceTyp(newWorkhorseConfig.getPersistenceTyp());

        return newWorkhorseConfig;
    }

    public void updatePersistenceTyp(WorkhorseConfig workhorseConfig) {

        workhorseConfigControl.updatePersistenceTyp(workhorseConfig.getPersistenceTyp());

        restartEvent.fire(new RestartWorkhorseEvent(null, workhorseConfig));
    }

    public WorkhorseConfig getWorkhorseConfig() {
        return workhorseConfigControl.getWorkhorseConfig();
    }

}
