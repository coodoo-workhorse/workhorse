package io.coodoo.workhorse.persistence.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.coodoo.workhorse.config.entity.WorkhorseConfig;
import io.coodoo.workhorse.core.entity.WorkhorseLog;
import io.coodoo.workhorse.persistence.interfaces.LogPersistence;
import io.coodoo.workhorse.persistence.interfaces.PersistenceTyp;

@ApplicationScoped
public class MemoryLogPersistence implements LogPersistence {

    @Inject
    MemoryPersistence memoryService;

    @Inject
    WorkhorseConfig jobEngineConfig;

    private AtomicLong incId = new AtomicLong(0);

    @Override
    public WorkhorseLog get(Long id) {
        return memoryService.getWorkhorseLog().get(id);
    }

    @Override
    public WorkhorseLog update(Long id, WorkhorseLog jobEngineLog) {
        memoryService.getWorkhorseLog().put(id, jobEngineLog);
        return jobEngineLog;
    }

    @Override
    public WorkhorseLog delete(Long id) {
        return memoryService.getWorkhorseLog().remove(id);
    }

    @Override
    public WorkhorseLog persist(WorkhorseLog jobEngineLog) {
        Long id = incId.getAndIncrement();
        jobEngineLog.setId(id);
        jobEngineLog.setCreatedAt(jobEngineConfig.timestamp());
        memoryService.getWorkhorseLog().put(id, jobEngineLog);
        return jobEngineLog;
    }

    @Override
    public List<WorkhorseLog> getAll(int limit) {
        List<WorkhorseLog> result = new ArrayList<>();
        List<WorkhorseLog> jobEngineLogs = new ArrayList<>();
        jobEngineLogs.addAll(memoryService.getWorkhorseLog().values());
        // Long index = incId.get();
        // int count = 0;

        // JobEngineLog jobEngineLog = memoryService.jobEngineLog.get(index);
        // while (jobEngineLog != null && count <= limit) {

        // result.add(jobEngineLog);
        // count++;
        // index--;
        // jobEngineLog = memoryService.jobEngineLog.get(index);
        // }

        return jobEngineLogs;

    }

    @Override
    public int deleteByJobId(Long jobId) {
        int count = 0;
        for (WorkhorseLog jobEngineLog : memoryService.getWorkhorseLog().values()) {
            if (jobEngineLog.getJobId() != null && jobEngineLog.getJobId().equals(jobId)) {
                memoryService.getWorkhorseLog().remove(jobEngineLog.getId(), jobEngineLog);
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
