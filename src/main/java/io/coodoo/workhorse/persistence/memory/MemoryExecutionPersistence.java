package io.coodoo.workhorse.persistence.memory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import io.coodoo.workhorse.core.control.StaticConfig;
import io.coodoo.workhorse.core.control.event.NewExecutionEvent;
import io.coodoo.workhorse.core.entity.Execution;
import io.coodoo.workhorse.core.entity.ExecutionFailStatus;
import io.coodoo.workhorse.core.entity.ExecutionLog;
import io.coodoo.workhorse.core.entity.ExecutionStatus;
import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.core.entity.JobExecutionCount;
import io.coodoo.workhorse.core.entity.JobExecutionStatusSummary;
import io.coodoo.workhorse.persistence.interfaces.ExecutionPersistence;
import io.coodoo.workhorse.persistence.interfaces.listing.ListingParameters;
import io.coodoo.workhorse.persistence.interfaces.listing.ListingResult;
import io.coodoo.workhorse.util.CollectionListing;
import io.coodoo.workhorse.util.WorkhorseUtil;

@ApplicationScoped
public class MemoryExecutionPersistence implements ExecutionPersistence {

    @Inject
    MemoryPersistence memoryPersistence;

    @Inject
    Event<NewExecutionEvent> newExecutionEventEvent;

    private AtomicLong executionId = new AtomicLong(1);

    @Override
    public String getPersistenceName() {
        return MemoryConfig.NAME;
    }

    @Override
    public Execution getById(Long jobId, Long executionId) {

        if (jobId == null) {
            Execution execution = null;
            for (Long id : memoryPersistence.getJobs().keySet()) {
                Execution foundexecution = memoryPersistence.getJobDataMap().get(id).executions.get(executionId);
                if (foundexecution != null) {
                    execution = foundexecution;
                }
            }
            return execution;
        }
        return memoryPersistence.getJobDataMap().get(jobId).executions.get(executionId);
    }

    @Override
    public ListingResult<Execution> getExecutionListing(Long jobId, ListingParameters listingParameters) {

        Collection<Execution> exeuctions = memoryPersistence.getJobDataMap().get(jobId).executions.values();
        return CollectionListing.getListingResult(exeuctions, Execution.class, listingParameters);
    }

    @Override
    public Execution persist(Execution execution) {

        Long id = executionId.getAndIncrement();
        execution.setId(id);
        execution.setCreatedAt(WorkhorseUtil.timestamp());

        JobData jobData = memoryPersistence.getJobDataMap().get(execution.getJobId());
        jobData.executions.put(id, execution);
        jobData.orderedIds.add(id);

        newExecutionEventEvent.fireAsync(new NewExecutionEvent(execution));
        return execution;
    }

    @Override
    public List<Execution> getByJobId(Long jobId, Long limit) {
        ListingParameters listingParameters = new ListingParameters(limit.intValue());
        ListingResult<Execution> listingResult = getExecutionListing(jobId, listingParameters);
        return listingResult.getResults();
    }

    @Override
    public List<Execution> pollNextExecutions(Long jobId, int limit) {

        List<Execution> executions = new ArrayList<>();
        LocalDateTime currentTimeStamp = LocalDateTime.now(ZoneId.of(StaticConfig.TIME_ZONE));

        for (Execution execution : memoryPersistence.getJobDataMap().get(jobId).executions.values()) {

            if (execution.getJobId().equals(jobId) && (execution.getStatus() == ExecutionStatus.QUEUED || execution.getStatus() == ExecutionStatus.PLANNED)
                            && (execution.getPlannedFor() == null || execution.getPlannedFor().isBefore(currentTimeStamp))
                            && (execution.getChainId() == null || execution.getId().equals(execution.getChainId())) && executions.size() < limit) {

                executions.add(execution);
            }
        }

        Comparator<Execution> sortByPriority = (Execution e1, Execution e2) -> Boolean.compare(e1.isPriority(), e2.isPriority());
        Collections.sort(executions, sortByPriority);

        return executions;
    }

    @Override
    public boolean isPusherAvailable() {
        return true;
    }

    @Override
    public Execution update(Execution execution) {

        execution.setUpdatedAt(WorkhorseUtil.timestamp());

        if (memoryPersistence.getJobDataMap().get(execution.getJobId()).executions.put(execution.getId(), execution) == null) {
            return null;
        }
        return execution;
    }

