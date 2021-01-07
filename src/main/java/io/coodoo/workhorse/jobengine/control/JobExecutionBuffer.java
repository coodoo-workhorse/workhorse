package io.coodoo.workhorse.jobengine.control;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.coodoo.workhorse.api.DTO.JobEngineInfo;
import io.coodoo.workhorse.config.entity.JobEngineConfig;
import io.coodoo.workhorse.jobengine.entity.Job;
import io.coodoo.workhorse.jobengine.entity.JobExecution;
import io.coodoo.workhorse.jobengine.entity.JobStatus;

@ApplicationScoped
public class JobExecutionBuffer {

    private static final Logger log = Logger.getLogger(JobExecutionBuffer.class);

    @Inject
    JobEngineController jobEngineController;

    @Inject
    JobEngineConfig jobEngineConfig;

    private Map<Long, Queue<Long>> jobExecutions = new HashMap<>();
    private Map<Long, Queue<Long>> priorityJobExecutions = new HashMap<>();
    private Map<Long, Set<Long>> runningJobExecutions = new HashMap<>();
    private Map<Long, Set<JobThread>> jobThreads = new HashMap<>();
    private Map<Long, Set<CompletionStage<Job>>> completionStages = new HashMap<>();
    private Map<Long, Integer> runningJobThreadCounts = new HashMap<>();
    private Map<Long, Integer> jobThreadCounts = new HashMap<>();
    private Map<Long, Long> jobStartTimes = new HashMap<>();
    private Map<Long, ReentrantLock> jobLocks = new ConcurrentHashMap<>();
    private ReentrantLock myLock = new ReentrantLock();

    public void initializeBuffer() {

        destroyQueue();

        for (Job job : jobEngineController.getAllJobsByStatus(JobStatus.ACTIVE)) {
            initializeBuffer(job);
        }

        log.info("Queue initialize !");
    }

    public void initializeBuffer(Job job) {

        jobThreads.put(job.getId(), new HashSet<>());
        jobExecutions.put(job.getId(), new ConcurrentLinkedQueue<>());
        priorityJobExecutions.put(job.getId(), new ConcurrentLinkedQueue<>());
        runningJobExecutions.put(job.getId(), new HashSet<>());
        jobThreadCounts.put(job.getId(), job.getThreads());
        runningJobThreadCounts.put(job.getId(), 0);
        completionStages.put(job.getId(), new HashSet<>());
        
    }

    public void destroyQueue() {

        jobExecutions.clear();
        priorityJobExecutions.clear();
        runningJobExecutions.clear();
        jobThreads.clear();
        jobThreadCounts.clear();
        runningJobThreadCounts.clear();
        completionStages.clear();
    }

    /**
     * Stop the execution of jobs of the given Job
     * 
     * @param job to cancel
     */
    public void cancelProcess(Job job) {
        log.info("The Processing of the job " + job + " will be cancel");

        if (!jobExecutions.containsKey(job.getId()) || !priorityJobExecutions.containsKey(job.getId())) {
            log.warn("Memory queue is missing for job: " + job);
            return;
        }

        int sizeMemoryQueue = jobExecutions.get(job.getId()).size();
        int sizePriorityMemoryQueue = priorityJobExecutions.get(job.getId()).size();

        if (sizeMemoryQueue > 0 || sizePriorityMemoryQueue > 0) {

            log.info("Clearing job execution queue with " + jobExecutions.get(job.getId()).size() + " elements and "
                    + priorityJobExecutions.get(job.getId()).size() + " priority elements for job: " + job.getName());

            jobExecutions.get(job.getId()).clear();
            priorityJobExecutions.get(job.getId()).clear();
        }

        stopAllJobThread(job);
    }

    /**
     * Stop all thread linked with a given job
     * @param job
     */
    public void stopAllJobThread(Job job) {

        if (jobThreads.containsKey(job.getId()) && jobThreads.get(job.getId()).isEmpty()) {
            log.info("Process cancelled. All job threads and job executions removed.");
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

            // Stop the thread
            ((CompletableFuture) completion).cancel(true);
        }

        completionStages.get(job.getId()).clear();
    }

    public void removeFromBuffer(JobExecution jobExecution) {

        Long jobId = jobExecution.getJobId();

        if (runningJobExecutions.containsKey(jobId) && runningJobExecutions.get(jobId).contains(jobExecution)) {
            log.warn("Can't remove running job execution from memory queue: " + jobExecution);

        } else if (jobExecutions.containsKey(jobId) && jobExecutions.get(jobId).remove(jobExecution)) {
            log.info("Removed from memory queue: " + jobExecution);

        } else if (priorityJobExecutions.containsKey(jobId) && priorityJobExecutions.get(jobId).remove(jobExecution)) {
            log.info("Removed from priority memory queue: " + jobExecution);
        }
    }

