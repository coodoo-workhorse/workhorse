package io.coodoo.workhorse.api.DTO;

import java.util.List;

import io.coodoo.workhorse.jobengine.entity.Job;

public class JobDTO extends Job {

    public List<MemoryCountData> memoryCount;

    public JobDTO() {}

    public JobDTO(Job job, List<MemoryCountData> memoryCount) {

        setId(job.getId());
        //setVersion(job.getVersion());
        setCreatedAt(job.getCreatedAt());
        setUpdatedAt(job.getUpdatedAt());
        setName(job.getName());
        setDescription(job.getDescription());
        //setTags(job.getTags());
        setWorkerClassName(job.getWorkerClassName());
        setParametersClassName(job.getParametersClassName());
        setSchedule(job.getSchedule());
        setStatus(job.getStatus());
        setThreads(job.getThreads());
        setMaxPerMinute(job.getMaxPerMinute());
        setFailRetries(job.getFailRetries());
        setRetryDelay(job.getRetryDelay());
        setDaysUntilCleanUp(job.getDaysUntilCleanUp());
        setUniqueInQueue(job.isUniqueInQueue());

        this.memoryCount = memoryCount;
    }
}
