package io.coodoo.workhorse.core.control;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.coodoo.workhorse.core.entity.Execution;
import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.core.entity.JobBufferStatus;
import io.coodoo.workhorse.core.entity.JobStatus;
import io.coodoo.workhorse.core.entity.WorkhorseInfo;

/**
 * Class that is used as buffer that contains a subset of executions and their metadata like the corresponding {@link JobThread}
 * 
 * @author coodoo GmbH (coodoo.io)
 */
@ApplicationScoped
public class ExecutionBuffer {

    private static final Logger log = LoggerFactory.getLogger(ExecutionBuffer.class);

    @Inject
    WorkhorseController workhorseController;

    private Map<Long, Queue<Long>> executions = new HashMap<>();
    private Map<Long, Queue<Long>> priorityExecutions = new HashMap<>();
    private Map<Long, Set<Long>> runningExecutions = new HashMap<>();
    private Map<Long, Set<JobThread>> jobThreads = new HashMap<>();
    private Map<Long, Set<CompletionStage<Job>>> completionStages = new HashMap<>();
    private Map<Long, Integer> runningJobThreadCounts = new HashMap<>();
    private Map<Long, Integer> jobThreadCounts = new HashMap<>();
    private Map<Long, Long> jobStartTimes = new HashMap<>();
    private Map<Long, ReentrantLock> jobLocks = new ConcurrentHashMap<>();
    private ReentrantLock myLock = new ReentrantLock();

    public void initialize() {

        clear();

        for (Job job : workhorseController.getAllJobsByStatus(JobStatus.ACTIVE)) {
            initialize(job);
        }
        log.trace("Queue initialize!");
    }

    public void initialize(Job job) {

        jobThreads.put(job.getId(), new HashSet<>());
        executions.put(job.getId(), new ConcurrentLinkedQueue<>());
        priorityExecutions.put(job.getId(), new ConcurrentLinkedQueue<>());
        runningExecutions.put(job.getId(), new HashSet<>());
        jobThreadCounts.put(job.getId(), job.getThreads());
        runningJobThreadCounts.put(job.getId(), 0);
        completionStages.put(job.getId(), new HashSet<>());
    }

    /**
     * Get the status (executions, threads, CompletionStages) of the buffer of the given job
     * 
     * @param job job whose status has to be retrieve
     * @return JobBufferStatus
     */
    public JobBufferStatus getJobBufferStatus(Job job) {

        if (job == null) {
            return null;
        }

        Queue<Long> jobsExecutions = executions.get(job.getId());
        Queue<Long> jobsPriorityExecutions = priorityExecutions.get(job.getId());
        Set<Long> jobsRunningExecutions = runningExecutions.get(job.getId());
        Set<JobThread> threads = jobThreads.get(job.getId());
        Set<CompletionStage<Job>> jobsCompletionStages = completionStages.get(job.getId());
        Integer jobsRunningJobThreadCounts = runningJobThreadCounts.get(job.getId());
        Integer jobsThreadCounts = jobThreadCounts.get(job.getId());

        return new JobBufferStatus(jobsExecutions, jobsPriorityExecutions, jobsRunningExecutions, threads, jobsCompletionStages, jobsRunningJobThreadCounts,
                        jobsThreadCounts);
    }

    public void clear() {

        jobThreads.clear();
        executions.clear();
        priorityExecutions.clear();
        runningExecutions.clear();
        jobThreadCounts.clear();
        runningJobThreadCounts.clear();
        completionStages.clear();
    }

    /**
     * Clean up the buffers of all jobs.
     * 
     */
    public void clearMemoryQueue() {
        for (Job job : workhorseController.getAllJobsByStatus(JobStatus.ACTIVE)) {
            clearMemoryQueue(job);
        }
    }

