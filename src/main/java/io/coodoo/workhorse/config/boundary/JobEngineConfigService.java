package io.coodoo.workhorse.config.boundary;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import io.coodoo.workhorse.config.control.JobEngineConfigControl;
import io.coodoo.workhorse.config.entity.JobEngineConfig;
import io.coodoo.workhorse.jobengine.control.events.RestartTheJobEngine;
import io.coodoo.workhorse.storage.persistenceInterface.PersistenceTyp;

@ApplicationScoped
public class JobEngineConfigService {

    @Inject
    JobEngineConfigControl jobEngineConfigControl;

    @Inject
    JobEngineConfig jobEngineConfig;

    @Inject
    Event<RestartTheJobEngine> restartEvent;

    /**
     * Initialize the config of the job engine
     * 
     * @param persistenceTyp
     */
    public void initializeJobEngineConfig() {

        JobEngineConfig newJobEngineConfig = jobEngineConfigControl.getJobEngineConfig();

        jobEngineConfig.setJobQueueMax(newJobEngineConfig.getJobQueueMax());
        jobEngineConfig.setJobQueueMin(newJobEngineConfig.getJobQueueMin());
        jobEngineConfig.setJobQueuePollerInterval(newJobEngineConfig.getJobQueuePollerInterval());
        jobEngineConfig.setJobQueuePusherPoll(newJobEngineConfig.getJobQueuePusherPoll());
        jobEngineConfig.setTimeZone(newJobEngineConfig.getTimeZone());
        jobEngineConfig.setLogChange(newJobEngineConfig.getLogChange());
        jobEngineConfig.setPersistenceTyp(newJobEngineConfig.getPersistenceTyp());
        jobEngineConfig.setLogWarnMarker(newJobEngineConfig.getLogWarnMarker());
        jobEngineConfig.setLogInfoMarker(newJobEngineConfig.getLogInfoMarker());
        jobEngineConfig.setLogTimeFormatter(newJobEngineConfig.getLogTimeFormatter());
        jobEngineConfig.setZombieCureStatus(newJobEngineConfig.getZombieCureStatus());
        jobEngineConfig.setZombieRecognitionTime(newJobEngineConfig.getZombieRecognitionTime());

        jobEngineConfigControl.updateJobEngineConfig(jobEngineConfig);


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

        JobEngineConfig newJobEngineConfig = jobEngineConfigControl.getJobEngineConfig();

        if (jobQueueMax > 0L) {
            jobEngineConfig.setJobQueueMax(jobQueueMax);
        } else {
            jobEngineConfig.setJobQueueMax(newJobEngineConfig.getJobQueueMax());
        }

        if (jobQueueMin > 0) {
            jobEngineConfig.setJobQueueMin(jobQueueMin);

        } else {
            jobEngineConfig.setJobQueueMin(newJobEngineConfig.getJobQueueMin());
        }

        if (jobQueuePollerInterval > 0) {
            jobEngineConfig.setJobQueuePollerInterval(jobQueuePollerInterval);

        } else {
            jobEngineConfig.setJobQueuePollerInterval(newJobEngineConfig.getJobQueuePollerInterval());
        }

        if (jobQueuePusherPoll > 0) {
            jobEngineConfig.setJobQueuePusherPoll(jobQueuePusherPoll);

        } else {
            jobEngineConfig.setJobQueuePusherPoll(newJobEngineConfig.getJobQueuePusherPoll());
        }
        if (timeZone != null) {
            jobEngineConfig.setTimeZone(timeZone);

        } else {
            jobEngineConfig.setTimeZone(newJobEngineConfig.getTimeZone());
        }

        if (persistenceTyp != null) {
            jobEngineConfig.setPersistenceTyp(persistenceTyp);

        } else {
            jobEngineConfig.setPersistenceTyp(newJobEngineConfig.getPersistenceTyp());
        }

        jobEngineConfig.setLogChange(newJobEngineConfig.getLogChange());
        jobEngineConfig.setLogWarnMarker(newJobEngineConfig.getLogWarnMarker());
        jobEngineConfig.setLogInfoMarker(newJobEngineConfig.getLogInfoMarker());
        jobEngineConfig.setLogTimeFormatter(newJobEngineConfig.getLogTimeFormatter());
        jobEngineConfig.setZombieCureStatus(newJobEngineConfig.getZombieCureStatus());
        jobEngineConfig.setZombieRecognitionTime(newJobEngineConfig.getZombieRecognitionTime());

        jobEngineConfigControl.updateJobEngineConfig(jobEngineConfig);
    }

    /**
     * Update the configuration of the job engine
     * @param newJobEngineConfig
     * @return
     */
    public JobEngineConfig updateJobEngineConfig(JobEngineConfig newJobEngineConfig) {

        jobEngineConfigControl.updateJobQueuePollerInterval(newJobEngineConfig.getJobQueuePollerInterval());
        jobEngineConfigControl.updateJobQueuePusherPoll(newJobEngineConfig.getJobQueuePusherPoll());
        jobEngineConfigControl.updateJobQueueMax(newJobEngineConfig.getJobQueueMax());
        jobEngineConfigControl.updateJobQueueMin(newJobEngineConfig.getJobQueueMin());
        jobEngineConfigControl.updateLogChange(newJobEngineConfig.getLogChange());
        jobEngineConfigControl.updateLogTimeFormatter(newJobEngineConfig.getLogTimeFormatter());
        jobEngineConfigControl.updateTimeZone(newJobEngineConfig.getTimeZone());
        jobEngineConfigControl.updateLogInfoMarker(newJobEngineConfig.getLogInfoMarker());
        jobEngineConfigControl.updateLogWarnMarker(newJobEngineConfig.getLogWarnMarker());
        jobEngineConfigControl.updateLogErrorMarker(newJobEngineConfig.getLogErrorMarker());

        jobEngineConfigControl.updateJobEngineConfig(jobEngineConfig);

        // have to be the last update to start
        jobEngineConfigControl.updatePersistenceTyp(newJobEngineConfig.getPersistenceTyp());

        return newJobEngineConfig;
    }

    public void updatePersistenceTyp(JobEngineConfig jobEngineConfig) {

        jobEngineConfigControl.updatePersistenceTyp(jobEngineConfig.getPersistenceTyp());

        restartEvent.fire(new RestartTheJobEngine(null, jobEngineConfig));
    }

    public JobEngineConfig getJobEngineConfig() {
        return jobEngineConfigControl.getJobEngineConfig();
    }

}
