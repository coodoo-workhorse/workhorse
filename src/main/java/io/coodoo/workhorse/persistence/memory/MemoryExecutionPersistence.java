package io.coodoo.workhorse.persistence.memory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.coodoo.workhorse.core.control.StaticConfig;
import io.coodoo.workhorse.core.control.event.NewExecutionEvent;
import io.coodoo.workhorse.core.entity.Execution;
import io.coodoo.workhorse.core.entity.ExecutionFailStatus;
import io.coodoo.workhorse.core.entity.ExecutionLog;
import io.coodoo.workhorse.core.entity.ExecutionStatus;
import io.coodoo.workhorse.persistence.interfaces.ExecutionPersistence;
import io.coodoo.workhorse.persistence.interfaces.listing.ListingParameters;
import io.coodoo.workhorse.persistence.interfaces.listing.ListingResult;
import io.coodoo.workhorse.persistence.interfaces.listing.Metadata;
import io.coodoo.workhorse.util.WorkhorseUtil;

@ApplicationScoped
public class MemoryExecutionPersistence implements ExecutionPersistence {

    private static final String DESC = "-";
    private static final String ASC = "+";
    private static final String GT = ">";
    private static final String LT = "<";

    private static Logger log = LoggerFactory.getLogger(MemoryExecutionPersistence.class);

    @Inject
    MemoryPersistence memoryPersistence;

    @Inject
    Event<NewExecutionEvent> newExecutionEventEvent;

    private AtomicLong executionId = new AtomicLong(0);

    @Override
    public Execution getById(Long jobId, Long executionId) {

        return memoryPersistence.getJobDataMap().get(jobId).executions.get(executionId);
    }

