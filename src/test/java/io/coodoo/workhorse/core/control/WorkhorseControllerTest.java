package io.coodoo.workhorse.core.control;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.event.Event;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import io.coodoo.workhorse.core.boundary.WorkhorseLogService;
import io.coodoo.workhorse.core.control.event.JobErrorEvent;
import io.coodoo.workhorse.core.entity.Execution;
import io.coodoo.workhorse.core.entity.ExecutionStatus;
import io.coodoo.workhorse.persistence.interfaces.ExecutionPersistence;

@RunWith(MockitoJUnitRunner.class)
public class WorkhorseControllerTest {

    @Mock
    ExecutionPersistence executionPersistence;

    @Mock
    WorkhorseLogService workhorseLogService;

    @Mock
    Event<JobErrorEvent> jobErrorEvent;

    @InjectMocks
    WorkhorseController classUnderTest;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void testHuntTimeoutExecutions() throws Exception {

        StaticConfig.EXECUTION_TIMEOUT = 12;
        StaticConfig.EXECUTION_TIMEOUT_STATUS = ExecutionStatus.ABORTED;
        StaticConfig.TIME_ZONE = "UTC";

        Execution execution1 = new Execution();
        execution1.setId(1L);
        execution1.setJobId(1L);
        execution1.setStatus(ExecutionStatus.RUNNING);

        Execution execution2 = new Execution();
        execution1.setId(2L);
        execution1.setJobId(1L);
        execution2.setStatus(ExecutionStatus.RUNNING);

        List<Execution> timeoutExecutions = new ArrayList<>();

        timeoutExecutions.add(execution1);
        timeoutExecutions.add(execution2);

        // when(WorkhorseUtil.timestamp()).thenReturn(null);
        when(executionPersistence.findTimeoutExecutions(anyObject())).thenReturn(timeoutExecutions);

        classUnderTest.huntTimeoutExecution();

        for (Execution zombiExecution : timeoutExecutions) {
            assertEquals(ExecutionStatus.ABORTED, zombiExecution.getStatus());

            String logMessage = "Zombie execution found (ID: " + zombiExecution.getId() + "): ";
            verify(workhorseLogService).logMessage(logMessage + "Put in status " + ExecutionStatus.ABORTED, zombiExecution.getJobId(), false);

            verify(executionPersistence).update(zombiExecution);
        }

    }

    @Test
    public void testHuntTimeoutExecutions_ExecutionTimeoutStatus_is_QUEUED() throws Exception {

        StaticConfig.EXECUTION_TIMEOUT = 12;
        StaticConfig.EXECUTION_TIMEOUT_STATUS = ExecutionStatus.QUEUED;
        StaticConfig.TIME_ZONE = "UTC";

        Execution execution1 = new Execution();
        execution1.setId(1L);
        execution1.setJobId(1L);
        execution1.setStatus(ExecutionStatus.RUNNING);

        Execution execution2 = new Execution();
        execution1.setId(2L);
        execution1.setJobId(1L);
        execution2.setStatus(ExecutionStatus.RUNNING);

        List<Execution> timeoutExecutions = new ArrayList<>();

        timeoutExecutions.add(execution1);
        timeoutExecutions.add(execution2);

        // when(WorkhorseUtil.timestamp()).thenReturn(null);
        when(executionPersistence.findTimeoutExecutions(anyObject())).thenReturn(timeoutExecutions);

        classUnderTest.huntTimeoutExecution();

        for (Execution zombiExecution : timeoutExecutions) {
            assertEquals(ExecutionStatus.FAILED, zombiExecution.getStatus());

            String logMessage = "Zombie execution found (ID: " + zombiExecution.getId() + "): ";
            verify(workhorseLogService).logMessage(logMessage + "Marked as failed and queued a clone", zombiExecution.getJobId(), false);

            verify(executionPersistence).update(zombiExecution);
        }
    }

