package io.coodoo.workhorse.saas;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.coodoo.workhorse.core.control.BaseWorker;

@Path("/client")
@RegisterRestClient
public interface WorkhorseRestClient {

    @GET
    @Path("/baseworker/{workerClassName}")
    @Produces("application/json")
    BaseWorker getWorker(@PathParam("workerClassName") String workerClassName);

    @POST
    @Path("/do-work/{workerClassName}")
    @Produces("application/json")
    public void doWork(@PathParam("workerClassName") String workerClassName, DoWorkParameters params);
}
