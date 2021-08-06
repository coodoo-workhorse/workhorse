package io.coodoo.workhorse.core.boundary.job;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.coodoo.workhorse.core.boundary.Worker;
import io.coodoo.workhorse.core.boundary.WorkhorseService;
import io.coodoo.workhorse.core.boundary.annotation.InitialJobConfig;
import io.coodoo.workhorse.core.control.JobThread;
import io.coodoo.workhorse.core.entity.ExecutionStatus;
import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.core.entity.JobExecutionCount;
import io.coodoo.workhorse.core.entity.JobStatus;
import io.coodoo.workhorse.persistence.interfaces.listing.ListingParameters;
import io.coodoo.workhorse.util.CollectionListing;
import io.coodoo.workhorse.util.WorkhorseUtil;

/**
 * Check queued executions that are no longer executed.
 * 
 * This class is used until we find out why executions are not processed under load.
 */
@ApplicationScoped
@InitialJobConfig(name = "Check queued executions", schedule = "0 */5 * * * *", failRetries = 1,
                description = "Check for stuck queued executions that are no longer executed.", tags = "System")
public class CheckQueuedExecutionsWorker extends Worker {

    private final Logger logger = LoggerFactory.getLogger(CheckQueuedExecutionsWorker.class);

    private static final long MIN_NUMBER_OF_QUEUED_EXECUTION = 1000L;

    private static final long DURATION_OF_EXECUTIONS_IN_STATUS_QUEUED_IN_MINUTE = 10;

    @Inject
    WorkhorseService workhorseService;

    @Override
    public void doWork() throws Exception {

        logInfo(logger, "Starting check queued Execution.");
        logInfo(logger, "Queued executions | Threads | Job's name");

        // Get only jobs with the status ACTIVE
        List<Job> activeJobs = workhorseController.getAllJobsByStatus(JobStatus.ACTIVE);

        for (Job job : activeJobs) {

            // Here are the variables that are used to save some states that describes the queue of a job.
            int areThereRunningExecutions = 0;
            int areThereJobThreads = 0;
            int areExecutionNewInQueue = 0;

            LocalDateTime to = WorkhorseUtil.timestamp();
            LocalDateTime from = to.minusHours(24);

            // Count the executions of this job by execution's status
            JobExecutionCount jobExecutionCount = workhorseService.getJobExecutionCount(job.getId(), from, to);

            // The problem does not occur under a number of executions. An high arbitrary amount is here set.
            if (jobExecutionCount.getQueued() < MIN_NUMBER_OF_QUEUED_EXECUTION) {
                continue;
            }

            // Check if they aren't running execution of this job.
            if (jobExecutionCount.getRunning() < 1) {
                areThereRunningExecutions = 1;
            }

            // Check if they are no job thread for this job
            Set<JobThread> jobThreads = workhorseService.getJobThreads(job);
            if (jobThreads == null || jobThreads.isEmpty()) {
                areThereJobThreads = 1;
            }

            // Check if more queued executions than the defined limit <MIN_NUMBER_OF_QUEUED_EXECUTION> were created many minutes ago.
            ListingParameters listingParameters = new ListingParameters(0);
            listingParameters.addFilterAttributes("status", ExecutionStatus.QUEUED);
            listingParameters.addFilterAttributes("createdAt",
                            CollectionListing.OPERATOR_LT + WorkhorseUtil.timestamp().minusMinutes(DURATION_OF_EXECUTIONS_IN_STATUS_QUEUED_IN_MINUTE));
            long numberOfOldQueuedExecutions = workhorseService.getExecutionListing(job.getId(), listingParameters).getMetadata().getCount();

            if (numberOfOldQueuedExecutions > MIN_NUMBER_OF_QUEUED_EXECUTION) {
                areExecutionNewInQueue = 1;
            }

            logInfo(logger, String.format("%17d | %7d | %s", jobExecutionCount.getQueued(), jobThreads == null ? 0 : jobThreads.size(), job.getName()));

            // If at least 2/3 conditions are reached, send an email.
            if ((areThereRunningExecutions + areThereJobThreads + areExecutionNewInQueue) >= 2) {
                // TODO Send Email !!

                String message = "Executions of the job: " + job.getName() + " with ID: " + job.getId() + " are no longer processed.";
                logWarn(logger, message);
                executionContext.summerize(message);
                workhorseLogService.logMessage(message, job.getId(), false);

                // Try to restart the job.
                workhorseLogService.logMessage("Try to restart the job", job.getId(), false);
                workhorseService.deactivateJob(job.getId());
                workhorseService.activateJob(job.getId());
            }

        }

        logInfo(logger, "Finished hunt queued executions.");
    }

}
