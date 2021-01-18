package io.coodoo.workhorse.config.boundary;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import io.coodoo.workhorse.config.control.WorkhorseConfigControl;
import io.coodoo.workhorse.config.entity.WorkhorseConfig;
import io.coodoo.workhorse.core.control.event.RestartWorkhorseEvent;
import io.coodoo.workhorse.persistence.interfaces.PersistenceTyp;

@ApplicationScoped
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
    public void initializeJobEngineConfig() {

        WorkhorseConfig newJobEngineConfig = workhorseConfigControl.getJobEngineConfig();

        workhorseConfig.setJobQueueMax(newJobEngineConfig.getJobQueueMax());
        workhorseConfig.setJobQueueMin(newJobEngineConfig.getJobQueueMin());
        workhorseConfig.setJobQueuePollerInterval(newJobEngineConfig.getJobQueuePollerInterval());
        workhorseConfig.setJobQueuePusherPoll(newJobEngineConfig.getJobQueuePusherPoll());
        workhorseConfig.setTimeZone(newJobEngineConfig.getTimeZone());
        workhorseConfig.setLogChange(newJobEngineConfig.getLogChange());
        workhorseConfig.setPersistenceTyp(newJobEngineConfig.getPersistenceTyp());
        workhorseConfig.setLogWarnMarker(newJobEngineConfig.getLogWarnMarker());
        workhorseConfig.setLogInfoMarker(newJobEngineConfig.getLogInfoMarker());
        workhorseConfig.setLogTimeFormat(newJobEngineConfig.getLogTimeFormat());
        workhorseConfig.setZombieCureStatus(newJobEngineConfig.getZombieCureStatus());
        workhorseConfig.setZombieRecognitionTime(newJobEngineConfig.getZombieRecognitionTime());

        workhorseConfigControl.updateJobEngineConfig(workhorseConfig);


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
    public void initializeJobEngineConfig(String timeZone, int jobQueuePollerInterval, int jobQueuePusherPoll,
            Long jobQueueMax, int jobQueueMin, PersistenceTyp persistenceTyp) {

        WorkhorseConfig newJobEngineConfig = workhorseConfigControl.getJobEngineConfig();

        if (jobQueueMax > 0L) {
            workhorseConfig.setJobQueueMax(jobQueueMax);
        } else {
            workhorseConfig.setJobQueueMax(newJobEngineConfig.getJobQueueMax());
        }

        if (jobQueueMin > 0) {
            workhorseConfig.setJobQueueMin(jobQueueMin);

        } else {
            workhorseConfig.setJobQueueMin(newJobEngineConfig.getJobQueueMin());
        }

        if (jobQueuePollerInterval > 0) {
            workhorseConfig.setJobQueuePollerInterval(jobQueuePollerInterval);

        } else {
            workhorseConfig.setJobQueuePollerInterval(newJobEngineConfig.getJobQueuePollerInterval());
        }

        if (jobQueuePusherPoll > 0) {
            workhorseConfig.setJobQueuePusherPoll(jobQueuePusherPoll);

        } else {
            workhorseConfig.setJobQueuePusherPoll(newJobEngineConfig.getJobQueuePusherPoll());
        }
        if (timeZone != null) {
            workhorseConfig.setTimeZone(timeZone);

        } else {
            workhorseConfig.setTimeZone(newJobEngineConfig.getTimeZone());
        }

        if (persistenceTyp != null) {
            workhorseConfig.setPersistenceTyp(persistenceTyp);

        } else {
            workhorseConfig.setPersistenceTyp(newJobEngineConfig.getPersistenceTyp());
        }

        workhorseConfig.setLogChange(newJobEngineConfig.getLogChange());
        workhorseConfig.setLogWarnMarker(newJobEngineConfig.getLogWarnMarker());
        workhorseConfig.setLogInfoMarker(newJobEngineConfig.getLogInfoMarker());
        workhorseConfig.setLogTimeFormat(newJobEngineConfig.getLogTimeFormat());
        workhorseConfig.setZombieCureStatus(newJobEngineConfig.getZombieCureStatus());
        workhorseConfig.setZombieRecognitionTime(newJobEngineConfig.getZombieRecognitionTime());

        workhorseConfigControl.updateJobEngineConfig(workhorseConfig);
    }

    /**
     * Update the configuration of the job engine
     * @param newJobEngineConfig
     * @return
     */
    public WorkhorseConfig updateJobEngineConfig(WorkhorseConfig newJobEngineConfig) {

        workhorseConfigControl.updateJobQueuePollerInterval(newJobEngineConfig.getJobQueuePollerInterval());
        workhorseConfigControl.updateJobQueuePusherPoll(newJobEngineConfig.getJobQueuePusherPoll());
        workhorseConfigControl.updateJobQueueMax(newJobEngineConfig.getJobQueueMax());
        workhorseConfigControl.updateJobQueueMin(newJobEngineConfig.getJobQueueMin());
        workhorseConfigControl.updateLogChange(newJobEngineConfig.getLogChange());
        workhorseConfigControl.updateLogTimeFormatter(newJobEngineConfig.getLogTimeFormat());
        workhorseConfigControl.updateTimeZone(newJobEngineConfig.getTimeZone());
        workhorseConfigControl.updateLogInfoMarker(newJobEngineConfig.getLogInfoMarker());
        workhorseConfigControl.updateLogWarnMarker(newJobEngineConfig.getLogWarnMarker());
        workhorseConfigControl.updateLogErrorMarker(newJobEngineConfig.getLogErrorMarker());

        workhorseConfigControl.updateJobEngineConfig(workhorseConfig);

        // have to be the last update to start
        workhorseConfigControl.updatePersistenceTyp(newJobEngineConfig.getPersistenceTyp());

        return newJobEngineConfig;
    }

    public void updatePersistenceTyp(WorkhorseConfig jobEngineConfig) {

        workhorseConfigControl.updatePersistenceTyp(jobEngineConfig.getPersistenceTyp());

        restartEvent.fire(new RestartWorkhorseEvent(null, jobEngineConfig));
    }

    public WorkhorseConfig getJobEngineConfig() {
        return workhorseConfigControl.getJobEngineConfig();
    }

}