    @Test
    public void testHuntTimeoutExecutions_ExecutionTimeoutStatus_is_RUNNING() throws Exception {

        StaticConfig.EXECUTION_TIMEOUT = 12;
        StaticConfig.EXECUTION_TIMEOUT_STATUS = ExecutionStatus.RUNNING;
        StaticConfig.TIME_ZONE = "UTC";

        Execution execution1 = new Execution();
        execution1.setId(1L);
        execution1.setJobId(1L);
        execution1.setStatus(ExecutionStatus.RUNNING);

        Execution execution2 = new Execution();
        execution1.setId(2L);
        execution1.setJobId(1L);
        execution2.setStatus(ExecutionStatus.RUNNING);

        List<Execution> timeoutExecutions = new ArrayList<>();

        timeoutExecutions.add(execution1);
        timeoutExecutions.add(execution2);

        when(executionPersistence.findTimeoutExecutions(anyObject())).thenReturn(timeoutExecutions);

        classUnderTest.huntTimeoutExecution();

        for (Execution zombiExecution : timeoutExecutions) {
            assertEquals(ExecutionStatus.RUNNING, zombiExecution.getStatus());

            String logMessage = "Zombie execution found (ID: " + zombiExecution.getId() + "): ";
            verify(workhorseLogService).logMessage(logMessage + "No action is taken", zombiExecution.getJobId(), false);

            verify(executionPersistence).update(zombiExecution);
        }
    }

    @Test
    public void testHuntTimeoutExecutions_withEmptyList() throws Exception {

        StaticConfig.EXECUTION_TIMEOUT = 12;
        StaticConfig.EXECUTION_TIMEOUT_STATUS = ExecutionStatus.ABORTED;
        StaticConfig.TIME_ZONE = "UTC";

        List<Execution> timeoutExecutions = new ArrayList<>();

        when(executionPersistence.findTimeoutExecutions(anyObject())).thenReturn(timeoutExecutions);

        classUnderTest.huntTimeoutExecution();

        verify(executionPersistence).findTimeoutExecutions(anyObject());

        verify(workhorseLogService, never()).logMessage(anyString(), anyLong(), anyBoolean());

        verify(executionPersistence, never()).update(anyObject());
    }

    @Test
    public void testHuntTimeoutExecutions_ToLow_ExecutionTimeout() throws Exception {

        StaticConfig.EXECUTION_TIMEOUT = 0;

        classUnderTest.huntTimeoutExecution();

        verify(executionPersistence, never()).findTimeoutExecutions(anyObject());

        verify(workhorseLogService, never()).logMessage(anyString(), anyLong(), anyBoolean());

        verify(executionPersistence, never()).update(anyObject());
    }

    @Test
    public void testCreateExecution_uniqueQueued_deactivated() throws Exception {

        Long jobId = 1L;
        Long executionId = 1L;
        String parameters = "parameter";
        boolean priority = false;
        LocalDateTime plannedFor = null;
        LocalDateTime expiresAt = null;
        Long batchId = null;
        Long chainId = null;
        Long chainedPreviousExecutionId = null;
        boolean uniqueQueued = false;

        Execution newexecution = new Execution();

        newexecution.setId(executionId);
        newexecution.setJobId(jobId);
        newexecution.setParameters(parameters);
        newexecution.setStatus(ExecutionStatus.QUEUED);

        when(executionPersistence.persist(anyObject())).thenReturn(newexecution);

        Execution execution = classUnderTest.createExecution(jobId, parameters, priority, plannedFor, expiresAt, batchId, chainId, chainedPreviousExecutionId,
                        uniqueQueued);

        verify(executionPersistence, never()).getFirstCreatedByJobIdAndParametersHash(jobId, parameters.hashCode());

        verify(executionPersistence).persist(anyObject());
        assertEquals(parameters, execution.getParameters());
    }

    @Test
    public void testCreateExecution_uniqueQueued_activ() throws Exception {

        Long jobId = 1L;
        Long executionId = 1L;
        String parameters = "parameter";
        boolean priority = false;
        LocalDateTime plannedFor = null;
        LocalDateTime expiresAt = null;
        Long batchId = null;
        Long chainId = null;
        Long chainedPreviousExecutionId = null;
        boolean uniqueQueued = true;

        Execution newexecution = new Execution();

        newexecution.setId(executionId);
        newexecution.setJobId(jobId);
        newexecution.setParameters(parameters);
        newexecution.setStatus(ExecutionStatus.QUEUED);

        when(executionPersistence.persist(anyObject())).thenReturn(newexecution);

        classUnderTest.createExecution(jobId, parameters, priority, plannedFor, expiresAt, batchId, chainId, chainedPreviousExecutionId, uniqueQueued);

        verify(executionPersistence).getFirstCreatedByJobIdAndParametersHash(jobId, parameters.hashCode());

    }

