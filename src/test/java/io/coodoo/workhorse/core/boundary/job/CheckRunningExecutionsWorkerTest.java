package io.coodoo.workhorse.core.boundary.job;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
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
import io.coodoo.workhorse.core.control.StaticConfig;
import io.coodoo.workhorse.core.control.WorkhorseController;
import io.coodoo.workhorse.core.entity.Execution;
import io.coodoo.workhorse.core.entity.ExecutionFailStatus;
import io.coodoo.workhorse.core.entity.ExecutionStatus;
import io.coodoo.workhorse.persistence.interfaces.ExecutionPersistence;

@RunWith(MockitoJUnitRunner.class)
public class CheckRunningExecutionsWorkerTest {

    @Mock
    ExecutionPersistence executionPersistence;

    @Mock
    WorkhorseLogService workhorseLogService;

    @Mock
    WorkhorseController workhorseController;

    @InjectMocks
    CheckRunningExecutionsWorker classUnderTest;

    @Test
    public void testDoWork() throws Exception {

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

        classUnderTest.doWork();

        for (Execution zombiExecution : timeoutExecutions) {
            String logMessage = "Stuck running execution found (ID: " + zombiExecution.getId() + "): ";
            verify(workhorseLogService).logMessage(logMessage + "Put in status " + ExecutionStatus.ABORTED, zombiExecution.getJobId(), false);
            verify(workhorseController).updateExecutionStatus(zombiExecution.getJobId(), zombiExecution.getId(), ExecutionStatus.ABORTED);
        }
    }

    @Test
    public void testDoWork_ExecutionTimeoutStatus_is_QUEUED() throws Exception {

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

        when(executionPersistence.findTimeoutExecutions(anyObject())).thenReturn(timeoutExecutions);

        when(workhorseController.createRetryExecution(anyObject())).thenReturn(mock(Execution.class));

        classUnderTest.doWork();

        for (Execution zombiExecution : timeoutExecutions) {

            String logMessage = "Stuck running execution found (ID: " + zombiExecution.getId() + "): ";
            verify(workhorseLogService).logMessage(logMessage + "Marked as failed and queued a clone (ID: 0)", zombiExecution.getJobId(), false);
            verify(workhorseController).createRetryExecution(zombiExecution);
            verify(workhorseController).setExecutionStatusToFailed(zombiExecution, ExecutionFailStatus.TIMEOUT);
        }
    }

    @Test
    public void testDoWork_ExecutionTimeoutStatus_is_RUNNING() throws Exception {

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

        classUnderTest.doWork();

        for (Execution zombiExecution : timeoutExecutions) {
            assertEquals(ExecutionStatus.RUNNING, zombiExecution.getStatus());

            String logMessage = "Stuck running execution found (ID: " + zombiExecution.getId() + "): ";
            verify(workhorseLogService).logMessage(logMessage + "No action is taken", zombiExecution.getJobId(), false);
            verify(workhorseController, never()).updateExecutionStatus(zombiExecution.getJobId(), zombiExecution.getId(), ExecutionStatus.RUNNING);
        }
    }

    @Test
    public void testDoWork_withEmptyList() throws Exception {

        StaticConfig.EXECUTION_TIMEOUT = 12;
        StaticConfig.EXECUTION_TIMEOUT_STATUS = ExecutionStatus.ABORTED;
        StaticConfig.TIME_ZONE = "UTC";

        List<Execution> timeoutExecutions = new ArrayList<>();

        when(executionPersistence.findTimeoutExecutions(anyObject())).thenReturn(timeoutExecutions);

        classUnderTest.doWork();

        verify(executionPersistence).findTimeoutExecutions(anyObject());
        verify(workhorseLogService, never()).logMessage(anyString(), anyLong(), anyBoolean());
        verify(workhorseController, never()).updateExecutionStatus(anyLong(), anyLong(), any(ExecutionStatus.class));
    }

    @Test
    public void testDoWork_ToLow_ExecutionTimeout() throws Exception {

        StaticConfig.EXECUTION_TIMEOUT = 0;

        classUnderTest.doWork();

        verify(executionPersistence, never()).findTimeoutExecutions(anyObject());
        verify(workhorseLogService, never()).logMessage(anyString(), anyLong(), anyBoolean());
        verify(workhorseController, never()).updateExecutionStatus(anyLong(), anyLong(), any(ExecutionStatus.class));
    }

}
