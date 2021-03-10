package io.coodoo.workhorse.persistence.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.core.entity.JobStatus;
import io.coodoo.workhorse.persistence.interfaces.listing.Metadata;
import io.coodoo.workhorse.persistence.interfaces.JobPersistence;
import io.coodoo.workhorse.persistence.interfaces.listing.ListingParameters;
import io.coodoo.workhorse.persistence.interfaces.listing.ListingResult;
import io.coodoo.workhorse.util.WorkhorseUtil;

@ApplicationScoped
public class MemoryJobPersistence implements JobPersistence {

    @Inject
    MemoryPersistence memoryPersistence;

    private AtomicLong incId = new AtomicLong(0);

    @Override
    public Job get(Long id) {

        return memoryPersistence.getJobs().get(id);
    }

    @Override
    public Job getByWorkerClassName(String jobClassName) {
        for (Job job : memoryPersistence.getJobs().values()) {
            if (job.getWorkerClassName().equals(jobClassName)) {
                return job;
            }
        }
        return null;
    }

    @Override
    public Job persist(Job job) {
        Long id = incId.getAndIncrement();
        job.setId(id);
        job.setCreatedAt(WorkhorseUtil.timestamp());
        memoryPersistence.getJobs().put(id, job);
        return job;
    }

    @Override
    public Job update(Job job) {
        memoryPersistence.getJobs().put(job.getId(), job);
        return job;
    }

    @Override
    public List<Job> getAll() {
        List<Job> result = new ArrayList<>();
        result.addAll(memoryPersistence.getJobs().values());
        return result;
    }

    @Override
    public Long count() {
        return Long.valueOf(memoryPersistence.getJobs().size());
    }

    @Override
    public String getPersistenceName() {
        return MemoryPersistence.NAME;
    }

    @Override
    public Job getByName(String jobName) {
        for (Job job : memoryPersistence.getJobs().values()) {
            if (Objects.equals(job.getName(), jobName)) {
                return job;
            }
        }
        return null;
    }

    @Override
    public List<Job> getAllByStatus(JobStatus jobStatus) {
        List<Job> result = new ArrayList<>();

        for (Job job : memoryPersistence.getJobs().values()) {
            if (Objects.equals(job.getStatus(), jobStatus)) {
                result.add(job);
            }
        }

        return result;
    }

    @Override
    public List<Job> getAllScheduled() {
        List<Job> result = new ArrayList<>();

        for (Job job : memoryPersistence.getJobs().values()) {
            if (job.getSchedule() != null && !job.getSchedule().isEmpty()) {
                result.add(job);
            }
        }

        return result;
    }

    @Override
    public Long countByStatus(JobStatus jobStatus) {
        return Long.valueOf(getAllByStatus(jobStatus).size());
    }

    @Override
    public void connect(Object... params) {
        // TODO Auto-generated method stub

    }

    @Override
    public ListingResult<Job> getJobListing(ListingParameters listingParameters) {
        List<Job> jobs = getAll();

        return new ListingResult<Job>(jobs, new Metadata(Long.valueOf(listingParameters.getLimit()), listingParameters));

    }
}
