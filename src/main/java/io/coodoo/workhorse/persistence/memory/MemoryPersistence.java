package io.coodoo.workhorse.persistence.memory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;

import io.coodoo.workhorse.core.entity.Execution;
import io.coodoo.workhorse.core.entity.ExecutionLog;
import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.core.entity.WorkhorseConfig;
import io.coodoo.workhorse.core.entity.WorkhorseLog;

@ApplicationScoped
public class MemoryPersistence {

    private WorkhorseConfig workhorseConfig;
    private Map<Long, WorkhorseLog> workhorseLog = new ConcurrentHashMap<>();
    private Map<Long, Job> jobs = new ConcurrentHashMap<>();
    private Map<Long, JobData> jobDataMap = new ConcurrentHashMap<>();

    @Deprecated
    private Map<Long, Execution> executions = new ConcurrentHashMap<>();
    @Deprecated
    private Map<Long, ExecutionLog> executionLogs = new ConcurrentHashMap<>();

    public Map<Long, JobData> getJobDataMap() {
        return jobDataMap;
    }

    public void setJobDataMap(Map<Long, JobData> jobDataMap) {
        this.jobDataMap = jobDataMap;
    }

    public synchronized WorkhorseConfig getWorkhorseConfig() {
        return workhorseConfig;
    }

    public synchronized WorkhorseConfig setWorkhorseConfig(WorkhorseConfig workhorseConfig) {
        this.workhorseConfig = workhorseConfig;
        return workhorseConfig;
    }

    @Deprecated
    public Map<Long, ExecutionLog> getExecutionLogs() {
        return executionLogs;
    }

    @Deprecated
    public Map<Long, Execution> getExecutions() {
        return executions;
    }

    public Map<Long, Job> getJobs() {
        return jobs;
    }

    public Map<Long, WorkhorseLog> getWorkhorseLog() {
        return workhorseLog;
    }

}