    @Override
    public Execution updateStatus(Long jobId, Long executionId, ExecutionStatus status, ExecutionFailStatus failStatus) {

        Execution execution = memoryPersistence.getJobDataMap().get(jobId).executions.get(executionId);

        execution.setStatus(status);
        if (failStatus != null) {
            execution.setFailStatus(failStatus);
        }
        return update(execution);
    }

    @Override
    public boolean isBatchFinished(Long jobId, Long batchId) {
        return getQueuedBatchExecution(jobId, batchId) == null ? true : false;
    }

    private Execution getQueuedBatchExecution(Long jobId, Long batchId) {

        ListingParameters listingParameters = new ListingParameters(1);
        listingParameters.addFilterAttributes("status", ExecutionStatus.QUEUED);
        listingParameters.addFilterAttributes("batchId", batchId);

        ListingResult<Execution> executionListing = getExecutionListing(jobId, listingParameters);

        if (executionListing.getResults().isEmpty()) {
            return null;
        }
        return executionListing.getResults().get(0);
    }

    @Override
    public boolean abortChain(Long jobId, Long chainId) {

        ListingParameters listingParameters = new ListingParameters(0);
        listingParameters.addFilterAttributes("status", ExecutionStatus.QUEUED);
        listingParameters.addFilterAttributes("chainId", chainId);

        ListingResult<Execution> executionListing = getExecutionListing(jobId, listingParameters);

        for (Execution execution : executionListing.getResults()) {
            execution.setStatus(ExecutionStatus.FAILED);
            update(execution);
        }
        return true;
    }

    @Override
    public List<Execution> getBatch(Long jobId, Long batchId) {

        ListingParameters listingParameters = new ListingParameters(0);
        listingParameters.addFilterAttributes("batchId", batchId);

        ListingResult<Execution> executionListing = getExecutionListing(jobId, listingParameters);

        return executionListing.getResults();
    }

    @Override
    public List<Execution> getChain(Long jobId, Long chainId) {

        ListingParameters listingParameters = new ListingParameters(0);
        listingParameters.addFilterAttributes("chainId", chainId);
        listingParameters.setSortAttribute(CollectionListing.SORT_ASC + "createdAt");

        return getExecutionListing(jobId, listingParameters).getResults();
    }

    @Override
    public void delete(Long jobId, Long executionId) {

        JobData jobData = memoryPersistence.getJobDataMap().get(jobId);
        jobData.executions.remove(executionId);
        jobData.executionLogs.remove(executionId);
        jobData.orderedIds.remove(executionId);
    }

    @Override
    public int deleteOlderExecutions(Long jobId, LocalDateTime preDate) {

        long millis = WorkhorseUtil.toEpochMilli(preDate);

        ListingParameters listingParameters = new ListingParameters(0);
        listingParameters.addFilterAttributes("createdAt", CollectionListing.OPERATOR_LT + millis);

        ListingResult<Execution> executionListing = getExecutionListing(jobId, listingParameters);

        for (Execution execution : executionListing.getResults()) {
            delete(jobId, execution.getId());
        }
        return executionListing.getResults().size();
    }

    @Override
    public Execution getFirstCreatedByJobIdAndParametersHash(Long jobId, Integer parameterHash) {

        ListingParameters listingParameters = new ListingParameters(1);
        listingParameters.addFilterAttributes("status", ExecutionStatus.QUEUED);
        listingParameters.addFilterAttributes("parametersHash", parameterHash);

        ListingResult<Execution> executionListing = getExecutionListing(jobId, listingParameters);

        if (executionListing.getResults().isEmpty()) {
            return null;
        }
        return executionListing.getResults().get(0);
    }

    @Override
    public List<Execution> findTimeoutExecutions(LocalDateTime time) {

        List<Execution> executions = new ArrayList<>();
        long millis = WorkhorseUtil.toEpochMilli(time);

        for (Long jobId : memoryPersistence.getJobDataMap().keySet()) {

            ListingParameters listingParameters = new ListingParameters(0);
            listingParameters.addFilterAttributes("status", ExecutionStatus.RUNNING);

            listingParameters.addFilterAttributes("startedAt", CollectionListing.OPERATOR_LT + millis);

            ListingResult<Execution> executionListing = getExecutionListing(jobId, listingParameters);

            executions.addAll(executionListing.getResults());
        }
        return executions;
    }

    @Override
    public ExecutionLog getLog(Long jobId, Long executionId) {

        return memoryPersistence.getJobDataMap().get(jobId).executionLogs.get(executionId);
    }

