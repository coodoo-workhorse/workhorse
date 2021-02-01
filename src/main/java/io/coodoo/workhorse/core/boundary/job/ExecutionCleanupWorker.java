package io.coodoo.workhorse.core.boundary.job;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.coodoo.workhorse.core.boundary.Worker;
import io.coodoo.workhorse.core.boundary.WorkhorseService;
import io.coodoo.workhorse.core.boundary.annotation.InitialJobConfig;
import io.coodoo.workhorse.core.entity.Job;

@ApplicationScoped
@InitialJobConfig(name = "Execution Cleanup", schedule = "15 * * * * *", failRetries = 1, description = "Deletes old executions from the storage")
public class ExecutionCleanupWorker extends Worker {

    private final Logger logger = Logger.getLogger(ExecutionCleanupWorker.class);

    @Inject
    WorkhorseService workhorseService;

    @Override
    public void doWork() throws Exception {
        int deletedSum = 0;

        logInfo(logger, "Deleted | Days | Job ID | Job Name");

        for (Job job : workhorseService.getAllJobs()) {
            if (job.getDaysUntilCleanUp() > 0) {
                try {
                    int deleted = workhorseController.deleteOlderExecutions(job.getId(), job.getDaysUntilCleanUp());
                    logInfo(logger, String.format("%7d | %4d | %6d | %s", deleted, job.getDaysUntilCleanUp(), job.getId(), job.getName()));
                    deletedSum += deleted;
                } catch (Exception e) {
                    logger.error("Could not delete executions for job (ID " + job.getId() + ") ': " + e.getMessage(), e);
                }
            } else {
                logInfo(logger, String.format("      - |    - | %6d | %s", job.getId(), job.getName()));
            }
        }
        logInfo(logger, "Deleted " + deletedSum + " job executions");
    }

}
