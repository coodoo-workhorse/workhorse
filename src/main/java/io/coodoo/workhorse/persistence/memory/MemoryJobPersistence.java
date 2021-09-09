package io.coodoo.workhorse.persistence.memory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.core.entity.JobStatus;
import io.coodoo.workhorse.core.entity.JobStatusCount;
import io.coodoo.workhorse.persistence.interfaces.JobPersistence;
import io.coodoo.workhorse.persistence.interfaces.listing.ListingParameters;
import io.coodoo.workhorse.persistence.interfaces.listing.ListingResult;
import io.coodoo.workhorse.util.CollectionListing;
import io.coodoo.workhorse.util.WorkhorseUtil;

@ApplicationScoped
public class MemoryJobPersistence implements JobPersistence {

    @Inject
    MemoryPersistence memoryPersistence;

    private AtomicLong incId = new AtomicLong(0);

    @Override
    public String getPersistenceName() {
        return MemoryConfig.NAME;
    }

    @Override
    public Job get(Long jobId) {
        return memoryPersistence.getJobs().get(jobId);
    }

    @Override
    public ListingResult<Job> getJobListing(ListingParameters listingParameters) {

        Collection<Job> jobs = memoryPersistence.getJobs().values();
        return CollectionListing.getListingResult(jobs, Job.class, listingParameters);
    }

    @Override
    public Job getByWorkerClassName(String jobClassName) {

        ListingParameters listingParameters = new ListingParameters(1);
        listingParameters.addFilterAttributes("workerClassName", jobClassName);
        ListingResult<Job> listingResult = getJobListing(listingParameters);

        if (listingResult.getResults().isEmpty()) {
            return null;
        }
        return listingResult.getResults().get(0);
    }

    @Override
    public Job persist(Job job) {

        Long jobId = incId.getAndIncrement();
        job.setId(jobId);
        job.setCreatedAt(WorkhorseUtil.timestamp());

        memoryPersistence.getJobs().put(jobId, job);
        JobData jobData = new JobData();
        memoryPersistence.getJobDataMap().put(job.getId(), jobData);
        return job;
    }

    @Override
    public Job update(Job job) {

        job.setUpdatedAt(WorkhorseUtil.timestamp());
        memoryPersistence.getJobs().put(job.getId(), job);
        return job;
    }

    @Override
    public void deleteJob(Long jobId) {
        memoryPersistence.getJobDataMap().remove(jobId);
    }

    @Override
    public List<Job> getAll() {
        return new ArrayList<>(memoryPersistence.getJobs().values());
    }

    @Override
    public Long count() {
        return Long.valueOf(memoryPersistence.getJobs().size());
    }

    @Override
    public Job getByName(String jobName) {

        ListingParameters listingParameters = new ListingParameters(1);
        listingParameters.addFilterAttributes("name", jobName);
        ListingResult<Job> listingResult = getJobListing(listingParameters);

        if (listingResult.getResults().isEmpty()) {
            return null;
        }
        return listingResult.getResults().get(0);
    }

    @Override
    public List<Job> getAllByStatus(JobStatus jobStatus) {

        ListingParameters listingParameters = new ListingParameters(0);
        listingParameters.addFilterAttributes("status", jobStatus);
        ListingResult<Job> listingResult = getJobListing(listingParameters);

        return listingResult.getResults();
    }

    @Override
    public List<Job> getAllScheduled() {

        ListingParameters listingParameters = new ListingParameters(0);
        listingParameters.addFilterAttributes("schedule", " "); // jeder schedule sollte min ein leerzeichen haben...
        ListingResult<Job> listingResult = getJobListing(listingParameters);

        return listingResult.getResults();
    }

    @Override
    public Long countByStatus(JobStatus jobStatus) {
        return Long.valueOf(getAllByStatus(jobStatus).size());
    }

    @Override
    public JobStatusCount getJobStatusCount() {

        long countActive = 0L;
        long countInactive = 0L;
        long countError = 0L;
        long countNoWorker = 0L;

        ListingParameters listingParameters = new ListingParameters(0);

        listingParameters.addFilterAttributes("status", JobStatus.ACTIVE);
        countActive = countActive + getJobListing(listingParameters).getMetadata().getCount();

        listingParameters.addFilterAttributes("status", JobStatus.INACTIVE);
        countInactive = countInactive + getJobListing(listingParameters).getMetadata().getCount();

        listingParameters.addFilterAttributes("status", JobStatus.NO_WORKER);
        countNoWorker = countNoWorker + getJobListing(listingParameters).getMetadata().getCount();

        listingParameters.addFilterAttributes("status", JobStatus.ERROR);
        countError = countError + getJobListing(listingParameters).getMetadata().getCount();

        return new JobStatusCount(countActive, countInactive, countNoWorker, countError);
    }

}
