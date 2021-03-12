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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.core.entity.JobStatus;
import io.coodoo.workhorse.persistence.interfaces.JobPersistence;
import io.coodoo.workhorse.persistence.interfaces.listing.ListingParameters;
import io.coodoo.workhorse.persistence.interfaces.listing.ListingResult;
import io.coodoo.workhorse.persistence.interfaces.listing.Metadata;
import io.coodoo.workhorse.util.WorkhorseUtil;

@ApplicationScoped
public class MemoryJobPersistence implements JobPersistence {

    private static Logger log = LoggerFactory.getLogger(MemoryJobPersistence.class);

    @Inject
    MemoryPersistence memoryPersistence;

    private AtomicLong incId = new AtomicLong(0);

    @Override
    public Job get(Long jobId) {
        return memoryPersistence.getJobs().get(jobId);
    }

    @Override
    public ListingResult<Job> getJobListing(ListingParameters listingParameters) {

        List<Predicate<Job>> allPredicates = new ArrayList<Predicate<Job>>();

        for (String key : listingParameters.getFilterAttributes().keySet()) {
            String rawvalue = listingParameters.getFilterAttributes().get(key);
            try {
                for (String value : rawvalue.split("|")) {
                    switch (key) {
                        case "id":
                            allPredicates.add(job -> job.getId().equals(new Long(value)));
                            break;
                        case "name":
                            allPredicates.add(job -> job.getName().matches(".*" + value + ".*"));
                            break;
                        case "description":
                            allPredicates.add(job -> job.getDescription().matches(".*" + value + ".*"));
                            break;
                        case "workerClassName":
                            allPredicates.add(job -> job.getWorkerClassName().matches(".*" + value + ".*"));
                            break;
                        case "parametersClassName":
                            allPredicates.add(job -> job.getParametersClassName().matches(".*" + value + ".*"));
                            break;
                        case "status":
                            JobStatus status = JobStatus.valueOf(value);
                            allPredicates.add(job -> job.getStatus().equals(status));
                            break;
                        case "threads":
                            allPredicates.add(job -> job.getThreads() == new Integer(value).intValue());
                            break;
                        case "maxPerMinute":
                            allPredicates.add(job -> Objects.equals(job.getMaxPerMinute(), new Integer(value)));
                            break;
                        case "failRetries":
                            allPredicates.add(job -> job.getFailRetries() == new Integer(value).intValue());
                            break;
                        case "retryDelay":
                            allPredicates.add(job -> job.getRetryDelay() == new Integer(value).intValue());
                            break;
                        case "minutesUntilCleanUp":
                            allPredicates.add(job -> job.getMinutesUntilCleanUp() == new Integer(value).intValue());
                            break;
                        case "uniqueQueued":
                            allPredicates.add(job -> job.isUniqueQueued() == new Boolean(value));
                            break;
                        case "schedule":
                            allPredicates.add(job -> job.getSchedule().matches(".*" + value + ".*"));
                            break;
                        case "createdAt":
                            if (value.startsWith(MemoryListingUtil.LT)) {
                                LocalDateTime timestamp = MemoryListingUtil.fromIso8601(value.replace(MemoryListingUtil.LT, ""));
                                allPredicates.add(execution -> timestamp.isAfter(execution.getCreatedAt()));
                            } else if (value.startsWith(MemoryListingUtil.GT)) {
                                LocalDateTime timestamp = MemoryListingUtil.fromIso8601(value.replace(MemoryListingUtil.GT, ""));
                                allPredicates.add(execution -> timestamp.isBefore(execution.getCreatedAt()));
                            }
                            break;
                        case "updatedAt":
                            if (value.startsWith(MemoryListingUtil.LT)) {
                                LocalDateTime timestamp = MemoryListingUtil.fromIso8601(value.replace(MemoryListingUtil.LT, ""));
                                allPredicates.add(execution -> timestamp.isAfter(execution.getUpdatedAt()));
                            } else if (value.startsWith(MemoryListingUtil.GT)) {
                                LocalDateTime timestamp = MemoryListingUtil.fromIso8601(value.replace(MemoryListingUtil.GT, ""));
                                allPredicates.add(execution -> timestamp.isBefore(execution.getUpdatedAt()));
                            }
                            break;
                        default:
                            break;
                    }
                }
            } catch (Exception e) {
                log.warn("Job filter {} with value {} is invalid: {}", key, rawvalue, e.getMessage());
            }
        }

        List<Job> filteredList = memoryPersistence.getJobs().values().stream().filter(allPredicates.stream().reduce(x -> true, Predicate::and))
                        .collect(Collectors.toList());

        String sort = listingParameters.getSortAttribute();
        boolean asc = true;
        if (sort != null && !sort.isEmpty()) {
            if (sort.startsWith(MemoryListingUtil.ASC)) {
                sort = sort.replace(MemoryListingUtil.ASC, "");
            } else if (sort.startsWith(MemoryListingUtil.DESC)) {
                sort = sort.replace(MemoryListingUtil.DESC, "");
                asc = false;
            }
        } else {
            sort = "status";
        }
        switch (sort) {
            case "id":
                filteredList.sort(Comparator.comparing(job -> job.getId()));
                break;
            case "name":
                filteredList.sort(Comparator.comparing(job -> job.getName()));
                break;
            case "description":
                filteredList.sort(Comparator.comparing(job -> job.getDescription()));
                break;
            case "workerClassName":
                filteredList.sort(Comparator.comparing(job -> job.getWorkerClassName()));
                break;
            case "parametersClassName":
                filteredList.sort(Comparator.comparing(job -> job.getParametersClassName()));
                break;
            case "status":
                filteredList.sort(Comparator.comparing(job -> job.getStatus()));
                break;
            case "threads":
                filteredList.sort(Comparator.comparing(job -> job.getThreads()));
                break;
            case "maxPerMinute":
                filteredList.sort(Comparator.comparing(job -> job.getMaxPerMinute()));
                break;
            case "failRetries":
                filteredList.sort(Comparator.comparing(job -> job.getFailRetries()));
                break;
            case "retryDelay":
                filteredList.sort(Comparator.comparing(job -> job.getRetryDelay()));
                break;
            case "minutesUntilCleanUp":
                filteredList.sort(Comparator.comparing(job -> job.getMinutesUntilCleanUp()));
                break;
            case "uniqueQueued":
                filteredList.sort(Comparator.comparing(job -> job.isUniqueQueued()));
                break;
            case "schedule":
                filteredList.sort(Comparator.comparing(job -> job.getSchedule()));
                break;
            case "createdAt":
                filteredList.sort(Comparator.comparing(job -> job.getCreatedAt()));
                break;
            case "updatedAt":
                filteredList.sort(Comparator.comparing(job -> job.getUpdatedAt()));
                break;
            default:
                break;
        }
        if (!asc) {
            Collections.reverse(filteredList);
        }
        Metadata metadata = new Metadata(new Long(filteredList.size()), listingParameters);
        List<Job> result = filteredList.subList(metadata.getStartIndex() - 1, metadata.getEndIndex() - 1);
        return new ListingResult<Job>(result, metadata);
    }

