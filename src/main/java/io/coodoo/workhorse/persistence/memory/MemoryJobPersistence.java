package io.coodoo.workhorse.persistence.memory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.coodoo.workhorse.core.entity.ExecutionFailStatus;
import io.coodoo.workhorse.core.entity.ExecutionStatus;
import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.core.entity.JobStatus;
import io.coodoo.workhorse.persistence.interfaces.JobPersistence;
import io.coodoo.workhorse.persistence.interfaces.listing.ListingParameters;
import io.coodoo.workhorse.persistence.interfaces.listing.ListingResult;
import io.coodoo.workhorse.persistence.interfaces.listing.Metadata;
import io.coodoo.workhorse.util.WorkhorseUtil;

@ApplicationScoped
public class MemoryJobPersistence implements JobPersistence {

    @Inject
    MemoryPersistence memoryPersistence;

    private AtomicLong incId = new AtomicLong(0);

    @Override
    public Job get(Long jobId) {
        return memoryPersistence.getJobDataMap().get(jobId).job;
    }

    @Override
    public ListingResult<Job> getJobListing(ListingParameters listingParameters) {

        JobData jobData = memoryPersistence.getJobDataMap().get(jobId);

        if (listingParameters.getFilterAttributes().isEmpty()) {

            Metadata metadata = new Metadata(new Long(jobData.executions.size()), listingParameters);
            List<Long> ids = jobData.orderedIds.subList(metadata.getStartIndex(), metadata.getEndIndex());
            List<Job> result = ids.stream().map(id -> jobData.executions.get(id)).collect(Collectors.toList());

            return new ListingResult<Job>(result, metadata);
        }

        List<Predicate<Job>> allPredicates = new ArrayList<Predicate<Job>>();

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
        List<Job> filteredList =
                        jobData.executions.values().stream().filter(allPredicates.stream().reduce(x -> true, Predicate::and)).collect(Collectors.toList());

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
                case "jobId":
                    filteredList.sort(Comparator.comparing(execution -> execution.getId()));
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
                default:
                    break;
            }
            if (!asc) {
                Collections.reverse(filteredList);
            }
        }

        Metadata metadata = new Metadata(new Long(filteredList.size()), listingParameters);
        List<Job> result = filteredList.subList(metadata.getStartIndex(), metadata.getEndIndex());

        return new ListingResult<Job>(result, metadata);
    }

    @Override
    public Job getByWorkerClassName(String jobClassName) {
        for (Job job : memoryPersistence.getJobs().values()) {
            if (job.getWorkerClassName().equals(jobClassName)) {
                return job;
            }
        }
        return null;
    }

    @Override
    public Job persist(Job job) {

        Long jobId = incId.getAndIncrement();
        job.setId(jobId);
        job.setCreatedAt(WorkhorseUtil.timestamp());

        JobData jobData = new JobData(job);
        memoryPersistence.getJobDataMap().put(job.getId(), jobData);
        return job;
    }

    @Override
    public Job update(Job job) {

        memoryPersistence.getJobDataMap().get(job.getId()).job = job;
        job.setUpdatedAt(WorkhorseUtil.timestamp());
        return job;
    }

    @Override
    public void deleteJob(Long jobId) {
        memoryPersistence.getJobDataMap().remove(jobId);
    }

    @Override
    public List<Job> getAll() {
        List<Job> result = new ArrayList<>();
        result.addAll(memoryPersistence.getJobs().values());
        return result;
    }

    @Override
    public Long count() {
        return Long.valueOf(memoryPersistence.getJobs().size());
    }

    @Override
    public String getPersistenceName() {
        return MemoryPersistence.NAME;
    }

    @Override
    public Job getByName(String jobName) {
        for (Job job : memoryPersistence.getJobs().values()) {
            if (Objects.equals(job.getName(), jobName)) {
                return job;
            }
        }
        return null;
    }

    @Override
    public List<Job> getAllByStatus(JobStatus jobStatus) {
        List<Job> result = new ArrayList<>();

        for (Job job : memoryPersistence.getJobs().values()) {
            if (Objects.equals(job.getStatus(), jobStatus)) {
                result.add(job);
            }
        }

        return result;
    }

    @Override
    public List<Job> getAllScheduled() {
        List<Job> result = new ArrayList<>();

        for (Job job : memoryPersistence.getJobs().values()) {
            if (job.getSchedule() != null && !job.getSchedule().isEmpty()) {
                result.add(job);
            }
        }

        return result;
    }

    @Override
    public Long countByStatus(JobStatus jobStatus) {
        return Long.valueOf(getAllByStatus(jobStatus).size());
    }

    @Override
    public void connect(Object... params) {}

}
