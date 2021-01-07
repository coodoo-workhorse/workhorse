package io.coodoo.workhorse.api;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.coodoo.workhorse.config.boundary.JobEngineConfigService;
import io.coodoo.workhorse.config.entity.JobEngineConfig;

@Path("/workhorse/config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class JobEngineConfigRessource {
    
    @Inject
    JobEngineConfigService jobEngineConfigService;

    
    @GET
    @Path("/")
    public JobEngineConfig get() {
        return jobEngineConfigService.getJobEngineConfig();  
         
    }

    @PUT
    @Path("/")
    public JobEngineConfig update(JobEngineConfig jobEngineConfig) {
      
       return jobEngineConfigService.updateJobEngineConfig(jobEngineConfig);
    }

    @PUT
    @Path("/persistence")
    public void changePersistence(JobEngineConfig jobEngineConfig) {
        jobEngineConfigService.updatePersistenceTyp(jobEngineConfig);
    }
}
