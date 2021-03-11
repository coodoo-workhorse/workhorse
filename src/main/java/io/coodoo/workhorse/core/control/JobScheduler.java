package io.coodoo.workhorse.core.control;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.coodoo.workhorse.core.control.event.JobErrorEvent;
import io.coodoo.workhorse.core.entity.ErrorType;
import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.core.entity.JobStatus;
import io.coodoo.workhorse.util.CronExpression;
import io.coodoo.workhorse.util.WorkhorseUtil;

/**
 * Class that have to execute all scheduled executions.
 * 
 * @author coodoo GmbH (coodoo.io)
 */
@ApplicationScoped
public class JobScheduler {

    private static final Logger log = LoggerFactory.getLogger(JobScheduler.class);

    @Inject
    WorkhorseController workhorseController;

    @Inject
    Event<JobErrorEvent> jobErrorEvent;

    private ScheduledExecutorService scheduledExecutorService;

    private Map<Long, ScheduledFuture<?>> scheduledJobFutures = new HashMap<>();

    @PostConstruct
    public void init() {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    public void startScheduler() {
        for (Job job : workhorseController.getAllScheduledJobs()) {
            start(job);
        }
    }

    public void stopScheduler() {
        for (Job job : workhorseController.getAllScheduledJobs()) {
            stop(job);
        }
    }

    /**
     * start the scheduling of Job
     * 
     * @param job
     */
    public void start(Job job) {

        if (JobStatus.ACTIVE.equals(job.getStatus()) && job.getSchedule() != null && !job.getSchedule().isEmpty()) {
            stop(job);

            try {

                CronExpression cron = new CronExpression(job.getSchedule());
                LocalDateTime nextTime = cron.nextTimeAfter(WorkhorseUtil.timestamp());
                log.trace("next execution of Job: {} on : {} ", job, nextTime);
                long initialDelay = ChronoUnit.SECONDS.between(WorkhorseUtil.timestamp(), nextTime);
                long period = ChronoUnit.SECONDS.between(nextTime, cron.nextTimeAfter(nextTime));
                log.trace("period: {} seconds", period);

                ScheduledFuture<?> scheduledJobFuture = scheduledExecutorService.scheduleAtFixedRate(() -> {
                    triggerScheduledExecutionCreation(job);
                }, initialDelay, period, TimeUnit.SECONDS);

                scheduledJobFutures.put(job.getId(), scheduledJobFuture);

            } catch (Exception e) {
                job.setStatus(JobStatus.ERROR);
                workhorseController.update(job);
                jobErrorEvent.fire(new JobErrorEvent(e, ErrorType.INVALID_SCHEDULE.getMessage(), job.getId(), JobStatus.ERROR));
            }
        }
    }

    /**
     * Stop a scheduled job
     */
    public void stop(Job job) {
        ScheduledFuture<?> scheduledFuture = scheduledJobFutures.get(job.getId());
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);

            log.trace("Schedule stopped for Job {} ", job);
        } else {
            log.trace("No scheduled execution found for the given job {} ", job);
        }

    }

    /**
     * Start an execution after timeout
     */
    public void triggerScheduledExecutionCreation(Job job) {
        log.trace("Timeout with Job: {} ", job);
        try {
            workhorseController.triggerScheduledExecutionCreation(job);
        } catch (Exception exception) {
            log.error("Timeout failed for job {}.", job.getName(), exception);
        }
    }

}
