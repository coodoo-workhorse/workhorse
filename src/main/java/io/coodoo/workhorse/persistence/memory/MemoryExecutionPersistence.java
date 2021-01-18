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

import io.coodoo.workhorse.config.entity.WorkhorseConfig;
import io.coodoo.workhorse.core.control.event.NewExecutionEvent;
import io.coodoo.workhorse.core.entity.Execution;
import io.coodoo.workhorse.core.entity.ExecutionStatus;
import io.coodoo.workhorse.persistence.interfaces.ExecutionPersistence;
import io.coodoo.workhorse.persistence.interfaces.PersistenceTyp;

@ApplicationScoped
public class MemoryExecutionPersistence implements ExecutionPersistence {

    private static Logger log = Logger.getLogger(MemoryExecutionPersistence.class);

    @Inject
    MemoryPersistence memoryService;

    @Inject
    WorkhorseConfig jobEngineConfig;

    @Inject
    Event<NewExecutionEvent> newJobExecutionEvent;

    private AtomicLong incId = new AtomicLong(0);

    @Override
    public Execution getById(Long jobId, Long id) {
        return memoryService.getExecutions().get(id);
    }

    @Override
    public void persist(Execution jobExecution) {
        Long id = incId.getAndIncrement();
        jobExecution.setId(id);
        jobExecution.setCreatedAt(jobEngineConfig.timestamp());
        memoryService.getExecutions().put(id, jobExecution);
        
        newJobExecutionEvent.fireAsync(new NewExecutionEvent(jobExecution.getJobId(), jobExecution.getId()));

    }

    @Override
    public Long count() {
        return Long.valueOf(memoryService.getExecutions().size());
    }

    @Override
    public PersistenceTyp getPersistenceTyp() {
        return PersistenceTyp.MEMORY;
    }

    @Override
    public List<Execution> getByJobId(Long jobId, Long limit) {
        List<Execution> jobEx = new ArrayList<>();

        for (Execution jobExecution : memoryService.getExecutions().values()) {
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
    public List<Execution> pollNextJobExecutions(Long jobId, Long limit) {
        List<Execution> jobEx = new ArrayList<>();

        LocalDateTime currentTimeStamp = LocalDateTime.now(ZoneId.of(jobEngineConfig.getTimeZone()));

        for (Execution jobExecution : memoryService.getExecutions().values()) {

            if (jobExecution.getJobId().equals(jobId) && jobExecution.getStatus().equals(ExecutionStatus.QUEUED)
                    && (jobExecution.getMaturity() == null
                            || jobExecution.getMaturity().compareTo(currentTimeStamp) < 0)
                    && jobExecution.getChainedPreviousExecutionId() == null && jobEx.size() < limit.intValue()) {

                jobEx.add(jobExecution);
            }
        }

        Comparator<Execution> sortByPriority = (Execution jobEx1, Execution jobEx2) -> jobEx1.getPriority()
                .compareTo(jobEx2.getPriority());
        Collections.sort(jobEx, sortByPriority);

        return jobEx;
    }

    @Override
    public Execution update(Long jobId, Long id, Execution jobExecution) {
        if (memoryService.getExecutions().put(id, jobExecution) == null) {
            return null;
        } else {
            return jobExecution;
        }
    }

    @Override
    public Execution addJobExecutionAtEndOfChain(Long jobId, Long chainId, Execution jobExecution) {
        for (Execution jobEx : memoryService.getExecutions().values()) {
            if (jobEx.getJobId().equals(jobId) && chainId.equals(jobEx.getChainId())
                    && jobEx.getChainedNextExecutionId() == null) {

                jobEx.setChainedNextExecutionId(jobExecution.getId());
                return jobExecution;
            }
        }
        return null;
    }

    @Override
    public Execution getNextQueuedJobExecutionInChain(Long jobId, Long chainId, Execution jobExecution) {
        Execution jobEx = getById(jobId, jobExecution.getChainedNextExecutionId());
        Long previousExecutionId = jobExecution.getId();
        if (jobEx == null) {
            for (Execution execution : memoryService.getExecutions().values()) {
                if (execution != null && chainId.equals(execution.getChainId())
                        && previousExecutionId.equals(execution.getChainedPreviousExecutionId())
                        && ExecutionStatus.QUEUED.equals(execution.getStatus())) {
                    log.info("From Peristennce. Next Job Execution In Chain : " + execution);
                    return execution;
                }
            }
        } else {
            if (jobEx.getStatus().equals(ExecutionStatus.QUEUED)) {
                return jobEx;
            }
        }
        return null;

    }

    @Override
    public Execution getQueuedBatchJobExecution(Long jobId, Long batchId) {
        for (Execution jobExecution : memoryService.getExecutions().values()) {
            if (jobExecution.getJobId().equals(jobId) && Objects.equals(jobExecution.getBatchId(), batchId)
                    && jobExecution.getStatus().equals(ExecutionStatus.QUEUED)) {

                return jobExecution;
            }

        }
        return null;
    }

    @Override
    public List<Execution> getFailedBatchJobExecutions(Long jobId, Long batchId) {
        List<Execution> jobEx = new ArrayList<>();
        for (Execution jobExecution : memoryService.getExecutions().values()) {
            if (Objects.equals(jobExecution.getBatchId(), batchId)
                    && ExecutionStatus.FAILED.equals(jobExecution.getStatus())) {
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
        for (Execution jobExecution : memoryService.getExecutions().values()) {
            if (jobExecution.getJobId().equals(jobId) && Objects.equals(jobExecution.getChainId(), chainId)
                    && jobExecution.getStatus().equals(ExecutionStatus.QUEUED)) {

                jobExecution.setStatus(ExecutionStatus.ABORTED);
                update(jobId, jobExecution.getId(), jobExecution);
            }
        }
        return true;
    }

    @Override
    public List<Execution> getBatch(Long jobId, Long batchId) {

        List<Execution> jobEx = new ArrayList<>();
        for (Execution jobExecution : memoryService.getExecutions().values()) {
            if (Objects.equals(jobExecution.getBatchId(), batchId)) {
                jobEx.add(jobExecution);
            }
        }
        return jobEx;
    }

    @Override
    public List<Execution> getChain(Long jobId, Long chainId) {
        List<Execution> jobEx = new ArrayList<>();
        for (Execution jobExecution : memoryService.getExecutions().values()) {
            if (Objects.equals(jobExecution.getChainId(), chainId)) {
                jobEx.add(jobExecution);
            }
        }
        return jobEx;
    }

    @Override
    public void delete(Long jobId, Long id) {
        memoryService.getExecutions().remove(id);
    }

    @Override
    public boolean isPusherAvailable() {
        return true;
    }

    @Override
    public int deleteOlderExecutions(Long jobId, LocalDateTime preDate) {
        int count = 0;
        for (Execution jobExecution : memoryService.getExecutions().values()) {
            if (jobId.equals(jobExecution.getJobId()) &&  preDate.compareTo(jobExecution.getCreatedAt()) > 0) {
                log.info("Next JobExecution have to be delete: " + jobExecution );
               delete(jobId, jobExecution.getId());
               count++;
            }
        }
        return count;
    }

}
