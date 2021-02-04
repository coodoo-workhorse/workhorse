package io.coodoo.workhorse.api.DTO;

import io.coodoo.workhorse.core.entity.WorkhorseConfig;
import io.coodoo.workhorse.persistence.interfaces.PersistenceTyp;

public class ConfigDTO {
    public String timeZone;
    public Long jobQueueMax;
    public int jobQueueMin;
    public int jobQueuePollerInterval; // in seconds
    public int jobQueuePusherPoll; // in seconds
    public PersistenceTyp persistenceTyp;

    public boolean isPusherAvailable;

    public ConfigDTO() {}

    public ConfigDTO(WorkhorseConfig workhorseConfig) {
        timeZone = workhorseConfig.getTimeZone();
        jobQueueMax = workhorseConfig.getJobQueueMax();
        jobQueueMin = workhorseConfig.getJobQueueMin();
        jobQueuePollerInterval = workhorseConfig.getJobQueuePollerInterval();
        jobQueuePusherPoll = workhorseConfig.getJobQueuePusherPoll();
        persistenceTyp = workhorseConfig.getPersistenceTyp();
    }
}
