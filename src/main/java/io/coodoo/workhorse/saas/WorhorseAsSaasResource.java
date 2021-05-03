package io.coodoo.workhorse.saas;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.coodoo.workhorse.core.boundary.WorkhorseLogService;
import io.coodoo.workhorse.core.boundary.WorkhorseService;
import io.coodoo.workhorse.core.control.ExecutionBuffer;
import io.coodoo.workhorse.core.control.JobScheduler;
import io.coodoo.workhorse.core.control.Workhorse;
import io.coodoo.workhorse.core.control.WorkhorseController;
import io.coodoo.workhorse.core.entity.Execution;
import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.persistence.PersistenceManager;

/**
 * @author coodoo GmbH (coodoo.io)
 */
@Path("/workhorse/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorhorseAsSaasResource {

    private static final Logger log = LoggerFactory.getLogger(WorhorseAsSaasResource.class);

    @Inject
    WorkhorseService workhorseService;

    @Inject
    WorkhorseController workhorseController;

    @Inject
    Workhorse workhorse;

    @Inject
    ExecutionBuffer executionBuffer;

    @Inject
    PersistenceManager persistenceManager;

    @Inject
    WorkhorseLogService workhorseLogService;

    @Inject
    JobScheduler jobScheduler;

    @POST
    @Path("/start")
    public Response start(List<ClassMetadata> workerClasses) {
        workhorseService.init();

        workhorseController.loadWorkers(workerClasses);
        executionBuffer.initialize();
        workhorse.start();
        // jobScheduler.startScheduler();

        log.info("Workhorse is running...");

        return Response.ok().build();
    }

    @POST
    @Path("/worker/{workerClassName}/executions")
    public Execution createExecution(@PathParam("workerClassName") String workerClassName, Execution execution) {

        Job job = workhorseController.getByWorkerClassName(workerClassName);
        log.info(" worker class name {}", workerClassName);
        if (job == null) {
            return null;
        }
        return workhorseService.createExecution(job.getId(), execution.getParameters(), execution.isPriority(), execution.getPlannedFor(),
                        execution.getExpiresAt(), execution.getBatchId(), execution.getChainId(), false);
    }
}
