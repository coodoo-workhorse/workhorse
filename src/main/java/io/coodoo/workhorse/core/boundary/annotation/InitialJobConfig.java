package io.coodoo.workhorse.core.boundary.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.coodoo.workhorse.core.entity.JobStatus;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface InitialJobConfig {

    public static final int JOB_CONFIG_THREADS = 1;
    public static final int JOB_CONFIG_MAX_PER_MINUTE = 0;
    public static final int JOB_CONFIG_FAIL_RETRIES = 0;
    public static final int JOB_CONFIG_RETRY_DELAY = 4000;
    public static final int JOB_CONFIG_MINUTES_UNTIL_CLEANUP = -1;
    public static final boolean JOB_CONFIG_UNIQUE_IN_QUEUE = false;

    /**
     * @return Unique name of the job
     */
    String name() default "";

    /**
     * @return A readable description of the job purpose.
     */
    String description() default "";

    /**
     * @return A comma-separated tag list to help organize the jobs.
     */
    String tags() default "";

    /**
     * @return Initial Status of the Job. Default is ACTIVE.
     */
    JobStatus status() default JobStatus.ACTIVE;

    /**
     * @return Unix-like CRON expressions to provide scheduled job executions.
     */
    String schedule() default "";

    /**
     * @return Number of threads for processing parallel work. Default is 1.
     */
    int threads() default JOB_CONFIG_THREADS;

    /**
     * @return Limit of execution throughput per minute. Default is null (no limitation)
     */
    int maxPerMinute() default JOB_CONFIG_MAX_PER_MINUTE;

    /**
     * @return Number of retries after the job faild by an exception. Default value is 0 (no retries).
     */
    int failRetries() default JOB_CONFIG_FAIL_RETRIES;

    /**
     * @return Delay to start a retry after a failed job exception. Default is 4000 milliseconds.
     */
    int retryDelay() default JOB_CONFIG_RETRY_DELAY;

    /**
     * @return Number of minutes after the job executions get deleted. The default value depends on the active persistence.
     */
    int minutesUntilCleanUp() default JOB_CONFIG_MINUTES_UNTIL_CLEANUP;

    /**
     * @return If <code>true</code> (default) a new job execution will only be added and saved into the queue if no other job with the same parameters exists in
     *         queue.
     */
    boolean uniqueQueued() default JOB_CONFIG_UNIQUE_IN_QUEUE;
}
