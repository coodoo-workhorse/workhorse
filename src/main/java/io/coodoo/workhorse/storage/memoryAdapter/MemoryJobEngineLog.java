package io.coodoo.workhorse.storage.memoryAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.coodoo.workhorse.config.entity.JobEngineConfig;
import io.coodoo.workhorse.log.entity.JobEngineLog;
import io.coodoo.workhorse.storage.persistenceInterface.JobEngineLogPersistence;
import io.coodoo.workhorse.storage.persistenceInterface.PersistenceTyp;

@ApplicationScoped
public class MemoryJobEngineLog implements JobEngineLogPersistence {

    @Inject
    Memory memoryService;

    @Inject
    JobEngineConfig jobEngineConfig;

    private AtomicLong incId = new AtomicLong(0);

    @Override
    public JobEngineLog get(Long id) {
        return memoryService.jobEngineLog.get(id);
    }

    @Override
    public JobEngineLog update(Long id, JobEngineLog jobEngineLog) {
        memoryService.jobEngineLog.put(id, jobEngineLog);
        return jobEngineLog;
    }

    @Override
    public JobEngineLog delete(Long id) {
        // TODO Auto-generated method stub
        return memoryService.jobEngineLog.remove(id);
    }

    @Override
    public JobEngineLog persist(JobEngineLog jobEngineLog) {
        Long id = incId.getAndIncrement();
        jobEngineLog.setId(id);
        jobEngineLog.setCreatedAt(jobEngineConfig.timestamp());
        memoryService.jobEngineLog.put(id, jobEngineLog);
        return jobEngineLog;
    }

    @Override
    public List<JobEngineLog> getAll(int limit) {
        List<JobEngineLog> result = new ArrayList<>();
        List<JobEngineLog> jobEngineLogs = new ArrayList<>();
        jobEngineLogs.addAll(memoryService.jobEngineLog.values());
        // Long index = incId.get();
        // int count = 0;

        // JobEngineLog jobEngineLog = memoryService.jobEngineLog.get(index);
        // while (jobEngineLog != null && count <= limit) {

        //     result.add(jobEngineLog);  
        //     count++;
        //     index--;
        //     jobEngineLog = memoryService.jobEngineLog.get(index);
        // }

    return jobEngineLogs;

    }

    @Override
    public int deleteByJobId(Long jobId) {
        int count = 0;
        for (JobEngineLog jobEngineLog : memoryService.jobEngineLog.values()) {
            if (jobEngineLog.getJobId() != null && jobEngineLog.getJobId().equals(jobId)) {
                memoryService.jobEngineLog.remove(jobEngineLog.getId(), jobEngineLog);
                count++;
            }
        }
        return count;
    }

    @Override
    public PersistenceTyp getPersistenceTyp() {
        // TODO Auto-generated method stub
        return PersistenceTyp.MEMORY;
    }

    @Override
    public void connect(Object... params) {
        // TODO Auto-generated method stub

    }

}
