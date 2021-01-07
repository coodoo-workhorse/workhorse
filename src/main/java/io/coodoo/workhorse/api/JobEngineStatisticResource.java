package io.coodoo.workhorse.api;

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.coodoo.framework.listing.boundary.ListingParameters;
import io.coodoo.workhorse.jobengine.boundary.JobEngineService;

/**
 * WARNING: THIS CLASS IS FOR THE MOMENT JUST A MOCK FOR THE UI
 * 
 * Rest interface for the workhorse statistics
 * 
 * @author coodoo GmbH (coodoo.io)
 */
@Path("/workhorse/statistics")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class JobEngineStatisticResource {

    @Inject
    JobEngineService jobEngineService;

    @GET
    @Path("/minutes")
    public Object getJobStatisticsMinutes(@BeanParam ListingParameters listingParameters) {
        return null;
    }

    @GET
    @Path("/hours")
    public Object getJobStatisticsHours(@BeanParam ListingParameters listingParameters) {
        return null;
    }

    @GET
    @Path("/days")
    public Object getJobStatisticsDays(@BeanParam ListingParameters listingParameters) {
        return null;
    }

    @GET
    @Path("/duration-heatmap")
    public Object getDurationHeatmap() {
        return null;
    }

    @GET
    @Path("/duration-heatmap/{jobId}")
    public Object getDurationHeatmap(@PathParam("jobId") Long jobId) {
        return null;
    }

    @GET
    @Path("/memory-counts/{jobId}")
    public Object getMemoryCounts(@PathParam("jobId") Long jobId) {
        return null;
    }
}
