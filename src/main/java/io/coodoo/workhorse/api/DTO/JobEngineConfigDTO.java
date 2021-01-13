package io.coodoo.workhorse.api.DTO;

import io.coodoo.workhorse.config.entity.JobEngineConfig;
import io.coodoo.workhorse.persistence.interfaces.PersistenceTyp;

public class JobEngineConfigDTO {
    public String timeZone ;
    public Long jobQueueMax ;
    public int jobQueueMin ;
    public int jobQueuePollerInterval ; // In second
    public int jobQueuePusherPoll ; // In second
    public PersistenceTyp persistenceTyp;


    public boolean isPusherAvailable;

    public JobEngineConfigDTO(){}

    public JobEngineConfigDTO(JobEngineConfig jobEngineConfig ) {
        timeZone = jobEngineConfig.getTimeZone();
        jobQueueMax = jobEngineConfig.getJobQueueMax();
        jobQueueMin = jobEngineConfig.getJobQueueMin();
        jobQueuePollerInterval = jobEngineConfig.getJobQueuePollerInterval();
        jobQueuePusherPoll = jobEngineConfig.getJobQueuePusherPoll();
        persistenceTyp = jobEngineConfig.getPersistenceTyp();
    }
}
