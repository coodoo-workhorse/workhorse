package io.coodoo.workhorse.core.control;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import io.coodoo.workhorse.core.control.event.NewExecutionEvent;
import io.coodoo.workhorse.core.entity.Execution;
import io.coodoo.workhorse.core.entity.ExecutionStatus;
import io.coodoo.workhorse.persistence.interfaces.ExecutionPersistence;

@RunWith(MockitoJUnitRunner.class)
public class WorkhorseTest {

    @Mock
    ExecutionPersistence executionPersistence;

    @Mock
    WorkhorseController workhorseController;

    @Mock
    ExecutionBuffer executionBuffer;

    @Mock
    Logger log;

    @Mock
    ScheduledFuture<?> scheduledFuture;

    @Mock
    ScheduledExecutorService scheduledExecutorService;

    @InjectMocks
    Workhorse classUnderTest;

    @Test
    public void testPush() throws Exception {

        long jobId = 1L;
        long executionId = 4L;
        StaticConfig.BUFFER_MAX = 4L;

        Execution execution = new Execution();
        execution.setId(executionId);
        execution.setJobId(jobId);
        execution.setStatus(ExecutionStatus.QUEUED);

        NewExecutionEvent newExecutionEvent = new NewExecutionEvent(jobId, executionId);

        when(executionPersistence.isPusherAvailable()).thenReturn(true);
        when(executionPersistence.getById(jobId, executionId)).thenReturn(execution);
        when(executionBuffer.getNumberOfExecution(jobId)).thenReturn(1);

        classUnderTest.push(newExecutionEvent);

        verify(executionPersistence).isPusherAvailable();
        verify(executionPersistence).getById(jobId, executionId);
        verify(executionBuffer).getNumberOfExecution(jobId);
        verify(scheduledExecutorService, never()).schedule(any(Runnable.class), anyLong(), anyObject());
    }

    @Test
    public void testPush_pusher_not_available() throws Exception {

        long jobId = 1L;
        long executionId = 4L;

        Execution execution = new Execution();
        execution.setId(executionId);
        execution.setJobId(jobId);
        execution.setStatus(ExecutionStatus.QUEUED);

        NewExecutionEvent newExecutionEvent = new NewExecutionEvent(jobId, executionId);

        when(executionPersistence.isPusherAvailable()).thenReturn(false);
        when(executionPersistence.getById(jobId, executionId)).thenReturn(execution);

        classUnderTest.push(newExecutionEvent);

        verify(executionPersistence, never()).getById(anyLong(), anyLong());

    }

    @Test
    public void testPush_status_PLANNED() throws Exception {

        long jobId = 1L;
        long executionId = 4L;
        StaticConfig.BUFFER_MAX = 4L;
        StaticConfig.TIME_ZONE = "UTC";

        Execution execution = new Execution();
        execution.setId(executionId);
        execution.setJobId(jobId);
        execution.setStatus(ExecutionStatus.PLANNED);
        execution.setPlannedAt(LocalDateTime.now(ZoneId.of(StaticConfig.TIME_ZONE)).plusSeconds(30));

        NewExecutionEvent newExecutionEvent = new NewExecutionEvent(jobId, executionId);

        when(executionPersistence.isPusherAvailable()).thenReturn(true);
        when(executionPersistence.getById(jobId, executionId)).thenReturn(execution);
        when(executionBuffer.getNumberOfExecution(jobId)).thenReturn(1);

        classUnderTest.push(newExecutionEvent);

        verify(executionPersistence).isPusherAvailable();
        verify(executionPersistence).getById(jobId, executionId);
        verify(executionBuffer).getNumberOfExecution(jobId);
        verify(scheduledExecutorService).schedule(any(Runnable.class), anyLong(), anyObject());
    }

    @Ignore
    @Test
    public void testPush_status_execution_is_NULL() throws Exception {

        long jobId = 1L;
        long executionId = 4L;
        StaticConfig.BUFFER_MAX = 4L;
        StaticConfig.TIME_ZONE = "UTC";

        // Logger log = mock(Logger.class);

        Execution execution = null;

        NewExecutionEvent newExecutionEvent = new NewExecutionEvent(jobId, executionId);

        when(executionPersistence.isPusherAvailable()).thenReturn(true);
        when(executionBuffer.getNumberOfExecution(jobId)).thenReturn(1);
        when(executionPersistence.getById(jobId, executionId)).thenReturn(null);

        classUnderTest.push(newExecutionEvent);

        verify(scheduledExecutorService, never()).schedule(any(Runnable.class), anyLong(), anyObject());
        verify(log).error(anyString());

    }

}
