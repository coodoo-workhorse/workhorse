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
import io.coodoo.workhorse.api.dto.LogView;
import io.coodoo.workhorse.core.boundary.WorkhorseLogService;
import io.coodoo.workhorse.core.boundary.WorkhorseService;
import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.core.entity.WorkhorseLog;
import io.coodoo.workhorse.persistence.interfaces.listing.ListingResult;

/**
 * Rest interface for the workhorse logs
 * 
 * @author coodoo GmbH (coodoo.io)
 */
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

        io.coodoo.workhorse.persistence.interfaces.listing.ListingParameters listingParameter2 =
                        new io.coodoo.workhorse.persistence.interfaces.listing.ListingParameters(listingParameters.getPage(), listingParameters.getLimit(),
                                        listingParameters.getSortAttribute());
        listingParameter2.setFilter(listingParameters.getFilter());
        listingParameter2.setFilterAttributes(listingParameters.getFilterAttributes());

        ListingResult<WorkhorseLog> workhorseLogListing = workhorseLogService.getWorkhorseLogListing(listingParameter2);
        return workhorseLogListing;
    }

    @GET
    @Path("/view")
    public ListingResult<LogView> getLogViews(@BeanParam ListingParameters listingParameters) {

        io.coodoo.workhorse.persistence.interfaces.listing.ListingParameters listingParameter2 =
                        new io.coodoo.workhorse.persistence.interfaces.listing.ListingParameters(listingParameters.getPage(), listingParameters.getLimit(),
                                        listingParameters.getSortAttribute());
        listingParameter2.setFilter(listingParameters.getFilter());
        listingParameter2.setFilterAttributes(listingParameters.getFilterAttributes());

        ListingResult<WorkhorseLog> workhorseLogListing = workhorseLogService.getWorkhorseLogListing(listingParameter2);

        List<LogView> results = new ArrayList<>();

        for (WorkhorseLog log : workhorseLogListing.getResults()) {
            Job job = null;
            if (log.getJobId() != null) {
                job = workhorseService.getJobById(log.getJobId());
            }

            results.add(new LogView(log, job));
        }

        return new ListingResult<LogView>(results, workhorseLogListing.getMetadata());
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
