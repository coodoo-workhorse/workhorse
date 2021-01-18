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
import io.coodoo.workhorse.api.DTO.LogView;
import io.coodoo.workhorse.core.boundary.WorkhorseLogService;
import io.coodoo.workhorse.core.boundary.WorkhorseService;
import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.core.entity.WorkhorseLog;

/**
 * Rest interface for the workhorse logs
 * 
 * @author coodoo GmbH (coodoo.io)
 * @deprecated gehört nicht in den core, wird erstmal gelassen, zwecks test
 */
@Deprecated
@Path("/workhorse/logs")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class WorkhorseLogResource {

    @Inject
    WorkhorseLogService workhorseLogService;

    @Inject
    WorkhorseService workhorseService;

    @GET
    @Path("/")
    public ListingResult<WorkhorseLog> getLogs(@BeanParam ListingParameters listingParameters) {
        int limit = 100;
        List<WorkhorseLog> listJobs = workhorseLogService.getAllLogs(limit);

        ListingResult<WorkhorseLog> jobsListing = new ListingResult(listJobs, new Metadata(Long.valueOf(listJobs.size()), listingParameters));
        return jobsListing;
    }

    @GET
    @Path("/view")
    public ListingResult<LogView> getLogViews(@BeanParam ListingParameters listingParameters) {

        int limit = 100;
        List<WorkhorseLog> listJobs = workhorseLogService.getAllLogs(limit);

        ListingResult<WorkhorseLog> jobEngineLogsListing = new ListingResult(listJobs, new Metadata(Long.valueOf(listJobs.size()), listingParameters));

        List<LogView> results = new ArrayList<>();

        for (WorkhorseLog log : jobEngineLogsListing.getResults()) {
            Job job = null;
            if (log.getJobId() != null) {
                job = workhorseService.getJobById(log.getJobId());
            }

            results.add(new LogView(log, job));
        }

        return new ListingResult<LogView>(results, jobEngineLogsListing.getTerms(), jobEngineLogsListing.getStats(),
                        jobEngineLogsListing.getMetadata());
    }

    @GET
    @Path("/{id}")
    public WorkhorseLog getLog(@PathParam("id") Long id) {
        return workhorseLogService.getLog(id);
    }

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Path("/")
    public WorkhorseLog createLogMessage(String message) {
        return workhorseLogService.logMessage(message, null, true);
    }

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Path("/{jobId}")
    public WorkhorseLog createLogMessage(@PathParam("jobId") Long jobId, String message) {
        return workhorseLogService.logMessage(message, jobId, true);
    }

}
