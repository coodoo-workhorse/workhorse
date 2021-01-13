package io.coodoo.workhorse.persistence.memory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.coodoo.workhorse.config.entity.JobEngineConfig;
import io.coodoo.workhorse.jobengine.control.events.NewJobExecutionEvent;
import io.coodoo.workhorse.jobengine.entity.JobExecution;
import io.coodoo.workhorse.jobengine.entity.JobExecutionStatus;
import io.coodoo.workhorse.persistence.interfaces.JobExecutionPersistence;
import io.coodoo.workhorse.persistence.interfaces.PersistenceTyp;

@ApplicationScoped
public class MemoryJobExecutionDAO implements JobExecutionPersistence {

    private static Logger log = Logger.getLogger(MemoryJobExecutionDAO.class);

    @Inject
    Memory memoryService;

    @Inject
    JobEngineConfig jobEngineConfig;

    @Inject
    Event<NewJobExecutionEvent> newJobExecutionEvent;

    private AtomicLong incId = new AtomicLong(0);

    @Override
    public JobExecution getById(Long jobId, Long id) {
        return memoryService.getJobExecutions().get(id);
    }

    @Override
    public void persist(JobExecution jobExecution) {
        Long id = incId.getAndIncrement();
        jobExecution.setId(id);
        jobExecution.setCreatedAt(jobEngineConfig.timestamp());
        memoryService.getJobExecutions().put(id, jobExecution);
        
        newJobExecutionEvent.fireAsync(new NewJobExecutionEvent(jobExecution.getJobId(), jobExecution.getId()));

    }

    @Override
    public Long count() {
        return Long.valueOf(memoryService.getJobExecutions().size());
    }

    @Override
    public PersistenceTyp getPersistenceTyp() {
        return PersistenceTyp.MEMORY;
    }

    @Override
    public List<JobExecution> getByJobId(Long jobId, Long limit) {
        List<JobExecution> jobEx = new ArrayList<>();

        for (JobExecution jobExecution : memoryService.getJobExecutions().values()) {
            if (jobExecution.getJobId().equals(jobId) && jobEx.size() < limit.intValue()) {
                jobEx.add(jobExecution);
            }
        }
        return jobEx;
    }

    @Override
    public void connect(Object... params) {
        return;
    }

    @Override
    public List<JobExecution> pollNextJobExecutions(Long jobId, Long limit) {
        List<JobExecution> jobEx = new ArrayList<>();

        LocalDateTime currentTimeStamp = LocalDateTime.now(ZoneId.of(jobEngineConfig.getTimeZone()));

        for (JobExecution jobExecution : memoryService.getJobExecutions().values()) {

            if (jobExecution.getJobId().equals(jobId) && jobExecution.getStatus().equals(JobExecutionStatus.QUEUED)
                    && (jobExecution.getMaturity() == null
                            || jobExecution.getMaturity().compareTo(currentTimeStamp) < 0)
                    && jobExecution.getChainedPreviousExecutionId() == null && jobEx.size() < limit.intValue()) {

                jobEx.add(jobExecution);
            }
        }

        Comparator<JobExecution> sortByPriority = (JobExecution jobEx1, JobExecution jobEx2) -> jobEx1.getPriority()
                .compareTo(jobEx2.getPriority());
        Collections.sort(jobEx, sortByPriority);

        return jobEx;
    }

    @Override
    public JobExecution update(Long jobId, Long id, JobExecution jobExecution) {
        if (memoryService.getJobExecutions().put(id, jobExecution) == null) {
            return null;
        } else {
            return jobExecution;
        }
    }

    @Override
    public JobExecution addJobExecutionAtEndOfChain(Long jobId, Long chainId, JobExecution jobExecution) {
        for (JobExecution jobEx : memoryService.getJobExecutions().values()) {
            if (jobEx.getJobId().equals(jobId) && chainId.equals(jobEx.getChainId())
                    && jobEx.getChainedNextExecutionId() == null) {

                jobEx.setChainedNextExecutionId(jobExecution.getId());
                return jobExecution;
            }
        }
        return null;
    }

