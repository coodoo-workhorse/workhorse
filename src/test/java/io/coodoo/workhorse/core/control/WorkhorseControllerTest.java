package io.coodoo.workhorse.core.control;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import io.coodoo.workhorse.core.boundary.WorkhorseLogService;
import io.coodoo.workhorse.core.entity.Execution;
import io.coodoo.workhorse.core.entity.ExecutionStatus;
import io.coodoo.workhorse.persistence.interfaces.ExecutionPersistence;

@RunWith(MockitoJUnitRunner.class)
public class WorkhorseControllerTest {

    @Mock
    ExecutionPersistence executionPersistence;

    @Mock
    WorkhorseLogService workhorseLogService;

    @InjectMocks
    WorkhorseController classUnderTest;

    @Test
    public void testHuntExpiredExecutions() throws Exception {

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

        List<Execution> expiredExecutions = new ArrayList<>();

        expiredExecutions.add(execution1);
        expiredExecutions.add(execution2);

        // when(WorkhorseUtil.timestamp()).thenReturn(null);
        when(executionPersistence.findExpiredExecutions(anyObject())).thenReturn(expiredExecutions);

        classUnderTest.huntExpiredExecutions();

        for (Execution zombiExecution : expiredExecutions) {
            assertEquals(ExecutionStatus.ABORTED, zombiExecution.getStatus());

            String logMessage = "Zombie execution found (ID: " + zombiExecution.getId() + "): ";
            verify(workhorseLogService).logMessage(logMessage + "Put in status " + ExecutionStatus.ABORTED, zombiExecution.getJobId(), false);

            verify(executionPersistence).update(zombiExecution.getJobId(), zombiExecution.getId(), zombiExecution);
        }

    }

    @Test
    public void testHuntExpiredExecutions_ExecutionTimeoutStatus_is_QUEUED() throws Exception {

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

        List<Execution> expiredExecutions = new ArrayList<>();

        expiredExecutions.add(execution1);
        expiredExecutions.add(execution2);

        // when(WorkhorseUtil.timestamp()).thenReturn(null);
        when(executionPersistence.findExpiredExecutions(anyObject())).thenReturn(expiredExecutions);

        classUnderTest.huntExpiredExecutions();

        for (Execution zombiExecution : expiredExecutions) {
            assertEquals(ExecutionStatus.FAILED, zombiExecution.getStatus());

            String logMessage = "Zombie execution found (ID: " + zombiExecution.getId() + "): ";
            verify(workhorseLogService).logMessage(logMessage + "Marked as failed and queued a clone", zombiExecution.getJobId(), false);

            verify(executionPersistence).update(zombiExecution.getJobId(), zombiExecution.getId(), zombiExecution);
        }
    }

    @Test
    public void testHuntExpiredExecutions_ExecutionTimeoutStatus_is_RUNNING() throws Exception {

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

        List<Execution> expiredExecutions = new ArrayList<>();

        expiredExecutions.add(execution1);
        expiredExecutions.add(execution2);

        when(executionPersistence.findExpiredExecutions(anyObject())).thenReturn(expiredExecutions);

        classUnderTest.huntExpiredExecutions();

        for (Execution zombiExecution : expiredExecutions) {
            assertEquals(ExecutionStatus.RUNNING, zombiExecution.getStatus());

            String logMessage = "Zombie execution found (ID: " + zombiExecution.getId() + "): ";
            verify(workhorseLogService).logMessage(logMessage + "No action is taken", zombiExecution.getJobId(), false);

            verify(executionPersistence).update(zombiExecution.getJobId(), zombiExecution.getId(), zombiExecution);
        }
    }

    @Test
    public void testHuntExpiredExecutions_withEmptyList() throws Exception {

        StaticConfig.EXECUTION_TIMEOUT = 12;
        StaticConfig.EXECUTION_TIMEOUT_STATUS = ExecutionStatus.ABORTED;
        StaticConfig.TIME_ZONE = "UTC";

        List<Execution> expiredExecutions = new ArrayList<>();

        when(executionPersistence.findExpiredExecutions(anyObject())).thenReturn(expiredExecutions);

        classUnderTest.huntExpiredExecutions();

        verify(executionPersistence).findExpiredExecutions(anyObject());

        verify(workhorseLogService, never()).logMessage(anyString(), anyLong(), anyBoolean());

        verify(executionPersistence, never()).update(anyLong(), anyLong(), anyObject());
    }

    @Test
    public void testHuntExpiredExecutions_ToLow_ExecutionTimeout() throws Exception {

        StaticConfig.EXECUTION_TIMEOUT = 0;

        classUnderTest.huntExpiredExecutions();

        verify(executionPersistence, never()).findExpiredExecutions(anyObject());

        verify(workhorseLogService, never()).logMessage(anyString(), anyLong(), anyBoolean());

        verify(executionPersistence, never()).update(anyLong(), anyLong(), anyObject());
    }

}
