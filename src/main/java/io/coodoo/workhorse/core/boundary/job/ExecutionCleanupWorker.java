package io.coodoo.workhorse.core.boundary.job;

import java.util.List;

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
            logInfo(logger, "The cleanup is deactivated.");
            return;
        }

        int deletedSum = 0;

        List<Job> jobs = workhorseService.getAllJobs();
        logInfo(logger, "Starting old execution cleanup of " + jobs.size() + " jobs");
        logInfo(logger, "Deleted | Minutes | Job ID | Job Name");

        boolean minOneJobCleanupFailed = false;
        Exception firstException = null;
        String failedJobInfo = "";
        for (Job job : jobs) {
            try {
                int deleted = workhorseController.deleteOlderExecutions(job.getId(), job.getMinutesUntilCleanUp());
                logInfo(logger, String.format("%7d | %4d | %6d | %s", deleted, job.getMinutesUntilCleanUp(), job.getId(), job.getName()));
                deletedSum += deleted;
            } catch (Exception e) {
                minOneJobCleanupFailed = true;
                firstException = e;
                failedJobInfo = job.getName() + " (ID " + job.getId() + ")";
                logError(logger, "Could not delete executions for job (ID " + job.getId() + "): " + e.getMessage(), e);
            }
        }
        logInfo(logger, "Finished execution cleanup. Deleted " + deletedSum + " job executions");

        if (minOneJobCleanupFailed) {
            throw new RuntimeException("Exception during cleaup of job " + failedJobInfo, firstException);
        }
    }

}
