package io.coodoo.workhorse.persistence.interfaces;

import java.util.List;

import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.core.entity.JobStatus;
import io.coodoo.workhorse.core.entity.JobStatusCount;
import io.coodoo.workhorse.persistence.interfaces.listing.ListingParameters;
import io.coodoo.workhorse.persistence.interfaces.listing.ListingResult;

public interface JobPersistence {

    /**
     * 
     * @return retrieve the name of the persistence to initialize
     */
    String getPersistenceName();

    /**
     * Get a job by his Id
     * 
     * @param jobId
     * @return
     */
    Job get(Long jobId);

    /**
     * Get a Job by his Name
     * 
     * @param jobName
     * @return
     */
    Job getByName(String jobName);

    /**
     * Get a job by the Worker classname
     * 
     * @param jobClassName
     * @return
     */
    Job getByWorkerClassName(String jobClassName);

    /**
     * Get all jobs in storage
     * 
     * @return List of all Jobs
     */
    List<Job> getAll();

    /**
     * Get the listing result of jobs
     * 
     * @param listingParameters defines the listing queue. It contains optional query parameters as described above
     * @return list of jobs
     */
    ListingResult<Job> getJobListing(ListingParameters listingParameters);

    /**
     * Get all jobs filtered by a given status
     * 
     * @param jobStatus
     * @return List of Job filtered by status
     */
    List<Job> getAllByStatus(JobStatus jobStatus);

    /**
     * Get All scheduled Job
     * 
     * @return List of scheduled Job
     */
    List<Job> getAllScheduled();

    /**
     * Get the count of jobs by status
     * 
     * @return JobStatusCount
     */
    JobStatusCount getJobStatusCount();

    /**
     * Get the number of Job in storage
     * 
     * @return Number of Job
     */
    Long count();

    /**
     * Get the number of Jobs in storage filtered by given Status
     * 
     * @param jobStatus
     * @return Number of Job by Status
     */
    Long countByStatus(JobStatus jobStatus);

    /**
     * persist a Job in storage. The id of the job have to be set by the persistence.
     * 
     * @param job
     * @return new persisted job
     */
    Job persist(Job job);

    /**
     * Update the job given by Id
     * 
     * @return updated job
     */
    Job update(Job job);

    /**
     * Delete the job given by Id
     * 
     * @param jobId
     */
    void deleteJob(Long jobId);

}
