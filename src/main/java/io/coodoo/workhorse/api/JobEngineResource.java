package io.coodoo.workhorse.api;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.coodoo.framework.listing.boundary.ListingParameters;
import io.coodoo.framework.listing.boundary.ListingResult;
import io.coodoo.framework.listing.boundary.Metadata;
import io.coodoo.workhorse.api.DTO.GroupInfo;
import io.coodoo.workhorse.api.DTO.JobCountView;
import io.coodoo.workhorse.api.DTO.JobDTO;
import io.coodoo.workhorse.api.DTO.JobEngineInfo;
import io.coodoo.workhorse.api.DTO.JobExecutionView;
import io.coodoo.workhorse.api.DTO.JobScheduleExecutionTimeDTO;
import io.coodoo.workhorse.jobengine.boundary.JobEngineService;
import io.coodoo.workhorse.jobengine.entity.Job;
import io.coodoo.workhorse.jobengine.entity.JobExecution;

@Path("/workhorse")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class JobEngineResource {

    @Inject
    JobEngineService jobEngineService;


    @GET
    @Path("scheduled-job/{jobId}/stop")
    public void stopScheduledJob(@PathParam("jobId") Long jobId) {
        jobEngineService.stopScheduledJob(jobId);
    }

    @GET
    @Path("/infos")
    public List<JobEngineInfo> getJobEngineInfos() {
        return jobEngineService.getAllJobs().stream().map(job -> jobEngineService.getJobEngineInfo(job.getId()))
                .collect(Collectors.toList());
    }

    @GET
    @Path("/infos/{jobId}")
    public JobEngineInfo getJobEngineInfo(@PathParam("jobId") Long jobId) {
        return jobEngineService.getJobEngineInfo(jobId);
    }

    @GET
    @Path("/start")
    public Response start(@QueryParam("interval") Integer interval) {

        // if (interval != null) {
        // JobEngineConfig.JOB_QUEUE_POLLER_INTERVAL = interval;
        // }
        jobEngineService.startTheEngine();
        return Response.ok().build();
    }

    @GET
    @Path("/stop")
    public Response stop() {
        jobEngineService.stopTheEngine();
        return Response.ok().build();
    }

    @GET
    @Path("/is-running")
    public Response isRunning() {
        return Response.ok(jobEngineService.isRunning()).build();
    }

    @GET
    @Path("/jobs")
    public ListingResult<JobDTO> getJobs(@BeanParam ListingParameters listingParameters) {
        List<Job> listJobs = jobEngineService.getAllJobs();
        ListingResult<Job> jobsListing = new ListingResult(listJobs, new Metadata(100L, listingParameters));
        List<JobDTO> results = jobsListing.getResults().stream().map(job -> new JobDTO(job, null))
                .collect(Collectors.toList());

        return new ListingResult<JobDTO>(results, jobsListing.getTerms(), jobsListing.getStats(),
                jobsListing.getMetadata());
    }

    @PUT
    @Path("/jobs/{jobId}")
    public Job updateJob(@PathParam("jobId") Long jobId, Job job) {
        return jobEngineService.updateJob(jobId, job.getName(), job.getDescription(), job.getWorkerClassName(),
                job.getSchedule(), job.getStatus(), job.getThreads(), job.getMaxPerMinute(), job.getFailRetries(),
                job.getRetryDelay(), job.getDaysUntilCleanUp(), job.isUniqueInQueue());
    }

    @GET
    @Path("/jobs-count")
    public ListingResult<JobCountView> getJobsWithCounts(@BeanParam ListingParameters listingParameters) {
        List<Job> listJobs = jobEngineService.getAllJobs();

        ListingResult<Job> jobsListing = new ListingResult(listJobs, new Metadata(100L, listingParameters));

        List<JobCountView> results = jobsListing.getResults().stream().map(job -> new JobCountView(job))
                .collect(Collectors.toList());

        return new ListingResult<JobCountView>(results, jobsListing.getTerms(), jobsListing.getStats(),
                jobsListing.getMetadata());

    }

    @GET
    @Path("/executions/{jobExecutionId}")
    public JobExecution getJobExecution(@PathParam("jobExecutionId") Long jobExecutionId) {
        return jobEngineService.getJobExecutionById(null, jobExecutionId);
    }

    @POST
    @Path("/jobs/{jobId}/executions")
    public JobExecution createJobExecution(@PathParam("jobId") Long jobId, JobExecution jobExecution) {
        return jobEngineService.createJobExecution(jobId, jobExecution.getParameters(), jobExecution.getPriority(),
                jobExecution.getMaturity(), jobExecution.getBatchId(), jobExecution.getChainId(),
                jobExecution.getChainedPreviousExecutionId(), false);
    }

    @PUT
    @Path("/jobs/{jobId}/executions/{jobExecutionId}")
    public JobExecution updateJobExecution(@PathParam("jobId") Long jobId,
            @PathParam("jobExecutionId") Long jobExecutionId, JobExecution jobExecution) {
        return jobEngineService.updateJobExecution(jobId, jobExecutionId, jobExecution.getStatus(),
                jobExecution.getParameters(), jobExecution.getPriority(), jobExecution.getMaturity(),
                jobExecution.getFailRetry());
    }

    @DELETE
    @Path("/jobs/{jobId}/executions/{jobExecutionId}")
    public void deleteJobExecution(@PathParam("jobId") Long jobId, @PathParam("jobExecutionId") Long jobExecutionId) {
        jobEngineService.deleteJobExecution(jobId, jobExecutionId);
    }

    @GET
    @Path("/jobs/{jobId}/execution-views")
    public ListingResult<JobExecutionView> getExecutionViews(@PathParam("jobId") Long jobId,
            @BeanParam ListingParameters listingParameters) {

        if (jobId != null && jobId > 0) {
            listingParameters.addFilterAttributes("jobId", jobId.toString());
        }
        List<JobExecution> listJobs = jobEngineService.getJobExecutions(jobId);

        Job job = jobEngineService.getJobById(jobId);

        ListingResult<JobExecution> jobsListing = new ListingResult(listJobs, new Metadata(100L, listingParameters));

        List<JobExecutionView> results = jobsListing.getResults().stream()
                .map(jobExecution -> new JobExecutionView(job, jobExecution)).collect(Collectors.toList());

        return new ListingResult<JobExecutionView>(results, jobsListing.getTerms(), jobsListing.getStats(),
                jobsListing.getMetadata());
    }

    @GET
    @Path("/jobs/{jobId}")
    public Job getJob(@PathParam("jobId") Long jobId) {
        return jobEngineService.getJobById(jobId);
    }

    @GET
    @Path("/jobs/{jobId}/activate")
    public void activateJob(@PathParam("jobId") Long jobId) {
        jobEngineService.activateJob(jobId);
    }

    @GET
    @Path("/jobs/{jobId}/deactivate")
    public void deactivateJob(@PathParam("jobId") Long jobId) {
        jobEngineService.deactivateJob(jobId);
    }

    @GET
    @Path("/jobs/{jobId}/executions/{jobExecutionId}")
    public JobExecution getJobExecution(@PathParam("jobId") Long jobId,
            @PathParam("jobExecutionId") Long jobExecutionId) {
        return jobEngineService.getJobExecutionById(jobId, jobExecutionId);
    }

    @GET
    @Path("/jobs/{jobId}/batch/{batchId}")
    public GroupInfo getBatchInfo(@PathParam("jobId") Long jobId, @PathParam("batchId") Long batchId) {
        return jobEngineService.getJobExecutionBatchInfo(jobId, batchId);
    }

    @GET
    @Path("/jobs/{jobId}/batch/{batchId}/executions")
    public List<JobExecution> getBatchJobExecutions(@PathParam("jobId") Long jobId,
            @PathParam("batchId") Long batchId) {
        return jobEngineService.getJobExecutionBatch(jobId, batchId);
    }

    @GET
    @Path("/jobs/{jobId}/chain/{chainId}")
    public GroupInfo getChainInfo(@PathParam("jobId") Long jobId, @PathParam("chainId") Long chainId) {
        return jobEngineService.getJobExecutionChainInfo(jobId, chainId);
    }

    @GET
    @Path("/jobs/{jobId}/chain/{chainId}/executions")
    public List<JobExecution> getChainJobExecutions(@PathParam("jobId") Long jobId,
            @PathParam("chainId") Long chainId) {
        return jobEngineService.getJobExecutionChain(jobId, chainId);
    }

    @POST
    @Path("/jobs/{jobId}/scheduled-job-execution")
    public Job scheduledJobExecutionCreation(@PathParam("jobId") Long jobId, Job job) throws Exception {

        jobEngineService.triggerScheduledJobExecutionCreation(jobEngineService.getJobById(jobId));
        return job;
    }

    @GET
    @Path("/jobs/next-scheduled-times")
    public List<LocalDateTime> getNextScheduledTimes(@QueryParam("schedule") String schedule,
            @QueryParam("times") Integer times, @QueryParam("start") String start) {

        Integer scheduleTimes = times != null ? times : 5;
        LocalDateTime startTime = start != null ? LocalDateTime.parse(start, DateTimeFormatter.ISO_DATE_TIME) : null;

        return jobEngineService.getNextScheduledTimes(schedule, scheduleTimes, startTime);
    }

    @GET
    @Path("/jobs/schedule-executions")
    public List<JobScheduleExecutionTimeDTO> getAllScheduleExecutionTimes(@QueryParam("start") String start,
            @QueryParam("end") String end) {

        LocalDateTime startTime = start != null ? LocalDateTime.parse(start, DateTimeFormatter.ISO_DATE_TIME) : null;
        LocalDateTime endTime = end != null ? LocalDateTime.parse(end, DateTimeFormatter.ISO_DATE_TIME) : null;

        List<JobScheduleExecutionTimeDTO> scheduledTimes = new ArrayList<>();
        for (Job job : jobEngineService.getAllScheduledJobs()) {
            try {
                JobScheduleExecutionTimeDTO dto = new JobScheduleExecutionTimeDTO();
                dto.jobId = job.getId();
                dto.jobName = job.getName();
                dto.schedule = job.getSchedule();
                dto.executions = jobEngineService.getScheduledTimes(job.getSchedule(), startTime, endTime);
                scheduledTimes.add(dto);
            } catch (RuntimeException e) {
            }
        }
        return scheduledTimes;
    }

    // --------------------------------------------------------------------------------------------------------------
    // Deprecated
    // --------------------------------------------------------------------------------------------------------------

    @GET
    @Path("/execution-counts/{minutes}")
    public Object getJobExecutionCount(@PathParam("minutes") Integer minutes) {

        return null;
    }

    @GET
    @Path("/jobs/{jobId}/execution-counts/{minutes}")
    public Object getJobExecutionCountByJob(@PathParam("jobId") Long jobId, @PathParam("minutes") Integer minutes) {
        return null;
    }

}
