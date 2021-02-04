package io.coodoo.workhorse.api;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.coodoo.workhorse.core.control.WorkhorseConfigService;
import io.coodoo.workhorse.core.entity.WorkhorseConfig;

/**
 * @deprecated gehört nicht in den core, wird erstmal gelassen, zwecks test
 * @author coodoo GmbH (coodoo.io)
 */
@Deprecated
@Path("/workhorse/config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorkhorseConfigRessource {

    @Inject
    WorkhorseConfigService workhorseConfigService;

    @GET
    @Path("/")
    public WorkhorseConfig get() {
        return workhorseConfigService.getWorkhorseConfig();

    }

    @PUT
    @Path("/")
    public WorkhorseConfig update(WorkhorseConfig jobEngineConfig) {

        return workhorseConfigService.updateWorkhorseConfig(jobEngineConfig);
    }

    @PUT
    @Path("/persistence")
    public void changePersistence(WorkhorseConfig jobEngineConfig) {
        workhorseConfigService.updatePersistenceTyp(jobEngineConfig);
    }
}
