package io.coodoo.workhorse.saas;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * @author coodoo GmbH (coodoo.io)
 */
@Path("/workhorse/as-client")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorkhorseAsClient {

    @Inject
    @RestClient
    WorkhorseRestClient workhorseRestClient;

    @GET
    @Path("/do-work/{workerClassName}/job/{jobId}/execution/{executionId}")
    public void doWork(@PathParam("workerClassName") String workerClassName, @PathParam("jobId") Long jobId, @PathParam("executionId") Long executionId)
                    throws Exception {

        workhorseRestClient.doWork(workerClassName, jobId, executionId);
    }

}
