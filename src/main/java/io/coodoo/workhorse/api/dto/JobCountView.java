package io.coodoo.workhorse.api.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.core.entity.JobStatus;

public class JobCountView {

    public Long id;

    public LocalDateTime createdAt;

    public LocalDateTime updatedAt;

    public String name;

    public String description;

    public List<String> tags = new ArrayList<>();

    public String workerClassName;

    public String parametersClassName;

    public String schedule;

    public JobStatus status;

    public int threads;

    public Integer maxPerMinute;

    public int failRetries;

    public int retryDelay;

    public int daysUntilCleanUp;

    public boolean uniqueInQueue;

    public int total;

    public Integer queued;

    public Integer running;

    public JobCountView() {}

    public JobCountView(Job job) {
        this.id = job.getId();
        this.createdAt = job.getCreatedAt();
        this.updatedAt = job.getUpdatedAt();
        this.name = job.getName();
        this.description = job.getDescription();
        // this.tags = job.tags;
        this.workerClassName = job.getWorkerClassName();
        this.parametersClassName = job.getParametersClassName();
        this.schedule = job.getSchedule();
        this.status = job.getStatus();
        this.threads = job.getThreads();
        this.maxPerMinute = job.getMaxPerMinute();
        this.failRetries = job.getFailRetries();
        this.retryDelay = job.getRetryDelay();
        this.daysUntilCleanUp = job.getMinutesUntilCleanUp();
        this.uniqueInQueue = job.isUniqueQueued();
        // this.total = job.total;
        // this.queued = queued;
        // this.running = running;
    }
}