    @Override
    public JobExecution getNextQueuedJobExecutionInChain(Long jobId, Long chainId, JobExecution jobExecution) {
        JobExecution jobEx = getById(jobId, jobExecution.getChainedNextExecutionId());
        Long previousExecutionId = jobExecution.getId();
        if (jobEx == null) {
            for (JobExecution execution : memoryService.getJobExecutions().values()) {
                if (execution != null && chainId.equals(execution.getChainId())
                        && previousExecutionId.equals(execution.getChainedPreviousExecutionId())
                        && JobExecutionStatus.QUEUED.equals(execution.getStatus())) {
                    log.info("From Peristennce. Next Job Execution In Chain : " + execution);
                    return execution;
                }
            }
        } else {
            if (jobEx.getStatus().equals(JobExecutionStatus.QUEUED)) {
                return jobEx;
            }
        }
        return null;

    }

    @Override
    public JobExecution getQueuedBatchJobExecution(Long jobId, Long batchId) {
        for (JobExecution jobExecution : memoryService.getJobExecutions().values()) {
            if (jobExecution.getJobId().equals(jobId) && Objects.equals(jobExecution.getBatchId(), batchId)
                    && jobExecution.getStatus().equals(JobExecutionStatus.QUEUED)) {

                return jobExecution;
            }

        }
        return null;
    }

    @Override
    public List<JobExecution> getFailedBatchJobExecutions(Long jobId, Long batchId) {
        List<JobExecution> jobEx = new ArrayList<>();
        for (JobExecution jobExecution : memoryService.getJobExecutions().values()) {
            if (Objects.equals(jobExecution.getBatchId(), batchId)
                    && JobExecutionStatus.FAILED.equals(jobExecution.getStatus())) {
                jobEx.add(jobExecution);
            }
        }
        return jobEx;
    }

    @Override
    public boolean isBatchFinished(Long jobId, Long batchId) {
        return getQueuedBatchJobExecution(jobId, batchId) == null ? true : false;
    }

    @Override
    public boolean abortChain(Long jobId, Long chainId) {
        for (JobExecution jobExecution : memoryService.getJobExecutions().values()) {
            if (jobExecution.getJobId().equals(jobId) && Objects.equals(jobExecution.getChainId(), chainId)
                    && jobExecution.getStatus().equals(JobExecutionStatus.QUEUED)) {

                jobExecution.setStatus(JobExecutionStatus.ABORTED);
                update(jobId, jobExecution.getId(), jobExecution);
            }
        }
        return true;
    }

    @Override
    public List<JobExecution> getBatch(Long jobId, Long batchId) {

        List<JobExecution> jobEx = new ArrayList<>();
        for (JobExecution jobExecution : memoryService.getJobExecutions().values()) {
            if (Objects.equals(jobExecution.getBatchId(), batchId)) {
                jobEx.add(jobExecution);
            }
        }
        return jobEx;
    }

    @Override
    public List<JobExecution> getChain(Long jobId, Long chainId) {
        List<JobExecution> jobEx = new ArrayList<>();
        for (JobExecution jobExecution : memoryService.getJobExecutions().values()) {
            if (Objects.equals(jobExecution.getChainId(), chainId)) {
                jobEx.add(jobExecution);
            }
        }
        return jobEx;
    }

    @Override
    public void delete(Long jobId, Long id) {
        memoryService.getJobExecutions().remove(id);
    }

    @Override
    public boolean isPusherAvailable() {
        return true;
    }

    @Override
    public int deleteOlderJobExecutions(Long jobId, LocalDateTime preDate) {
        int count = 0;
        for (JobExecution jobExecution : memoryService.getJobExecutions().values()) {
            if (jobId.equals(jobExecution.getJobId()) &&  preDate.compareTo(jobExecution.getCreatedAt()) > 0) {
                log.info("Next JobExecution have to be delete: " + jobExecution );
               delete(jobId, jobExecution.getId());
               count++;
            }
        }
        return count;
    }

}