    /**
     * Clean up the buffer of a job, that contents executions to process.
     * 
     * @param job job, which buffer has to be cleaned
     */
    public void clearMemoryQueue(Job job) {
        log.trace("The Processing of the job {} will be cancel", job);

        if (!executions.containsKey(job.getId()) || !priorityExecutions.containsKey(job.getId())) {
            log.warn("Queue is missing for job: {} ", job);
            return;
        }

        int sizeMemoryQueue = executions.get(job.getId()).size();
        int sizePriorityMemoryQueue = priorityExecutions.get(job.getId()).size();

        if (sizeMemoryQueue > 0 || sizePriorityMemoryQueue > 0) {

            log.trace("Clearing job execution queue with {} elements and {} priority elements for job: {}", executions.get(job.getId()).size(),
                            priorityExecutions.get(job.getId()).size(), job.getName());

            executions.get(job.getId()).clear();
            priorityExecutions.get(job.getId()).clear();
        }
    }

    /**
     * Stop the execution of jobs of the given Job
     * 
     * @param job to cancel
     */
    public void cancelProcess(Job job) {

        clearMemoryQueue(job);
        stopAllJobThread(job);
    }

    /**
     * Stop all thread linked to a given job
     * 
     * @param job
     */
    public void stopAllJobThread(Job job) {

        if (jobThreads.containsKey(job.getId()) && jobThreads.get(job.getId()).isEmpty()) {
            log.trace("Process cancelled. All job threads and job executions removed.");
        } else {

            for (JobThread jobThread : this.jobThreads.get(job.getId())) {

                // Stop the process
                jobThread.stop();
            }

            jobThreads.get(job.getId()).clear();
        }

        runningJobThreadCounts.replace(job.getId(), 0);

        if (!completionStages.containsKey(job.getId())) {
            return;
        }

        for (CompletionStage<Job> completion : completionStages.get(job.getId())) {

            // Stop the thread. The thread is not really stopped. The link with the thread is just broken.
            completion.toCompletableFuture().cancel(true);
        }

        completionStages.get(job.getId()).clear();
    }

    public void removeFromBuffer(Execution execution) {

        Long jobId = execution.getJobId();
        Long executionId = execution.getId();

        if (runningExecutions.containsKey(jobId) && runningExecutions.get(jobId).contains(executionId)) {
            log.warn("Can't remove running job execution from queue: {} ", execution);

        } else if (executions.containsKey(jobId) && executions.get(jobId).remove(executionId)) {
            log.trace("Removed from queue: {} ", execution);

        } else if (priorityExecutions.containsKey(jobId) && priorityExecutions.get(jobId).remove(executionId)) {
            log.trace("Removed from priority queue: {}", execution);
        }
    }

    public int getNumberOfExecution(Long jobId) {
        return executions.get(jobId).size() + runningExecutions.get(jobId).size();
    }

    public ReentrantLock getLock(Long jobId) {
        ReentrantLock keyLock = jobLocks.get(jobId);

        if (keyLock == null) {
            myLock.lock();
            try {
                keyLock = jobLocks.get(jobId);
                if (keyLock == null) {
                    keyLock = new ReentrantLock();
                    jobLocks.put(jobId, keyLock);
                }
            } finally {
                myLock.unlock();
            }
        }
        return keyLock;
    }

    public Map<Long, Queue<Long>> getExecutions() {
        return executions;
    }

    public void addExecution(Long jobId, Long execution) {
        executions.get(jobId).add(execution);
    }

    public Long pollExecutionQueue(Long jobId) {
        return executions.get(jobId).poll();
    }

    public Map<Long, Queue<Long>> getPriorityExecutions() {
        return priorityExecutions;
    }

    public void addPriorityExecution(Long jobId, Long execution) {
        priorityExecutions.get(jobId).add(execution);
    }

    public Long pollPriorityExecutionQueue(Long jobId) {
        return priorityExecutions.get(jobId).poll();
    }

    public Map<Long, Set<Long>> getRunningExecutions() {
        return runningExecutions;
    }

    /**
     * This function check, if a job execution can be process. The existence of a ExecutionQueueBuffer for the given <code>jobId</code> is checked. It will be
     * check, if the ExecutionQueueBuffer do not contain the given <code>executionId</code>
     * 
     * @param jobId
     * @param executionId
     * @return
     */
    public boolean isAddable(Long jobId, Long executionId) {

        if (runningExecutions.get(jobId) == null || executions.get(jobId) == null || priorityExecutions.get(jobId) == null) {
            log.error("They are not ExecutionQueue for the job with Id {} ", jobId);
            return false;
        }
        if (runningExecutions.get(jobId).contains(executionId) || executions.get(jobId).contains(executionId)
                        || priorityExecutions.get(jobId).contains(executionId)) {
            return false;
        }
        return true;
    }