    @Override
    public Job getByWorkerClassName(String jobClassName) {

        ListingParameters listingParameters = new ListingParameters(1);
        listingParameters.addFilterAttributes("workerClassName", jobClassName);
        ListingResult<Job> listingResult = getJobListing(listingParameters);

        if (listingResult.getResults().isEmpty()) {
            return null;
        }
        return listingResult.getResults().get(0);
    }

    @Override
    public Job persist(Job job) {

        Long jobId = incId.getAndIncrement();
        job.setId(jobId);
        job.setCreatedAt(WorkhorseUtil.timestamp());

        memoryPersistence.getJobs().put(jobId, job);
        JobData jobData = new JobData();
        memoryPersistence.getJobDataMap().put(job.getId(), jobData);
        return job;
    }

    @Override
    public Job update(Job job) {

        job.setUpdatedAt(WorkhorseUtil.timestamp());
        memoryPersistence.getJobs().put(job.getId(), job);
        return job;
    }

    @Override
    public void deleteJob(Long jobId) {
        memoryPersistence.getJobDataMap().remove(jobId);
    }

    @Override
    public List<Job> getAll() {
        return new ArrayList<>(memoryPersistence.getJobs().values());
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

        ListingParameters listingParameters = new ListingParameters(1);
        listingParameters.addFilterAttributes("name", jobName);
        ListingResult<Job> listingResult = getJobListing(listingParameters);

        if (listingResult.getResults().isEmpty()) {
            return null;
        }
        return listingResult.getResults().get(0);
    }

    @Override
    public List<Job> getAllByStatus(JobStatus jobStatus) {

        ListingParameters listingParameters = new ListingParameters(0);
        listingParameters.addFilterAttributes("status", jobStatus);
        ListingResult<Job> listingResult = getJobListing(listingParameters);

        return listingResult.getResults();
    }

    @Override
    public List<Job> getAllScheduled() {

        ListingParameters listingParameters = new ListingParameters(0);
        listingParameters.addFilterAttributes("schedule", " "); // jeder schedule sollte min ein leerzeichen haben...
        ListingResult<Job> listingResult = getJobListing(listingParameters);

        return listingResult.getResults();
    }

    @Override
    public Long countByStatus(JobStatus jobStatus) {
        return Long.valueOf(getAllByStatus(jobStatus).size());
    }

    @Override
    public void connect(Object... params) {}

}