    @Override
    public void log(Long jobId, Long executionId, String log) {

        JobData jobData = memoryPersistence.getJobDataMap().get(jobId);
        ExecutionLog executionLog = jobData.executionLogs.get(executionId);

        if (executionLog == null) {

            executionLog = new ExecutionLog();
            executionLog.setId(executionId);
            executionLog.setExecutionId(executionId);
            executionLog.setCreatedAt(WorkhorseUtil.timestamp());
            executionLog.setLog(log);

        } else {

            executionLog.setLog(executionLog.getLog() + System.lineSeparator() + log);
            executionLog.setUpdatedAt(WorkhorseUtil.timestamp());
        }
        jobData.executionLogs.put(executionId, executionLog);
    }

    @Override
    public void log(Long jobId, Long executionId, String error, String stacktrace) {

        JobData jobData = memoryPersistence.getJobDataMap().get(jobId);
        ExecutionLog executionLog = jobData.executionLogs.get(executionId);

        if (executionLog == null) {

            executionLog = new ExecutionLog();
            executionLog.setId(executionId);
            executionLog.setExecutionId(executionId);
            executionLog.setCreatedAt(WorkhorseUtil.timestamp());
        } else {
            executionLog.setUpdatedAt(WorkhorseUtil.timestamp());
        }

        executionLog.setError(error);
        executionLog.setStacktrace(stacktrace);

        jobData.executionLogs.put(executionId, executionLog);
    }

    @Override
    public List<JobExecutionStatusSummary> getJobExecutionStatusSummaries(ExecutionStatus status, LocalDateTime since) {

        List<JobExecutionStatusSummary> result = new ArrayList<>();

        for (Job job : memoryPersistence.getJobs().values()) {

            ListingParameters listingParameters = new ListingParameters(0);
            listingParameters.addFilterAttributes("status", status);

            if (since != null) {
                long millis = WorkhorseUtil.toEpochMilli(since);
                listingParameters.addFilterAttributes("createdAt", CollectionListing.OPERATOR_GT + millis);
            }

            int count = getExecutionListing(job.getId(), listingParameters).getResults().size();
            // Only job with executions in the given status are considered
            if (count > 0) {
                result.add(new JobExecutionStatusSummary(status, Long.valueOf(count), job));
            }

        }
        return result;
    }

    @Override
    public JobExecutionCount getJobExecutionCount(Long jobId, LocalDateTime from, LocalDateTime to) {

        Collection<Job> jobs = new ArrayList<>();

        if (jobId == null) {
            jobs.addAll(memoryPersistence.getJobs().values());
        } else {
            jobs.add(memoryPersistence.getJobs().get(jobId));
        }

        long countPlanned = 0L;
        long countRunning = 0L;
        long countFinished = 0L;
        long countFailed = 0L;
        long countAbort = 0L;
        long countQueued = 0L;

        ListingParameters listingParameters = new ListingParameters(0);

        long fromMillis = WorkhorseUtil.toEpochMilli(from);
        long toMillis = WorkhorseUtil.toEpochMilli(to);

        listingParameters.addFilterAttributes("createdAt", fromMillis + CollectionListing.OPERATOR_TO + toMillis);

        for (Job job : jobs) {

            listingParameters.addFilterAttributes("status", ExecutionStatus.PLANNED);
            countPlanned = countPlanned + getExecutionListing(job.getId(), listingParameters).getMetadata().getCount();

            listingParameters.addFilterAttributes("status", ExecutionStatus.QUEUED);
            countQueued = countQueued + getExecutionListing(job.getId(), listingParameters).getMetadata().getCount();

            listingParameters.addFilterAttributes("status", ExecutionStatus.RUNNING);
            countRunning = countRunning + getExecutionListing(job.getId(), listingParameters).getMetadata().getCount();

            listingParameters.addFilterAttributes("status", ExecutionStatus.FINISHED);
            countFinished = countFinished + getExecutionListing(job.getId(), listingParameters).getMetadata().getCount();

            listingParameters.addFilterAttributes("status", ExecutionStatus.FAILED);
            countFailed = countFailed + getExecutionListing(job.getId(), listingParameters).getMetadata().getCount();

            listingParameters.addFilterAttributes("status", ExecutionStatus.ABORTED);
            countAbort = countAbort + getExecutionListing(job.getId(), listingParameters).getMetadata().getCount();
        }

        return new JobExecutionCount(jobId, from, to, countPlanned, countQueued, countRunning, countFinished, countFailed, countAbort);
    }

    @Override
    public void subscribe() {}

    @Override
    public void unsubscribe() {}

}
