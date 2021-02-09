package io.coodoo.workhorse.core.control;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import io.coodoo.workhorse.core.boundary.WorkhorseLogService;
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

    WorkhorseConfig workhorseConfig = new WorkhorseConfig();

    @Test
    public void TestUpdateBufferMax() {

        Long bufferMax = 10L;

        classUnderTest.updateBufferMax(workhorseConfig, bufferMax);

        assertEquals(bufferMax, workhorseConfig.getBufferMax());
        assertEquals(bufferMax, StaticConfig.BUFFER_MAX);

    }

    @Test
    public void TestUpdateBufferMax_with_too_small_value() {

        Long bufferMax = 0L;

        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage(
                "The max amount of executions to load into the memory buffer per job must be higher than 0!");

        classUnderTest.updateBufferMax(workhorseConfig, bufferMax);

    }

    @Test
    public void TestUpdateBufferMin() {

        int bufferMin = 7;

        classUnderTest.updateBufferMin(workhorseConfig, bufferMin);

        assertEquals(bufferMin, workhorseConfig.getBufferMin());
        assertEquals(bufferMin, StaticConfig.BUFFER_MIN);
    }

    @Test
    public void TestUpdateBufferMin_with_too_small_value() {

        int bufferMin = 0;

        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage(
                "The min amount of executions in memory buffer before the poller gets to add more must be higher than 0!");

        classUnderTest.updateBufferMin(workhorseConfig, bufferMin);
    }

    @Test
    public void TestUpdateBufferPollInterval() {

        int bufferPollInterval = 3;

        classUnderTest.updateBufferPollInterval(workhorseConfig, bufferPollInterval);

        assertEquals(bufferPollInterval, workhorseConfig.getBufferPollInterval());
        assertEquals(bufferPollInterval, StaticConfig.BUFFER_POLL_INTERVAL);
    }

    @Test
    public void TestUpdateBufferPollInterval_with_too_high_bufferPollInterval() {

        int bufferPollInterval = 61;

        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("The buffer poller interval must be between 1 and 60!");

        classUnderTest.updateBufferPollInterval(workhorseConfig, bufferPollInterval);

    }

    @Test
    public void TestUpdateBufferPollInterval_with_too_small_bufferPollInterval() {

        int bufferPollInterval = 0;

        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("The buffer poller interval must be between 1 and 60!");

        classUnderTest.updateBufferPollInterval(workhorseConfig, bufferPollInterval);

    }

    @Test
    public void TestUpdateBufferPushFallbackPollInterval() {

        int bufferPushFallbackPollInterval = 60;

        classUnderTest.updateBufferPushFallbackPollInterval(workhorseConfig, bufferPushFallbackPollInterval);

        assertEquals(bufferPushFallbackPollInterval, workhorseConfig.getBufferPushFallbackPollInterval());
        assertEquals(bufferPushFallbackPollInterval, StaticConfig.BUFFER_PUSH_FALL_BACK_POLL_INTERVAL);
    }

    @Test
    public void TestUpdateBufferPushFallbackPollInterval_with_too_small_value() {

        int bufferPushFallbackPollInterval = -2;

        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("The buffer push fallback poller interval must be higher than 0!");

        classUnderTest.updateBufferPushFallbackPollInterval(workhorseConfig, bufferPushFallbackPollInterval);

    }
}
