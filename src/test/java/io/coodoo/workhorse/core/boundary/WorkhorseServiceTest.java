package io.coodoo.workhorse.core.boundary;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import io.coodoo.workhorse.core.control.StaticConfig;
import io.coodoo.workhorse.core.control.WorkhorseController;
import io.coodoo.workhorse.core.entity.JobExecutionCount;

@RunWith(MockitoJUnitRunner.class)
public class WorkhorseServiceTest {

    @InjectMocks
    WorkhorseService classUnderTest;

    @Mock
    WorkhorseController workhorseController;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void testGetJobExecutionCount_with_FROM_in_the_future() throws Exception {

        Long jobId = 1L;
        StaticConfig.TIME_ZONE = "UTC";
        LocalDateTime from = LocalDateTime.now(ZoneId.of(StaticConfig.TIME_ZONE)).plusHours(5);
        LocalDateTime to = LocalDateTime.now(ZoneId.of(StaticConfig.TIME_ZONE));

        JobExecutionCount result = new JobExecutionCount();

        when(workhorseController.getJobExecutionCount(jobId, from, to)).thenReturn(result);

        classUnderTest.getJobExecutionCount(jobId, from, to);

        verify(workhorseController).getJobExecutionCount(jobId, to.minusHours(24), to);

    }

    @Test
    public void testGetJobExecutionCount_with_FROM_null() throws Exception {

        Long jobId = 1L;
        StaticConfig.TIME_ZONE = "UTC";
        LocalDateTime from = null;
        LocalDateTime to = LocalDateTime.now(ZoneId.of(StaticConfig.TIME_ZONE));

        JobExecutionCount result = new JobExecutionCount();

        when(workhorseController.getJobExecutionCount(jobId, from, to)).thenReturn(result);

        classUnderTest.getJobExecutionCount(jobId, from, to);

        verify(workhorseController).getJobExecutionCount(jobId, to.minusHours(24), to);

    }

    @Test
    public void testGetJobExecutionCount_with_TO_null() throws Exception {

        Long jobId = 1L;
        StaticConfig.TIME_ZONE = "UTC";
        LocalDateTime from = null;
        LocalDateTime to = LocalDateTime.now(ZoneId.of(StaticConfig.TIME_ZONE));

        JobExecutionCount result = new JobExecutionCount();

        when(workhorseController.getJobExecutionCount(jobId, from, to)).thenReturn(result);

        classUnderTest.getJobExecutionCount(jobId, from, to);

        verify(workhorseController).getJobExecutionCount(jobId, to.minusHours(24), to);

    }

}
