package io.coodoo.workhorse.core.entity;

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import io.coodoo.workhorse.core.control.JobThread;

/**
 * <p>
 * A JobBufferStatus defines the status (executions, threads, CompletionStages) of the buffer of a given job at a moment.
 * </p>
 */
public class JobBufferStatus {

    public Integer runningJobThreadCounts;
    public Integer jobThreadCounts;
    public Queue<Long> executions;
    public Queue<Long> priorityExecutions;
    public Set<Long> runningExecutions;
    public Set<JobThread> jobThreads;
    public Set<CompletionStage<Job>> completionStages;

    public JobBufferStatus() {}

    public JobBufferStatus(Queue<Long> executions, Queue<Long> priorityExecutions, Set<Long> runningExecutions, Set<JobThread> jobThreads,
                    Set<CompletionStage<Job>> completionStages, Integer runningJobThreadCounts, Integer jobThreadCounts) {
        this.executions = executions;
        this.priorityExecutions = priorityExecutions;
        this.runningExecutions = runningExecutions;
        this.jobThreads = jobThreads;
        this.completionStages = completionStages;
        this.runningJobThreadCounts = runningJobThreadCounts;
        this.jobThreadCounts = jobThreadCounts;
    }

}
