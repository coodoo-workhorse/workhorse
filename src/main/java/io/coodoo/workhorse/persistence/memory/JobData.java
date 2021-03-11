package io.coodoo.workhorse.persistence.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.coodoo.workhorse.core.entity.Execution;
import io.coodoo.workhorse.core.entity.ExecutionLog;
import io.coodoo.workhorse.core.entity.Job;

/**
 * @author coodoo GmbH (coodoo.io)
 */
public class JobData {

    public Job job;
    public List<Long> orderedIds;
    public Map<Long, Execution> executions;
    public Map<Long, ExecutionLog> executionLogs;

    public JobData(Job job) {
        this.job = job;
        orderedIds = new ArrayList<>();
        executions = new ConcurrentHashMap<>();
        executionLogs = new ConcurrentHashMap<>();
    }

}
