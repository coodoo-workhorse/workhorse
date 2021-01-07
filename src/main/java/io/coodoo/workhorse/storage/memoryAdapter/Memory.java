package io.coodoo.workhorse.storage.memoryAdapter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;

import io.coodoo.workhorse.config.entity.JobEngineConfig;
import io.coodoo.workhorse.jobengine.entity.Job;
import io.coodoo.workhorse.jobengine.entity.JobExecution;
import io.coodoo.workhorse.log.entity.JobEngineLog;


@ApplicationScoped
public class Memory {
    public Map<Long, JobExecution> jobExecutions = new ConcurrentHashMap<>();
     
    public Map<Long, Job> jobs = new ConcurrentHashMap<>();

    public Map<Long, JobEngineLog> jobEngineLog = new ConcurrentHashMap<>();

    private JobEngineConfig engineConfig;

    public synchronized JobEngineConfig getJobEngineConfig() {
      return  engineConfig;
    }

    public synchronized JobEngineConfig  setJobEngineConfig(JobEngineConfig jobEngineConfig) {
        engineConfig = jobEngineConfig;
        return engineConfig;
    }

}