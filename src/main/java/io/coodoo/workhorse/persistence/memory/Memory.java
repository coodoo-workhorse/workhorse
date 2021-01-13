package io.coodoo.workhorse.persistence.memory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;

import io.coodoo.workhorse.config.entity.JobEngineConfig;
import io.coodoo.workhorse.jobengine.entity.Job;
import io.coodoo.workhorse.jobengine.entity.JobExecution;
import io.coodoo.workhorse.log.entity.JobEngineLog;

@ApplicationScoped
public class Memory {

    private Map<Long, JobExecution> jobExecutions = new ConcurrentHashMap<>();

    private Map<Long, Job> jobs = new ConcurrentHashMap<>();

    private Map<Long, JobEngineLog> jobEngineLog = new ConcurrentHashMap<>();

    private JobEngineConfig engineConfig;

    public synchronized JobEngineConfig getJobEngineConfig() {
        return engineConfig;
    }

    public synchronized JobEngineConfig setJobEngineConfig(JobEngineConfig jobEngineConfig) {
        engineConfig = jobEngineConfig;
        return engineConfig;
    }

    public Map<Long, JobExecution> getJobExecutions() {
        return jobExecutions;
    }

    public Map<Long, Job> getJobs() {
        return jobs;
    }

    public Map<Long, JobEngineLog> getJobEngineLog() {
        return jobEngineLog;
    }

}
