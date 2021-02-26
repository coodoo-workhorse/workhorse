package io.coodoo.workhorse.core.boundary.job;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.coodoo.workhorse.core.boundary.Worker;
import io.coodoo.workhorse.core.boundary.WorkhorseService;
import io.coodoo.workhorse.core.boundary.annotation.InitialJobConfig;
import io.coodoo.workhorse.core.control.StaticConfig;
import io.coodoo.workhorse.core.entity.Job;

// TODO The repetition rate of this job must be able, to be updated by the persisitence.
@ApplicationScoped
@InitialJobConfig(name = "Execution Cleanup", schedule = "0 13 * * * *", failRetries = 1, description = "Deletes old executions from the persistence")
public class ExecutionCleanupWorker extends Worker {

    private final Logger logger = LoggerFactory.getLogger(ExecutionCleanupWorker.class);

    @Inject
    WorkhorseService workhorseService;

    @Override
    public void doWork() throws Exception {

        if (StaticConfig.MINUTES_UNTIL_CLEANUP < 1) {
            logInfo("The cleanup is deactivate.");
            logger.trace("The cleanup is deactivate.");
            return;
        }

        int deletedSum = 0;

        logInfo(logger, "Deleted | Minutes | Job ID | Job Name");

        for (Job job : workhorseService.getAllJobs()) {
            try {
                int deleted = workhorseController.deleteOlderExecutions(job.getId(), job.getMinutesUntilCleanUp());
                logInfo(logger, String.format("%7d | %4d | %6d | %s", deleted, job.getMinutesUntilCleanUp(),
                        job.getId(), job.getName()));
                deletedSum += deleted;
            } catch (Exception e) {
                logger.error("Could not delete executions for job (ID " + job.getId() + ") ': " + e.getMessage(), e);
            }
        }
        logInfo(logger, "Deleted " + deletedSum + " job executions");
    }

}
