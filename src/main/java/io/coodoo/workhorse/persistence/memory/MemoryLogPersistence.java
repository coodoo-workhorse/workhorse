package io.coodoo.workhorse.persistence.memory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.coodoo.workhorse.core.entity.WorkhorseLog;
import io.coodoo.workhorse.persistence.interfaces.LogPersistence;
import io.coodoo.workhorse.persistence.interfaces.listing.ListingParameters;
import io.coodoo.workhorse.persistence.interfaces.listing.ListingResult;
import io.coodoo.workhorse.util.CollectionListing;
import io.coodoo.workhorse.util.WorkhorseUtil;

@ApplicationScoped
public class MemoryLogPersistence implements LogPersistence {

    @Inject
    MemoryPersistence memoryPersistence;

    private AtomicLong incId = new AtomicLong(0);

    @Override
    public WorkhorseLog get(Long id) {
        return memoryPersistence.getWorkhorseLog().get(id);
    }

    @Override
    public ListingResult<WorkhorseLog> getWorkhorseLogListing(ListingParameters listingParameters) {

        Collection<WorkhorseLog> logs = memoryPersistence.getWorkhorseLog().values();
        return CollectionListing.getListingResult(logs, WorkhorseLog.class, listingParameters);
    }

    @Override
    public WorkhorseLog update(Long id, WorkhorseLog workhorseLog) {
        memoryPersistence.getWorkhorseLog().put(id, workhorseLog);
        return workhorseLog;
    }

    @Override
    public WorkhorseLog delete(Long id) {
        return memoryPersistence.getWorkhorseLog().remove(id);
    }

    @Override
    public WorkhorseLog persist(WorkhorseLog workhorseLog) {
        Long id = incId.getAndIncrement();
        workhorseLog.setId(id);
        workhorseLog.setCreatedAt(WorkhorseUtil.timestamp());
        memoryPersistence.getWorkhorseLog().put(id, workhorseLog);
        return workhorseLog;
    }

    @Override
    public List<WorkhorseLog> getAll(int limit) {
        List<WorkhorseLog> workhorseLogs = new ArrayList<>();
        workhorseLogs.addAll(memoryPersistence.getWorkhorseLog().values());
        int logSize = workhorseLogs.size();
        limit = limit > logSize ? logSize : limit;
        return workhorseLogs.subList(0, limit);

    }

    @Override
    public int deleteByJobId(Long jobId) {
        int count = 0;
        for (WorkhorseLog workhorseLog : memoryPersistence.getWorkhorseLog().values()) {
            if (workhorseLog.getJobId() != null && workhorseLog.getJobId().equals(jobId)) {
                memoryPersistence.getWorkhorseLog().remove(workhorseLog.getId(), workhorseLog);
                count++;
            }
        }
        return count;
    }

    @Override
    public String getPersistenceName() {
        return MemoryPersistence.NAME;
    }

    @Override
    public void connect(Object... params) {
        // TODO Auto-generated method stub

    }

}
