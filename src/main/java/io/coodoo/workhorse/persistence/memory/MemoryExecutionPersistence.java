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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.coodoo.workhorse.core.control.StaticConfig;
import io.coodoo.workhorse.core.control.event.NewExecutionEvent;
import io.coodoo.workhorse.core.entity.Execution;
import io.coodoo.workhorse.core.entity.ExecutionStatus;
import io.coodoo.workhorse.persistence.interfaces.ExecutionPersistence;
import io.coodoo.workhorse.util.WorkhorseUtil;

@ApplicationScoped
public class MemoryExecutionPersistence implements ExecutionPersistence {

    private static Logger log = LoggerFactory.getLogger(MemoryExecutionPersistence.class);

    @Inject
    MemoryPersistence memoryPersistence;

    @Inject
    Event<NewExecutionEvent> newExecutionEventEvent;

    private AtomicLong incId = new AtomicLong(0);

    @Override
    public Execution getById(Long jobId, Long id) {
        return memoryPersistence.getExecutions().get(id);
    }

    @Override
    public void persist(Execution execution) {
        Long id = incId.getAndIncrement();
        execution.setId(id);
        execution.setCreatedAt(WorkhorseUtil.timestamp());
        memoryPersistence.getExecutions().put(id, execution);

        newExecutionEventEvent.fireAsync(new NewExecutionEvent(execution.getJobId(), execution.getId()));

    }

    @Override
    public Long count() {
        return Long.valueOf(memoryPersistence.getExecutions().size());
    }

    @Override
    public String getPersistenceName() {
        return MemoryPersistence.NAME;
    }

    @Override
    public List<Execution> getByJobId(Long jobId, Long limit) {

        List<Execution> executions = new ArrayList<>();
        for (Execution execution : memoryPersistence.getExecutions().values()) {
            if (execution.getJobId().equals(jobId) && executions.size() < limit.intValue()) {
                executions.add(execution);
            }
        }
        return executions;
    }

    @Override
    public void connect(Object... params) {
        return;
    }

    @Override
    public List<Execution> pollNextExecutions(Long jobId, Long limit) {

        List<Execution> executions = new ArrayList<>();
        LocalDateTime currentTimeStamp = LocalDateTime.now(ZoneId.of(StaticConfig.TIME_ZONE));

        for (Execution execution : memoryPersistence.getExecutions().values()) {

            if (execution.getJobId().equals(jobId) && execution.getStatus() == ExecutionStatus.QUEUED
                    && (execution.getMaturity() == null || execution.getMaturity().compareTo(currentTimeStamp) < 0)
                    && execution.getChainedPreviousExecutionId() == null && executions.size() < limit.intValue()) {

                executions.add(execution);
            }
        }

        Comparator<Execution> sortByPriority = (Execution e1, Execution e2) -> e1.getPriority()
                .compareTo(e2.getPriority());
        Collections.sort(executions, sortByPriority);

        return executions;
    }

    @Override
    public Execution update(Long jobId, Long id, Execution execution) {
        if (memoryPersistence.getExecutions().put(id, execution) == null) {
            return null;
        } else {
            return execution;
        }
    }

    @Override
    public Execution addExecutionAtEndOfChain(Long jobId, Long chainId, Execution execution) {
        for (Execution executionFromMemory : memoryPersistence.getExecutions().values()) {
            if (executionFromMemory.getJobId().equals(jobId) && chainId.equals(executionFromMemory.getChainId())
                    && executionFromMemory.getChainedNextExecutionId() == null) {
                executionFromMemory.setChainedNextExecutionId(execution.getId());
                return execution;
            }
        }
        return null;
    }

