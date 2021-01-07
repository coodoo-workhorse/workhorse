package io.coodoo.workhorse.jobengine.boundary.job;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.coodoo.workhorse.jobengine.boundary.JobEngineService;
import io.coodoo.workhorse.jobengine.boundary.jobWorkers.JobWorker;
import io.coodoo.workhorse.jobengine.control.annotations.InitialJobConfig;
import io.coodoo.workhorse.jobengine.entity.Job;

@ApplicationScoped
@InitialJobConfig(name = "Job Execution Cleanup", schedule = "15 * * * * *", failRetries = 1, description = "Deletes old job executions from the storage")
public class JobExecutionCleanupJobWorker extends JobWorker {

    private final Logger logger = Logger.getLogger(JobExecutionCleanupJobWorker.class);

    @Inject
    JobEngineService jobEngineService;
    
    @Override
    public void doWork() throws Exception {
        int deletedSum = 0;

        logInfo(logger, "Deleted | Days | Job ID | Job Name");

        for (Job job : jobEngineService.getAllJobs()) {
            if (job.getDaysUntilCleanUp() > 0) {
                try {
                    int deleted = jobEngineController.deleteOlderJobExecutions(job.getId(), job.getDaysUntilCleanUp());
                    logInfo(logger, String.format("%7d | %4d | %6d | %s", deleted, job.getDaysUntilCleanUp(),
                            job.getId(), job.getName()));
                    deletedSum += deleted;
                } catch (Exception e) {
                    logger.error("Could not delete executions for job (ID " + job.getId() + ") ': " + e.getMessage(),
                            e);
                }
            } else {
                logInfo(logger, String.format("      - |    - | %6d | %s", job.getId(), job.getName()));
            }
        }
        logInfo(logger, "Deleted " + deletedSum + " job executions");
    }

}
