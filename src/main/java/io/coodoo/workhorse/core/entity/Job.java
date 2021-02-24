package io.coodoo.workhorse.core.entity;

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
    private int maxPerMinute;

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
     * If a job has the uniqueInqueue set <code>true</code>, Two or more job
     * execution with the same parameters are not authorised
     */
    private Boolean uniqueInQueue;

    /**
     * Timstamp as Cron-syntax to schedule the job
     */
    private String schedule;

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
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

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

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

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String scheduler) {
        this.schedule = scheduler;
    }

    public Boolean isUniqueInQueue() {
        return uniqueInQueue;
    }

    public void setUniqueInQueue(Boolean uniqueInQueue) {
        this.uniqueInQueue = uniqueInQueue;
    }

    public int getMaxPerMinute() {
        return maxPerMinute;
    }

    public void setMaxPerMinute(int maxPerMinute) {
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

    public Boolean getUniqueInQueue() {
        return uniqueInQueue;
    }

    @Override
    public String toString() {
        return "Job [Id=" + id + ", description=" + description + ", name=" + name + ", parametersClassName="
                + parametersClassName + ", schedule=" + schedule + ", status=" + status + ", threads=" + threads
                + ", workerClassName=" + workerClassName + "]";
    }

}
