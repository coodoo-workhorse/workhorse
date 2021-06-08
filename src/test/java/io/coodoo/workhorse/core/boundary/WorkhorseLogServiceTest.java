package io.coodoo.workhorse.core.boundary;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CancellationException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import io.coodoo.workhorse.core.control.event.JobErrorEvent;
import io.coodoo.workhorse.core.entity.ErrorType;
import io.coodoo.workhorse.core.entity.WorkhorseLog;
import io.coodoo.workhorse.persistence.interfaces.LogPersistence;

@RunWith(MockitoJUnitRunner.class)
public class WorkhorseLogServiceTest {

    @InjectMocks
    WorkhorseLogService classUnderTest;

    @Mock
    LogPersistence logPersistence;

    @Test
    public void testLogException() {

        Long jobId = 1L;

        JobErrorEvent jobErrorPayload = new JobErrorEvent();
        jobErrorPayload.setJobId(jobId);

        String message = "test message";
        Throwable throwable = new RuntimeException(message);
        jobErrorPayload.setThrowable(throwable);

        WorkhorseLog workhorseLog = new WorkhorseLog();
        workhorseLog.setId(jobId);

        when(logPersistence.persist(anyObject())).thenReturn(workhorseLog);

        classUnderTest.logException(jobErrorPayload);

        ArgumentCaptor<WorkhorseLog> argument = ArgumentCaptor.forClass(WorkhorseLog.class);
        verify(logPersistence).persist(argument.capture());

        assertTrue(argument.getValue().getStacktrace().contains("RuntimeException"));
        assertEquals(message, argument.getValue().getMessage());

    }

    @Test
    public void testLogException_with_cancelletionException() throws Exception {

        Long jobId = 1L;

        JobErrorEvent jobErrorPayload = new JobErrorEvent();
        jobErrorPayload.setJobId(jobId);
        Throwable throwable = new CancellationException("test message");
        jobErrorPayload.setThrowable(throwable);

        WorkhorseLog workhorseLog = new WorkhorseLog();
        workhorseLog.setId(jobId);

        when(logPersistence.persist(anyObject())).thenReturn(workhorseLog);

        classUnderTest.logException(jobErrorPayload);

        String stacktraceInfo = "This job thread has been cancelled due to an error in another job thread of this job.";

        ArgumentCaptor<WorkhorseLog> argument = ArgumentCaptor.forClass(WorkhorseLog.class);
        verify(logPersistence).persist(argument.capture());
        assertEquals(stacktraceInfo, argument.getValue().getStacktrace().split(System.lineSeparator())[0]);
        assertEquals(ErrorType.JOB_THREAD_CANCELLED.getMessage(), argument.getValue().getMessage());

    }

}