    @Test
    public void testCreateExecution_uniqueQueued_activ_and_execution_found() throws Exception {

        Long jobId = 1L;
        Long executionId = 1L;
        String parameters = "parameter";
        boolean priority = false;
        LocalDateTime plannedFor = null;
        LocalDateTime expiresAt = null;
        Long batchId = null;
        Long chainId = null;
        Long chainedPreviousExecutionId = null;
        boolean uniqueQueued = true;

        Execution foundExecution = new Execution();
        when(executionPersistence.getFirstCreatedByJobIdAndParametersHash(jobId, parameters.hashCode())).thenReturn(foundExecution);

        Execution newexecution = new Execution();

        newexecution.setId(executionId);
        newexecution.setJobId(jobId);
        newexecution.setParameters(parameters);
        newexecution.setStatus(ExecutionStatus.QUEUED);

        when(executionPersistence.persist(anyObject())).thenReturn(newexecution);

        Execution execution = classUnderTest.createExecution(jobId, parameters, priority, plannedFor, expiresAt, batchId, chainId, chainedPreviousExecutionId,
                        uniqueQueued);

        // check if the search after an execution with given parameter succeed
        verify(executionPersistence).getFirstCreatedByJobIdAndParametersHash(jobId, parameters.hashCode());

        // This execution don't have to be persisted
        verify(executionPersistence, never()).persist(anyObject());

        assertEquals(foundExecution, execution);
    }

    @Test
    public void testCreateExecution_uniqueQueued_activ_parameters_null() throws Exception {

        Long jobId = 1L;
        Long executionId = 1L;
        String parameters = null;
        boolean priority = false;
        LocalDateTime plannedFor = null;
        LocalDateTime expiresAt = null;
        Long batchId = null;
        Long chainId = null;
        Long chainedPreviousExecutionId = null;
        boolean uniqueQueued = true;

        Execution newexecution = new Execution();

        newexecution.setId(executionId);
        newexecution.setJobId(jobId);
        newexecution.setStatus(ExecutionStatus.QUEUED);

        when(executionPersistence.persist(anyObject())).thenReturn(newexecution);

        Execution execution = classUnderTest.createExecution(jobId, parameters, priority, plannedFor, expiresAt, batchId, chainId, chainedPreviousExecutionId,
                        uniqueQueued);

        verify(executionPersistence).getFirstCreatedByJobIdAndParametersHash(jobId, null);

        verify(executionPersistence).persist(anyObject());

        assertNull(execution.getParameters());
        assertNull(execution.getParametersHash());

    }

    @Test
    public void testCreateExecution_uniqueQueued_activ_and_parameters_with_space() throws Exception {

        Long jobId = 1L;
        Long executionId = 1L;
        String parameters = "  ";
        boolean uniqueQueued = true;

        Execution newexecution = new Execution();

        newexecution.setId(executionId);
        newexecution.setJobId(jobId);
        newexecution.setStatus(ExecutionStatus.QUEUED);

        when(executionPersistence.persist(anyObject())).thenReturn(newexecution);

        Execution execution = classUnderTest.createExecution(jobId, parameters, false, null, null, null, null, null, uniqueQueued);

        verify(executionPersistence).getFirstCreatedByJobIdAndParametersHash(jobId, null);

        verify(executionPersistence).persist(anyObject());

        assertNull(execution.getParameters());
        assertNull(execution.getParametersHash());
    }

    @Test
    public void testCreateExecution_uniqueQueued_activ_and_parameters() throws Exception {

        Long jobId = 1L;
        Long executionId = 1L;
        String parameters = "";

        // feature to test
        boolean uniqueQueued = true;

        Execution newexecution = new Execution();

        newexecution.setId(executionId);
        newexecution.setJobId(jobId);
        newexecution.setStatus(ExecutionStatus.QUEUED);

        when(executionPersistence.persist(anyObject())).thenReturn(newexecution);

        Execution execution = classUnderTest.createExecution(jobId, parameters, false, null, null, null, null, null, uniqueQueued);

        verify(executionPersistence).getFirstCreatedByJobIdAndParametersHash(jobId, null);

        verify(executionPersistence).persist(anyObject());

        assertNull(execution.getParameters());
        assertNull(execution.getParametersHash());
    }

