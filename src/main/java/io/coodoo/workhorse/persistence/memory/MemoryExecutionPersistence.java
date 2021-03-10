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
import io.coodoo.workhorse.core.entity.ExecutionFailStatus;
import io.coodoo.workhorse.core.entity.ExecutionLog;
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

    private AtomicLong executionId = new AtomicLong(0);

    private AtomicLong executionLogId = new AtomicLong(0);

    @Override
    public Execution getById(Long jobId, Long id) {
        return memoryPersistence.getExecutions().get(id);
    }

    @Override
    public Execution persist(Execution execution) {
        Long id = executionId.getAndIncrement();
        execution.setId(id);
        execution.setCreatedAt(WorkhorseUtil.timestamp());
        memoryPersistence.getExecutions().put(id, execution);

        newExecutionEventEvent.fireAsync(new NewExecutionEvent(execution.getJobId(), execution.getId()));
        return execution;
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
    public List<Execution> pollNextExecutions(Long jobId, int limit) {

        List<Execution> executions = new ArrayList<>();
        LocalDateTime currentTimeStamp = LocalDateTime.now(ZoneId.of(StaticConfig.TIME_ZONE));

        for (Execution execution : memoryPersistence.getExecutions().values()) {

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
    public Execution update(Execution execution) {

        execution.setUpdatedAt(WorkhorseUtil.timestamp());

        if (memoryPersistence.getExecutions().put(execution.getId(), execution) == null) {
            return null;
        }
        return execution;
    }

    @Override
    public Execution updateStatus(Long jobId, Long executionId, ExecutionStatus status, ExecutionFailStatus failStatus) {

        Execution execution = memoryPersistence.getExecutions().get(executionId);

        execution.setStatus(status);
        if (failStatus != null) {
            execution.setFailStatus(failStatus);
        }
        return update(execution);
    }

    @Override
    public Execution getQueuedBatchExecution(Long jobId, Long batchId) {
        for (Execution execution : memoryPersistence.getExecutions().values()) {
            if (execution.getJobId().equals(jobId) && Objects.equals(execution.getBatchId(), batchId) && execution.getStatus() == ExecutionStatus.QUEUED) {
                return execution;
            }
        }
        return null;
    }

    @Override
    public List<Execution> getFailedBatchExecutions(Long jobId, Long batchId) {
        List<Execution> executions = new ArrayList<>();
        for (Execution execution : memoryPersistence.getExecutions().values()) {
            if (Objects.equals(execution.getBatchId(), batchId) && ExecutionStatus.FAILED.equals(execution.getStatus())) {
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
            if (execution.getJobId().equals(jobId) && Objects.equals(execution.getChainId(), chainId) && execution.getStatus() == ExecutionStatus.QUEUED) {

                execution.setStatus(ExecutionStatus.FAILED);
                update(execution);
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

        Comparator<Execution> sortByPriority = (Execution e1, Execution e2) -> e1.getCreatedAt().compareTo(e2.getCreatedAt());
        Collections.sort(executions, sortByPriority);
        return executions;
    }

    @Override
    public void delete(Long jobId, Long executionId) {
        memoryPersistence.getExecutionLogs().remove(executionId);
        memoryPersistence.getExecutions().remove(executionId);
    }

    @Override
    public boolean isPusherAvailable() {
        return true;
    }

    @Override
    public int deleteOlderExecutions(Long jobId, LocalDateTime preDate) {
        int count = 0;
        for (Execution execution : memoryPersistence.getExecutions().values()) {
            if (jobId.equals(execution.getJobId()) && preDate.isAfter(execution.getCreatedAt())) {
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
    public List<Execution> findTimeoutExecutions(LocalDateTime time) {

        List<Execution> executions = new ArrayList<>();

        for (Execution execution : memoryPersistence.getExecutions().values()) {

            if (time.isAfter(execution.getStartedAt()) && ExecutionStatus.RUNNING.equals(execution.getStatus())) {
                executions.add(execution);
            }
        }
        return executions;
    }

    @Override
    public ExecutionLog getLog(Long jobId, Long executionId) {
        return memoryPersistence.getExecutionLogs().get(executionId);
    }

    @Override
    public void log(Long jobId, Long executionId, String log) {

        ExecutionLog executionLog = memoryPersistence.getExecutionLogs().get(executionId);

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

        memoryPersistence.getExecutionLogs().put(executionId, executionLog);
    }

    @Override
    public void log(Long jobId, Long executionId, String error, String stacktrace) {

        ExecutionLog executionLog = memoryPersistence.getExecutionLogs().get(executionId);

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

        memoryPersistence.getExecutionLogs().put(executionId, executionLog);
    }

}
