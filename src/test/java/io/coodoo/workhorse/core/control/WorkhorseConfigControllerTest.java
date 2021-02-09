package io.coodoo.workhorse.core.control;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import io.coodoo.workhorse.core.boundary.WorkhorseLogService;
import io.coodoo.workhorse.core.entity.JobStatus;
import io.coodoo.workhorse.core.entity.WorkhorseConfig;

@RunWith(MockitoJUnitRunner.class)
public class WorkhorseConfigControllerTest {

    @Mock
    WorkhorseLogService workhorseLogService;

    @Mock
    Workhorse workhorse;

    @InjectMocks
    WorkhorseConfigController classUnderTest;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void TestUpdateBufferMax() {

        WorkhorseConfig workhorseConfig = new WorkhorseConfig();
        Long bufferMax = 10L;

        classUnderTest.updateBufferMax(workhorseConfig, bufferMax);

        assertEquals(bufferMax, workhorseConfig.getBufferMax());
        assertEquals(bufferMax, StaticConfig.BUFFER_MAX);

    }

    @Test
    public void TestUpdateBufferMax_with_too_small_value() {

        WorkhorseConfig workhorseConfig = new WorkhorseConfig();
        Long bufferMax = 0L;

        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("The max amount of executions to load into the memory buffer per job must be higher than 0!");

        classUnderTest.updateBufferMax(workhorseConfig, bufferMax);

    }

    @Test
    public void TestUpdateBufferMin() {

        WorkhorseConfig workhorseConfig = new WorkhorseConfig();
        int bufferMin = 7;

        classUnderTest.updateBufferMin(workhorseConfig, bufferMin);

        assertEquals(bufferMin, workhorseConfig.getBufferMin());
        assertEquals(bufferMin, StaticConfig.BUFFER_MIN);
    }

    @Test
    public void TestUpdateBufferMin_with_too_small_value() {

        WorkhorseConfig workhorseConfig = new WorkhorseConfig();
        int bufferMin = 0;

        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("The min amount of executions in memory buffer before the poller gets to add more must be higher than 0!");

        classUnderTest.updateBufferMin(workhorseConfig, bufferMin);
    }

    @Test
    public void TestUpdateBufferPollInterval() {

        WorkhorseConfig workhorseConfig = new WorkhorseConfig();
        int bufferPollInterval = 3;

        // Default is 5
        assertEquals(5, workhorseConfig.getBufferPollInterval());

        classUnderTest.updateBufferPollInterval(workhorseConfig, bufferPollInterval);

        assertEquals(bufferPollInterval, workhorseConfig.getBufferPollInterval());
    }

    @Test
    public void TestUpdateBufferPollInterval_staticConfigIsChanged() {

        StaticConfig.BUFFER_POLL_INTERVAL = 33;

        WorkhorseConfig workhorseConfig = new WorkhorseConfig();
        int bufferPollInterval = 3;

        classUnderTest.updateBufferPollInterval(workhorseConfig, bufferPollInterval);

        assertEquals(bufferPollInterval, StaticConfig.BUFFER_POLL_INTERVAL);
    }

    @Test
    public void TestUpdateBufferPollInterval_dontUpdateIfEquals() {

        int bufferPollInterval = 3;

        WorkhorseConfig workhorseConfig = new WorkhorseConfig();
        workhorseConfig.setBufferPollInterval(bufferPollInterval);

        classUnderTest.updateBufferPollInterval(workhorseConfig, bufferPollInterval);

        // if nothing has changed, there will be no log and no restart
        verify(workhorseLogService, never()).logChange(anyLong(), any(JobStatus.class), anyString(), anyObject(), anyObject(), anyString());
        verify(workhorse, never()).isRunning();
    }

    @Test(expected = RuntimeException.class)
    public void TestUpdateBufferPollInterval_toLow() {
        classUnderTest.updateBufferPollInterval(new WorkhorseConfig(), 0);
    }

    @Test
    public void TestUpdateBufferPollInterval_toLowMessage() {
        try {
            classUnderTest.updateBufferPollInterval(new WorkhorseConfig(), 0);
            fail("bufferPollInterval to low!");
        } catch (Exception e) {
            assertEquals(RuntimeException.class, e.getClass());
            assertEquals("The buffer poller interval must be between 1 and 60!", e.getMessage());
        }
    }