    @Override
    public Execution getNextQueuedExecutionInChain(Long jobId, Long chainId, Execution execution) {
        Execution chainedNextExecution = getById(jobId, execution.getChainedNextExecutionId());
        Long previousExecutionId = execution.getId();
        if (chainedNextExecution == null) {
            for (Execution executionFromMemory : memoryPersistence.getExecutions().values()) {
                if (executionFromMemory != null && chainId.equals(executionFromMemory.getChainId())
                        && previousExecutionId.equals(executionFromMemory.getChainedPreviousExecutionId())
                        && ExecutionStatus.QUEUED.equals(executionFromMemory.getStatus())) {
                    log.trace("From Peristennce. Next Job Execution In Chain : {}", executionFromMemory);
                    return executionFromMemory;
                }
            }
        } else {
            if (chainedNextExecution.getStatus() == ExecutionStatus.QUEUED) {
                return chainedNextExecution;
            }
        }
        return null;
    }

    @Override
    public Execution getQueuedBatchExecution(Long jobId, Long batchId) {
        for (Execution execution : memoryPersistence.getExecutions().values()) {
            if (execution.getJobId().equals(jobId) && Objects.equals(execution.getBatchId(), batchId)
                    && execution.getStatus() == ExecutionStatus.QUEUED) {
                return execution;
            }
        }
        return null;
    }

    @Override
    public List<Execution> getFailedBatchExecutions(Long jobId, Long batchId) {
        List<Execution> executions = new ArrayList<>();
        for (Execution execution : memoryPersistence.getExecutions().values()) {
            if (Objects.equals(execution.getBatchId(), batchId)
                    && ExecutionStatus.FAILED.equals(execution.getStatus())) {
                executions.add(execution);
            }
        }
        return executions;
    }

    @Override
    public boolean isBatchFinished(Long jobId, Long batchId) {
        return getQueuedBatchExecution(jobId, batchId) == null ? true : false;
    }

    @Override
    public boolean abortChain(Long jobId, Long chainId) {
        for (Execution execution : memoryPersistence.getExecutions().values()) {
            if (execution.getJobId().equals(jobId) && Objects.equals(execution.getChainId(), chainId)
                    && execution.getStatus() == ExecutionStatus.QUEUED) {

                execution.setStatus(ExecutionStatus.ABORTED);
                update(jobId, execution.getId(), execution);
            }
        }
        return true;
    }

    @Override
    public List<Execution> getBatch(Long jobId, Long batchId) {

        List<Execution> executions = new ArrayList<>();
        for (Execution execution : memoryPersistence.getExecutions().values()) {
            if (Objects.equals(execution.getBatchId(), batchId)) {
                executions.add(execution);
            }
        }
        return executions;
    }

    @Override
    public List<Execution> getChain(Long jobId, Long chainId) {

        List<Execution> executions = new ArrayList<>();
        for (Execution execution : memoryPersistence.getExecutions().values()) {
            if (Objects.equals(execution.getChainId(), chainId)) {
                executions.add(execution);
            }
        }
        return executions;
    }

    @Override
    public void delete(Long jobId, Long id) {
        memoryPersistence.getExecutions().remove(id);
    }

    @Override
    public boolean isPusherAvailable() {
        return true;
    }

    @Override
    public int deleteOlderExecutions(Long jobId, LocalDateTime preDate) {
        int count = 0;
        for (Execution execution : memoryPersistence.getExecutions().values()) {
            if (jobId.equals(execution.getJobId()) && preDate.compareTo(execution.getCreatedAt()) > 0) {
                log.trace("Next Execution have to be delete: {}", execution);
                delete(jobId, execution.getId());
                count++;
            }
        }
        return count;
    }

    @Override
    public Execution getFirstCreatedByJobIdAndParametersHash(Long jobId, Integer parameterHash) {

        for (Execution execution : memoryPersistence.getExecutions().values()) {
            if (jobId.equals(execution.getJobId()) && ExecutionStatus.QUEUED.equals(execution.getStatus())
                    && parameterHash.equals(execution.getParametersHash())) {
                return execution;
            }
        }
        return null;
    }

    @Override
    public List<Execution> findExpiredExecutions(LocalDateTime time) {

        List<Execution> executions = new ArrayList<>();

        for (Execution execution : memoryPersistence.getExecutions().values()) {

            if (time.compareTo(execution.getStartedAt()) > 0 && ExecutionStatus.RUNNING.equals(execution.getStatus())) {
                executions.add(execution);
            }
        }
        return executions;
    }

}