    public void addRunningExecution(Long jobId, Long execution) {
        runningExecutions.get(jobId).add(execution);
    }

    public void removeRunningExecution(Long jobId, Long execution) {
        runningExecutions.get(jobId).remove(execution);
    }

    public void addJobStartTimes(Long jobId, Long startTime) {
        jobStartTimes.put(jobId, startTime);
    }

    public Long getJobStartTimes(Long jobId) {
        return jobStartTimes.get(jobId);
    }

    public void removeJobStartTimes(Long jobId) {
        jobStartTimes.remove(jobId);
    }

    public Map<Long, Set<JobThread>> getJobThreads() {
        return jobThreads;
    }

    public Set<JobThread> getJobThreads(Long jobId) {
        log.trace("Number of Threads: {} ", jobThreads.get(jobId).size());
        return jobThreads.get(jobId);
    }

    public void addJobThreads(Long jobId, JobThread jobThread) {
        log.trace("Add JobThread in Queue: {} ", jobThread);
        jobThreads.get(jobId).add(jobThread);
    }

    public void removeJobThread(Long jobId, JobThread jobThread) {
        log.trace("Remove Thread: {}", jobThread);
        jobThreads.get(jobId).remove(jobThread);
    }

    public int getRunningJobThreadCounts(Long jobId) {
        return runningJobThreadCounts.get(jobId);
    }

    public void addRunningJobThreadCounts(Long jobId) {
        int actualCount = runningJobThreadCounts.get(jobId);
        runningJobThreadCounts.replace(jobId, actualCount + 1);
    }

    public void removeRunningJobThreadCounts(Long jobId) {
        int actualCount = runningJobThreadCounts.get(jobId);
        runningJobThreadCounts.replace(jobId, actualCount - 1);
    }

    public void addCompletionStage(Long jobId, CompletionStage<Job> completion) {
        completionStages.get(jobId).add(completion);
    }

    public Set<CompletionStage<Job>> getCompletionStage(Long jobId) {
        return completionStages.get(jobId);
    }

    public void removeCompletionStage(Long jobId, CompletionStage<Job> completion) {
        completionStages.get(jobId).remove(completion);
    }

    public int getJobThreadCounts(Long jobId) {
        return jobThreadCounts.get(jobId);
    }

    public ReentrantLock getJobLocks(Long jobId) {
        return jobLocks.get(jobId);
    }

    public ReentrantLock getMyLock() {
        return myLock;
    }

    /**
     * Retrieves all info about the ExecutionQueue of a job
     * 
     * @param jobId Id of the Job
     * @return WorkhorseInfo
     */
    public WorkhorseInfo getInfo(Long jobId) {

        WorkhorseInfo info = new WorkhorseInfo();
        info.setJobId(jobId);

        if (executions != null && executions.get(jobId) != null) {
            info.setQueuedExecutions(executions.get(jobId).size());
        }
        if (priorityExecutions != null && priorityExecutions.get(jobId) != null) {
            info.setQueuedPriorityExecutions(priorityExecutions.get(jobId).size());
        }
        if (runningExecutions != null && runningExecutions.get(jobId) != null) {
            for (Long executionId : runningExecutions.get(jobId)) {
                info.getRunningExecutions().add(workhorseController.getExecutionById(jobId, executionId));
            }
        }
        if (jobThreadCounts != null && jobThreadCounts.get(jobId) != null) {
            info.setThreadCount(jobThreadCounts.get(jobId));
        }
        if (jobStartTimes != null && jobStartTimes.get(jobId) != null) {
            info.setThreadStartTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(jobStartTimes.get(jobId)), ZoneId.of(StaticConfig.TIME_ZONE)));
        }
        // if (pausedJobs != null && pausedJobs.get(jobId) != null) {
        // info.setPaused(pausedJobs.get(jobId));
        // }
        return info;
    }

}