    @Override
    public ListingResult<Execution> getExecutionListing(Long jobId, ListingParameters listingParameters) {

        JobData jobData = memoryPersistence.getJobDataMap().get(jobId);

        if (listingParameters.getFilterAttributes().isEmpty()) {

            // TODO sort?!
            Metadata metadata = new Metadata(new Long(jobData.executions.size()), listingParameters);
            List<Long> ids = jobData.orderedIds.subList(metadata.getStartIndex(), metadata.getEndIndex());
            List<Execution> result = ids.stream().map(id -> jobData.executions.get(id)).collect(Collectors.toList());
            return new ListingResult<Execution>(result, metadata);
        }

        List<Predicate<Execution>> allPredicates = new ArrayList<Predicate<Execution>>();

        for (String key : listingParameters.getFilterAttributes().keySet()) {
            String rawvalue = listingParameters.getFilterAttributes().get(key);
            try {
                for (String value : rawvalue.split("|")) {
                    switch (key) {
                        case "status":
                            ExecutionStatus status = ExecutionStatus.valueOf(value);
                            allPredicates.add(execution -> execution.getStatus().equals(status));
                            break;
                        case "failStatus":
                            ExecutionFailStatus failStatus = ExecutionFailStatus.valueOf(value);
                            allPredicates.add(execution -> execution.getFailStatus().equals(failStatus));
                            break;
                        case "id":
                            allPredicates.add(execution -> execution.getId().equals(new Long(value)));
                            break;
                        case "batchId":
                            allPredicates.add(execution -> execution.getBatchId().equals(new Long(value)));
                            break;
                        case "chainId":
                            allPredicates.add(execution -> execution.getChainId().equals(new Long(value)));
                            break;
                        case "startedAt":
                            if (value.startsWith(LT)) {
                                LocalDateTime timestamp = fromIso8601(value.replace(LT, ""));
                                allPredicates.add(execution -> timestamp.isAfter(execution.getStartedAt()));
                            } else if (value.startsWith(GT)) {
                                LocalDateTime timestamp = fromIso8601(value.replace(GT, ""));
                                allPredicates.add(execution -> timestamp.isBefore(execution.getStartedAt()));
                            }
                            break;
                        case "endedAt":
                            if (value.startsWith(LT)) {
                                LocalDateTime timestamp = fromIso8601(value.replace(LT, ""));
                                allPredicates.add(execution -> timestamp.isAfter(execution.getEndedAt()));
                            } else if (value.startsWith(GT)) {
                                LocalDateTime timestamp = fromIso8601(value.replace(GT, ""));
                                allPredicates.add(execution -> timestamp.isBefore(execution.getEndedAt()));
                            }
                            break;
                        case "plannedFor":
                            if (value.startsWith(LT)) {
                                LocalDateTime timestamp = fromIso8601(value.replace(LT, ""));
                                allPredicates.add(execution -> timestamp.isAfter(execution.getPlannedFor()));
                            } else if (value.startsWith(GT)) {
                                LocalDateTime timestamp = fromIso8601(value.replace(GT, ""));
                                allPredicates.add(execution -> timestamp.isBefore(execution.getPlannedFor()));
                            }
                            break;
                        case "expiresAt":
                            if (value.startsWith(LT)) {
                                LocalDateTime timestamp = fromIso8601(value.replace(LT, ""));
                                allPredicates.add(execution -> timestamp.isAfter(execution.getExpiresAt()));
                            } else if (value.startsWith(GT)) {
                                LocalDateTime timestamp = fromIso8601(value.replace(GT, ""));
                                allPredicates.add(execution -> timestamp.isBefore(execution.getExpiresAt()));
                            }
                            break;
                        case "createdAt":
                            if (value.startsWith(LT)) {
                                LocalDateTime timestamp = fromIso8601(value.replace(LT, ""));
                                allPredicates.add(execution -> timestamp.isAfter(execution.getCreatedAt()));
                            } else if (value.startsWith(GT)) {
                                LocalDateTime timestamp = fromIso8601(value.replace(GT, ""));
                                allPredicates.add(execution -> timestamp.isBefore(execution.getCreatedAt()));
                            }
                            break;
                        case "updatedAt":
                            if (value.startsWith(LT)) {
                                LocalDateTime timestamp = fromIso8601(value.replace(LT, ""));
                                allPredicates.add(execution -> timestamp.isAfter(execution.getUpdatedAt()));
                            } else if (value.startsWith(GT)) {
                                LocalDateTime timestamp = fromIso8601(value.replace(GT, ""));
                                allPredicates.add(execution -> timestamp.isBefore(execution.getUpdatedAt()));
                            }
                            break;
                        case "failRetryExecutionId":
                            allPredicates.add(execution -> execution.getFailRetryExecutionId().equals(new Long(value)));
                            break;
                        case "parameters":
                            allPredicates.add(execution -> execution.getParameters().matches(".*" + value + ".*"));
                            break;
                        case "parameterHash":
                            if (value != null && !value.isEmpty()) {
                                allPredicates.add(execution -> execution.getParametersHash().equals(new Integer(value)));
                            } else {
                                allPredicates.add(execution -> execution.getParametersHash() == null);
                            }
                            break;
                        case "failRetry":
                            allPredicates.add(execution -> execution.getFailRetry() == new Integer(value).intValue());
                            break;
                        default:
                            break;
                    }
                }
            } catch (Exception e) {
                log.warn("Execution filter {} with value {} is invalid: {}", key, rawvalue, e.getMessage());
            }
        }

        // TOOD mit orderIds abgleichen
        List<Execution> filteredList =
                        jobData.executions.values().stream().filter(allPredicates.stream().reduce(x -> true, Predicate::and)).collect(Collectors.toList());

        sort(listingParameters, filteredList);

        Metadata metadata = new Metadata(new Long(filteredList.size()), listingParameters);
        List<Execution> result = filteredList.subList(metadata.getStartIndex(), metadata.getEndIndex());

        return new ListingResult<Execution>(result, metadata);
    }

