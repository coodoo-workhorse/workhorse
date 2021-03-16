package io.coodoo.workhorse.core.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author coodoo GmbH (coodoo.io)
 */
public class Job extends BaseEntity {

    /**
     * Name of the job
     */
    private String name;

    /**
     * Description about what the job do.
     */
    private String description;

    /**
     * Tags concerning the job.
     */
    private List<String> tags = new ArrayList<>();

    /**
     * Name of the Class that implement the job.
     */
    private String workerClassName;

    /**
     * Name of the Class that the Job use as Parameter
     */
    private String parametersClassName;

    /**
     * Status of the Job. <code>ACTIVE</code>, <code>NO_WORKER</code>
     */
    private JobStatus status;

    /**
     * Number of thread, that can process the job
     */
    private int threads;

    /**
     * Max number of execution per minute
     */
    private Integer maxPerMinute;

    /**
     * number of retries for a failed execution
     */
    private int failRetries;

    /**
     * Duration to wait before a retry
     */
    private int retryDelay;

    /**
     * number of minutes before delete execution of this Job
     */
    private int minutesUntilCleanUp;

    /**
     * If a job has the uniqueInqueue set <code>true</code>, Two or more job execution with the same parameters are not authorised
     */
    private boolean uniqueQueued;

    /**
     * Timstamp as Cron-syntax to schedule the job
     */
    private String schedule;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getWorkerClassName() {
        return workerClassName;
    }

    public void setWorkerClassName(String workerClassName) {
        this.workerClassName = workerClassName;
    }

    public String getParametersClassName() {
        return parametersClassName;
    }

    public void setParametersClassName(String parametersClassName) {
        this.parametersClassName = parametersClassName;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public Integer getMaxPerMinute() {
        return maxPerMinute;
    }

    public void setMaxPerMinute(Integer maxPerMinute) {
        this.maxPerMinute = maxPerMinute;
    }

    public int getFailRetries() {
        return failRetries;
    }

    public void setFailRetries(int failRetries) {
        this.failRetries = failRetries;
    }

    public int getRetryDelay() {
        return retryDelay;
    }

    public void setRetryDelay(int retryDelay) {
        this.retryDelay = retryDelay;
    }

    public int getMinutesUntilCleanUp() {
        return minutesUntilCleanUp;
    }

    public void setMinutesUntilCleanUp(int minutesUntilCleanUp) {
        this.minutesUntilCleanUp = minutesUntilCleanUp;
    }

    public boolean isUniqueQueued() {
        return uniqueQueued;
    }

    public void setUniqueQueued(boolean uniqueQueued) {
        this.uniqueQueued = uniqueQueued;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    @Override
    public String toString() {
        final int maxLen = 10;
        StringBuilder builder = new StringBuilder();
        builder.append("Job [id=");
        builder.append(id);
        builder.append(", name=");
        builder.append(name);
        builder.append(", description=");
        builder.append(description);
        builder.append(", tags=");
        builder.append(tags != null ? tags.subList(0, Math.min(tags.size(), maxLen)) : null);
        builder.append(", workerClassName=");
        builder.append(workerClassName);
        builder.append(", parametersClassName=");
        builder.append(parametersClassName);
        builder.append(", status=");
        builder.append(status);
        builder.append(", threads=");
        builder.append(threads);
        builder.append(", maxPerMinute=");
        builder.append(maxPerMinute);
        builder.append(", failRetries=");
        builder.append(failRetries);
        builder.append(", retryDelay=");
        builder.append(retryDelay);
        builder.append(", minutesUntilCleanUp=");
        builder.append(minutesUntilCleanUp);
        builder.append(", uniqueQueued=");
        builder.append(uniqueQueued);
        builder.append(", schedule=");
        builder.append(schedule);
        builder.append(", createdAt=");
        builder.append(createdAt);
        builder.append(", updatedAt=");
        builder.append(updatedAt);
        builder.append("]");
        return builder.toString();
    }

}