    public int getNumberOfJobExecution(Long jobId) {
        return jobExecutions.get(jobId).size() + runningJobExecutions.get(jobId).size();
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

    public Map<Long, Queue<Long>> getJobExecutions() {
        return jobExecutions;
    }

    public void addJobExecution(Long jobId, Long jobExecution) {
        jobExecutions.get(jobId).add(jobExecution);
    }

    public Long pollJobExecutionQueue(Long jobId) {
        return jobExecutions.get(jobId).poll();
    }

    public Map<Long, Queue<Long>> getPriorityJobExecutions() {
        return priorityJobExecutions;
    }

    public void addPriorityJobExecution(Long jobId, Long jobExecution) {
        priorityJobExecutions.get(jobId).add(jobExecution);
    }

    public Long pollPriorityJobExecutionQueue(Long jobId) {
        return priorityJobExecutions.get(jobId).poll();
    }

    public Map<Long, Set<Long>> getRunningJobExecutions() {
        return runningJobExecutions;
    }

    /**
     * This function check, if a job execution can be process. The existence of a
     * JobExecutionQueueBuffer for the given <code>jobId</code> is checked. It will
     * be check, if the JobExecutionQueueBuffer do not contain the given
     * <code>jobExecutionId</code>
     * 
     * @param jobId
     * @param jobExecutionId
     * @return
     */
    public boolean canTheJobExecutionBeAdd(Long jobId, Long jobExecutionId) {

        if (runningJobExecutions.get(jobId) == null || jobExecutions.get(jobId) == null
                || priorityJobExecutions.get(jobId) == null) {
            log.error("They are not JobExecutionQueue for the job with Id  " + jobId);
            return false;
        }

        if (runningJobExecutions.get(jobId).contains(jobExecutionId)
                || jobExecutions.get(jobId).contains(jobExecutionId)
                || priorityJobExecutions.get(jobId).contains(jobExecutionId)) {
            return false;
        }

        return true;

    }

    public void addRunningJobExecution(Long jobId, Long jobExecution) {
        runningJobExecutions.get(jobId).add(jobExecution);
    }

    public void removeRunningJobExecution(Long jobId, Long jobExecution) {
        runningJobExecutions.get(jobId).remove(jobExecution);
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

    public Set<JobThread> getJobThreads(Long jobId) {
        log.info("Number of Threads: " + jobThreads.get(jobId).size());
        return jobThreads.get(jobId);
    }

    public void addJobThreads(Long jobId, JobThread jobExecutor) {
        log.info("Add JobThread in Queue: " + jobExecutor);
        jobThreads.get(jobId).add(jobExecutor);
    }

    public void removeJobThread(Long jobId, JobThread jobExecutor) {
        log.info("Remove Thread: " + jobExecutor);
        jobThreads.get(jobId).remove(jobExecutor);
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
     * Retrieves all info about the JobExecutionQueue of a job
     * 
     * @param jobId Id of the Job
     * @return JobEngineInfo
     */
    public JobEngineInfo getInfo(Long jobId) {

        JobEngineInfo info = new JobEngineInfo();
        info.setJobId(jobId);

        if (jobExecutions != null && jobExecutions.get(jobId) != null) {
            info.setQueuedExecutions(jobExecutions.get(jobId).size());
        }
        if (priorityJobExecutions != null && priorityJobExecutions.get(jobId) != null) {
            info.setQueuedPriorityExecutions(priorityJobExecutions.get(jobId).size());
        }
        if (runningJobExecutions != null && runningJobExecutions.get(jobId) != null) {
            for (Long jobExecutionId : runningJobExecutions.get(jobId)) {
                info.getRunningExecutions().add(jobEngineController.getJobExecutionById(jobId, jobExecutionId));
            }
        }
        if (jobThreadCounts != null && jobThreadCounts.get(jobId) != null) {
            info.setThreadCount(jobThreadCounts.get(jobId));
        }
        if (jobStartTimes != null && jobStartTimes.get(jobId) != null) {
            info.setThreadStartTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(jobStartTimes.get(jobId)),
                    ZoneId.of(jobEngineConfig.getTimeZone())));
        }
        // if (pausedJobs != null && pausedJobs.get(jobId) != null) {
        // info.setPaused(pausedJobs.get(jobId));
        // }
        return info;
    }

}