    private void sort(ListingParameters listingParameters, List<Execution> filteredList) {
        String sort = listingParameters.getSortAttribute();
        if (sort != null && !sort.isEmpty()) {
            boolean asc = true;
            if (sort.startsWith(ASC)) {
                sort = sort.replace(ASC, "");
            } else if (sort.startsWith(DESC)) {
                sort = sort.replace(DESC, "");
                asc = false;
            }
            switch (sort) {
                case "id":
                    filteredList.sort(Comparator.comparing(execution -> execution.getId()));
                    break;
                case "jobId":
                    filteredList.sort(Comparator.comparing(execution -> execution.getJobId()));
                    break;
                case "status":
                    filteredList.sort(Comparator.comparing(execution -> execution.getStatus()));
                    break;
                case "failStatus":
                    filteredList.sort(Comparator.comparing(execution -> execution.getFailStatus()));
                    break;
                case "startedAt":
                    filteredList.sort(Comparator.comparing(execution -> execution.getStartedAt()));
                    break;
                case "endedAt":
                    filteredList.sort(Comparator.comparing(execution -> execution.getEndedAt()));
                    break;
                case "duration":
                    filteredList.sort(Comparator.comparing(execution -> execution.getDuration()));
                    break;
                case "priority":
                    filteredList.sort(Comparator.comparing(execution -> execution.isPriority()));
                    break;
                case "plannedFor":
                    filteredList.sort(Comparator.comparing(execution -> execution.getPlannedFor()));
                    break;
                case "expiresAt":
                    filteredList.sort(Comparator.comparing(execution -> execution.getExpiresAt()));
                    break;
                case "batchId":
                    filteredList.sort(Comparator.comparing(execution -> execution.getBatchId()));
                    break;
                case "chainId":
                    filteredList.sort(Comparator.comparing(execution -> execution.getChainId()));
                    break;
                case "parameters":
                    filteredList.sort(Comparator.comparing(execution -> execution.getParameters()));
                    break;
                case "parametersHash":
                    filteredList.sort(Comparator.comparing(execution -> execution.getParametersHash()));
                    break;
                case "failRetry":
                    filteredList.sort(Comparator.comparing(execution -> execution.getFailRetry()));
                    break;
                case "failRetryExecutionId":
                    filteredList.sort(Comparator.comparing(execution -> execution.getFailRetryExecutionId()));
                    break;
                case "createdAt":
                    filteredList.sort(Comparator.comparing(execution -> execution.getCreatedAt()));
                    break;
                case "updatedAt":
                    filteredList.sort(Comparator.comparing(execution -> execution.getUpdatedAt()));
                    break;
                default:
                    break;
            }
            if (!asc) {
                Collections.reverse(filteredList);
            }
        }
    }

    private String toIso8601(LocalDateTime timestamp) {
        return timestamp.atZone(ZoneId.of(StaticConfig.TIME_ZONE)).toString();
    }

    private LocalDateTime fromIso8601(String timestamp) {
        return ZonedDateTime.parse(timestamp).toLocalDateTime();
    }

    @Override
    public Execution persist(Execution execution) {

        Long id = executionId.getAndIncrement();
        execution.setId(id);
        execution.setCreatedAt(WorkhorseUtil.timestamp());

        JobData jobData = memoryPersistence.getJobDataMap().get(execution.getJobId());
        jobData.executions.put(id, execution);
        jobData.orderedIds.add(id);

        newExecutionEventEvent.fireAsync(new NewExecutionEvent(execution.getJobId(), execution.getId()));
        return execution;
    }

    @Override
    public String getPersistenceName() {
        return MemoryPersistence.NAME;
    }

    @Override
    public List<Execution> getByJobId(Long jobId, Long limit) {
        ListingParameters listingParameters = new ListingParameters(limit.intValue());
        ListingResult<Execution> listingResult = getExecutionListing(jobId, listingParameters);
        return listingResult.getResults();
    }

    @Override
    public void connect(Object... params) {
        return;
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

        ListingParameters listingParameters = new ListingParameters(1);
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
        listingParameters.setSortAttribute(DESC + "createdAt");

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

        ListingParameters listingParameters = new ListingParameters(0);
        listingParameters.addFilterAttributes("createdAt", GT + toIso8601(preDate));

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

        for (Long jobId : memoryPersistence.getJobDataMap().keySet()) {

            ListingParameters listingParameters = new ListingParameters(0);
            listingParameters.addFilterAttributes("status", ExecutionStatus.RUNNING);
            listingParameters.addFilterAttributes("startedAt", LT + toIso8601(time));

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

}
