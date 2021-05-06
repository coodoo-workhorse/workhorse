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
                        execution.getExpiresAt(), execution.getBatchId(), execution.getChainId(), job.isUniqueQueued());
    }

    @POST
    @Path("//worker/{workerClassName}/batch-executions")
    Long createBatchExecutions(@PathParam("workerClassName") String workerClassName, GroupExecutions groupExecutions) {

        Job job = workhorseController.getByWorkerClassName(workerClassName);
        log.info(" worker class name {}", workerClassName);
        if (job == null) {
            return null;
        }

        Long batchId = null;

        for (String parameters : groupExecutions.getParametersList()) {
            if (batchId == null) { // start of batch

                Execution execution = workhorseService.createExecution(job.getId(), parameters, groupExecutions.getPriority(), groupExecutions.getPlannedFor(),
                                groupExecutions.getExpiresAt(), -1L, null, job.isUniqueQueued());
                // Use the Id of the first added job execution in Batch as BatchId.
                execution.setBatchId(execution.getId());
                workhorseController.updateExecution(execution);

                batchId = execution.getId();
            } else { // now that we have the batch id, all the beloning executions can have it!
                workhorseService.createExecution(job.getId(), parameters, groupExecutions.getPriority(), groupExecutions.getPlannedFor(),
                                groupExecutions.getExpiresAt(), batchId, null, job.isUniqueQueued());
            }
        }
        return batchId;
    }

    @POST
    @Path("/worker/{workerClassName}/chain-executions")
    Long createChainedExecutions(@PathParam("workerClassName") String workerClassName, GroupExecutions groupExecutions) {

        Job job = workhorseController.getByWorkerClassName(workerClassName);
        log.info(" worker class name {}", workerClassName);
        if (job == null) {
            return null;
        }

        Long chainId = null;

        for (String parameter : groupExecutions.getParametersList()) {
            if (chainId == null) { // start of chain

                Execution execution = workhorseService.createExecution(job.getId(), parameter, groupExecutions.getPriority(), groupExecutions.getPlannedFor(),
                                groupExecutions.getExpiresAt(), null, -1L, job.isUniqueQueued());
                execution.setChainId(execution.getId());
                workhorseController.updateExecution(execution);

                chainId = execution.getId();
            } else { // now that we have the chain id, all the beloning executions can have it!
                workhorseService.createExecution(job.getId(), parameter, groupExecutions.getPriority(), groupExecutions.getPlannedFor(),
                                groupExecutions.getExpiresAt(), null, chainId, job.isUniqueQueued());
            }
        }
        return chainId;
    }

}