    @Test
    public void TestUpdateBufferPollInterval_Lowest() {

        WorkhorseConfig workhorseConfig = new WorkhorseConfig();
        int bufferPollInterval = 1;

        classUnderTest.updateBufferPollInterval(workhorseConfig, bufferPollInterval);
        assertEquals(bufferPollInterval, workhorseConfig.getBufferPollInterval());
    }

    @Test(expected = RuntimeException.class)
    public void TestUpdateBufferPollInterval_toHigh() {
        classUnderTest.updateBufferPollInterval(new WorkhorseConfig(), 61);
    }

    @Test
    public void TestUpdateBufferPollInterval_toHighMessage() {
        try {
            classUnderTest.updateBufferPollInterval(new WorkhorseConfig(), 61);
            fail("bufferPollInterval to high!");
        } catch (Exception e) {
            assertEquals(RuntimeException.class, e.getClass());
            assertEquals("The buffer poller interval must be between 1 and 60!", e.getMessage());
        }
    }

    @Test
    public void TestUpdateBufferPollInterval_Highest() {

        WorkhorseConfig workhorseConfig = new WorkhorseConfig();
        int bufferPollInterval = 60;

        classUnderTest.updateBufferPollInterval(workhorseConfig, bufferPollInterval);
        assertEquals(bufferPollInterval, workhorseConfig.getBufferPollInterval());
    }

    @Test
    public void TestUpdateBufferPollInterval_restart() {

        WorkhorseConfig workhorseConfig = new WorkhorseConfig();
        int bufferPollInterval = 3;

        given(workhorse.isRunning()).willReturn(true);

        classUnderTest.updateBufferPollInterval(workhorseConfig, bufferPollInterval);

        verify(workhorse, times(1)).start();
    }

    @Test
    public void TestUpdateBufferPollInterval_restartNotNeeded() {

        WorkhorseConfig workhorseConfig = new WorkhorseConfig();
        int bufferPollInterval = 3;

        given(workhorse.isRunning()).willReturn(false);

        classUnderTest.updateBufferPollInterval(workhorseConfig, bufferPollInterval);

        verify(workhorse, never()).start();
    }

    @Test
    public void TestUpdateBufferPollInterval_logMessage() {

        // Default is 5
        WorkhorseConfig workhorseConfigDefaults = new WorkhorseConfig();
        WorkhorseConfig workhorseConfig = new WorkhorseConfig();
        int bufferPollInterval = 3;

        classUnderTest.updateBufferPollInterval(workhorseConfig, bufferPollInterval);

        verify(workhorseLogService).logChange(null, null, "Buffer poller interval", workhorseConfigDefaults.getBufferPollInterval(), bufferPollInterval, null);
    }

    @Test
    public void TestUpdateBufferPollInterval_with_too_high_bufferPollInterval() {

        WorkhorseConfig workhorseConfig = new WorkhorseConfig();
        int bufferPollInterval = 61;

        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("The buffer poller interval must be between 1 and 60!");

        classUnderTest.updateBufferPollInterval(workhorseConfig, bufferPollInterval);

    }

    @Test
    public void TestUpdateBufferPollInterval_with_too_small_bufferPollInterval() {

        WorkhorseConfig workhorseConfig = new WorkhorseConfig();
        int bufferPollInterval = 0;

        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("The buffer poller interval must be between 1 and 60!");

        classUnderTest.updateBufferPollInterval(workhorseConfig, bufferPollInterval);

    }

    @Test
    public void TestUpdateBufferPushFallbackPollInterval() {

        WorkhorseConfig workhorseConfig = new WorkhorseConfig();
        int bufferPushFallbackPollInterval = 60;

        classUnderTest.updateBufferPushFallbackPollInterval(workhorseConfig, bufferPushFallbackPollInterval);

        assertEquals(bufferPushFallbackPollInterval, workhorseConfig.getBufferPushFallbackPollInterval());
        assertEquals(bufferPushFallbackPollInterval, StaticConfig.BUFFER_PUSH_FALL_BACK_POLL_INTERVAL);
    }

    @Test
    public void TestUpdateBufferPushFallbackPollInterval_with_too_small_value() {

        WorkhorseConfig workhorseConfig = new WorkhorseConfig();
        int bufferPushFallbackPollInterval = -2;

        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("The buffer push fallback poller interval must be higher than 0!");

        classUnderTest.updateBufferPushFallbackPollInterval(workhorseConfig, bufferPushFallbackPollInterval);

    }
}
