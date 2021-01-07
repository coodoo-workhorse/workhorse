package io.coodoo.workhorse.api;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.coodoo.framework.listing.boundary.ListingParameters;
import io.coodoo.framework.listing.boundary.ListingResult;
import io.coodoo.framework.listing.boundary.Metadata;
import io.coodoo.workhorse.api.DTO.JobEngineLogView;
import io.coodoo.workhorse.jobengine.boundary.JobEngineService;
import io.coodoo.workhorse.jobengine.entity.Job;
import io.coodoo.workhorse.log.boundary.JobEngineLogService;
import io.coodoo.workhorse.log.entity.JobEngineLog;

/**
 * Rest interface for the workhorse logs
 * 
 * @author coodoo GmbH (coodoo.io)
 */
@Path("/workhorse/logs")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class JobEngineLogResource {

    @Inject
    JobEngineLogService jobEngineLogService;

    @Inject
    JobEngineService jobEngineService;

    @GET
    @Path("/")
    public ListingResult<JobEngineLog> getLogs(@BeanParam ListingParameters listingParameters) {
        int limit = 100;
        List<JobEngineLog> listJobs = jobEngineLogService.getAllLogs(limit);

        ListingResult<JobEngineLog> jobsListing = new ListingResult(listJobs,
                new Metadata(Long.valueOf(listJobs.size()), listingParameters));
        return jobsListing;
    }

    @GET
    @Path("/view")
    public ListingResult<JobEngineLogView> getLogViews(@BeanParam ListingParameters listingParameters) {

        int limit = 100;
        List<JobEngineLog> listJobs = jobEngineLogService.getAllLogs(limit);

        ListingResult<JobEngineLog> jobEngineLogsListing = new ListingResult(listJobs,
                new Metadata(Long.valueOf(listJobs.size()), listingParameters));

        List<JobEngineLogView> results = new ArrayList<>();

        for (JobEngineLog log : jobEngineLogsListing.getResults()) {
            Job job = null;
            if (log.getJobId() != null) {
                 job = jobEngineService.getJobById(log.getJobId());    
            }

            results.add(new JobEngineLogView(log, job));
        }

        return new ListingResult<JobEngineLogView>(results, jobEngineLogsListing.getTerms(),
                jobEngineLogsListing.getStats(), jobEngineLogsListing.getMetadata());
    }

    @GET
    @Path("/{id}")
    public JobEngineLog getLog(@PathParam("id") Long id) {
        return jobEngineLogService.getLog(id);
    }

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Path("/")
    public JobEngineLog createLogMessage(String message) {
        return jobEngineLogService.logMessage(message, null, true);
    }

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Path("/{jobId}")
    public JobEngineLog createLogMessage(@PathParam("jobId") Long jobId, String message) {
        return jobEngineLogService.logMessage(message, jobId, true);
    }

}