    @Test
    public void testCreateExecution_priority_NULL() throws Exception {

        Long jobId = 1L;
        Long executionId = 1L;

        // Null value is tested
        Boolean priority = null;

        Execution newexecution = new Execution();

        newexecution.setId(executionId);
        newexecution.setJobId(jobId);
        newexecution.setStatus(ExecutionStatus.QUEUED);

        when(executionPersistence.persist(anyObject())).thenReturn(newexecution);

        Execution execution = classUnderTest.createExecution(jobId, null, priority, null, null, null, null, null, false);

        verify(executionPersistence).persist(anyObject());
        assertFalse(execution.isPriority());

    }

    @Test
    public void testCreateExecution_chainId() throws Exception {

        Long jobId = 1L;
        Long executionId = 1L;
        String parameters = "parameter";

        // Value to test
        Long chainId = 1L;

        Execution newexecution = new Execution();

        newexecution.setId(executionId);
        newexecution.setJobId(jobId);
        newexecution.setChainId(chainId);
        newexecution.setStatus(ExecutionStatus.QUEUED);

        when(executionPersistence.persist(anyObject())).thenReturn(newexecution);

        Execution persistedExecution = classUnderTest.createExecution(jobId, parameters, false, null, null, null, chainId, null, false);

        verify(executionPersistence).persist(anyObject());
        assertNotNull(persistedExecution);
        assertEquals(chainId, persistedExecution.getChainId());

    }

    @Test
    public void testCreateExecution_with_plannedFor_in_the_past() throws Exception {

        StaticConfig.TIME_ZONE = "UTC";

        Long jobId = 1L;
        Long executionId = 1L;

        LocalDateTime plannedFor = LocalDateTime.now(ZoneId.of(StaticConfig.TIME_ZONE));

        Execution newexecution = new Execution();

        newexecution.setId(executionId);
        newexecution.setJobId(jobId);
        newexecution.setPlannedFor(plannedFor);
        newexecution.setStatus(ExecutionStatus.PLANNED);

        when(executionPersistence.persist(anyObject())).thenReturn(newexecution);

        Execution persistedExecution = classUnderTest.createExecution(jobId, null, false, plannedFor, null, null, null, null, false);

        verify(executionPersistence).persist(anyObject());
        assertNotNull(persistedExecution);
        assertEquals(ExecutionStatus.PLANNED, persistedExecution.getStatus());
        assertEquals(plannedFor, persistedExecution.getPlannedFor());
    }

    @Test
    public void testCreateExecution_with_plannedFor_in_the_future() throws Exception {

        StaticConfig.TIME_ZONE = "UTC";

        Long jobId = 1L;
        Long executionId = 1L;

        LocalDateTime plannedFor = LocalDateTime.now(ZoneId.of(StaticConfig.TIME_ZONE)).plusSeconds(30);

        Execution newexecution = new Execution();

        newexecution.setId(executionId);
        newexecution.setJobId(jobId);
        newexecution.setPlannedFor(plannedFor);
        newexecution.setStatus(ExecutionStatus.PLANNED);

        when(executionPersistence.persist(anyObject())).thenReturn(newexecution);

        Execution persistedExecution = classUnderTest.createExecution(jobId, null, false, plannedFor, null, null, null, null, false);

        verify(executionPersistence).persist(anyObject());
        assertNotNull(persistedExecution);
        assertEquals(ExecutionStatus.PLANNED, persistedExecution.getStatus());
        assertEquals(plannedFor, persistedExecution.getPlannedFor());
    }

    @Test
    public void testCreateExecution_with_persisited_execution_null() throws Exception {

        Long jobId = 1L;

        Execution newexecution = new Execution();

        newexecution.setJobId(jobId);
        newexecution.setStatus(ExecutionStatus.QUEUED);

        when(executionPersistence.persist(anyObject())).thenReturn(null);

        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("The execution " + newexecution + " couldn't be persisited by the persisitence.");

        classUnderTest.createExecution(jobId, null, false, null, null, null, null, null, false);

    }

    @Test
    public void testCreateExecution_without_returned_execution_id() throws Exception {

        Long jobId = 1L;

        Execution newexecution = new Execution();

        newexecution.setJobId(jobId);
        newexecution.setStatus(ExecutionStatus.QUEUED);

        when(executionPersistence.persist(any())).thenReturn(newexecution);

        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("The execution " + newexecution + " couldn't be persisited by the persisitence.");

        classUnderTest.createExecution(jobId, null, false, null, null, null, null, null, false);

    }

}
