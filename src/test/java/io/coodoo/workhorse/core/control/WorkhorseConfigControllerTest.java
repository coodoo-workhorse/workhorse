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
import static org.mockito.Mockito.when;

import java.time.ZoneId;

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
import io.coodoo.workhorse.persistence.interfaces.ConfigPersistence;
import io.coodoo.workhorse.persistence.memory.MemoryConfigBuilder;

@RunWith(MockitoJUnitRunner.class)
public class WorkhorseConfigControllerTest {

    @Mock
    WorkhorseLogService workhorseLogService;

    @Mock
    ConfigPersistence configPersistence;

    @Mock
    Workhorse workhorse;

    @InjectMocks
    WorkhorseConfigController classUnderTest;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void testUpdateBufferMax() {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        Long bufferMax = 1L;

        classUnderTest.updateBufferMax(workhorseConfig, bufferMax);

        assertEquals(bufferMax, workhorseConfig.getBufferMax());
        assertEquals(bufferMax, StaticConfig.BUFFER_MAX);

    }

    @Test
    public void testUpdateBufferMax_tooLow() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        Long bufferMax = 0L;

        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("The max amount of executions to load into the memory buffer per job must be higher than 0!");

        classUnderTest.updateBufferMax(workhorseConfig, bufferMax);

    }

    @Test
    public void testUpdateBufferMax_dontUpdateIfEquals() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        Long bufferMax = 50L;

        workhorseConfig.setBufferMax(bufferMax);
        classUnderTest.updateBufferMax(workhorseConfig, bufferMax);

        // if nothing has changed, there will be no log and no restart
        verify(workhorseLogService, never()).logChange(anyLong(), any(JobStatus.class), anyString(), anyObject(), anyObject(), anyString());
    }

    @Test
    public void testUpdateBufferMax_logMessage() throws Exception {

        WorkhorseConfig workhorseConfigDefaults = new MemoryConfigBuilder().build();
        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        Long bufferMax = 50L;

        classUnderTest.updateBufferMax(workhorseConfig, bufferMax);

        verify(workhorseLogService).logChange(null, null, "Max amount of executions to load into the memory buffer per job",
                        workhorseConfigDefaults.getBufferMax(), bufferMax, null);
    }

    @Test
    public void testUpdateBufferMin() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        int bufferMin = 2;

        classUnderTest.updateBufferMin(workhorseConfig, bufferMin);

        assertEquals(bufferMin, workhorseConfig.getBufferMin());
        assertEquals(bufferMin, StaticConfig.BUFFER_MIN);
    }

    @Test
    public void testUpdateBufferMin_tooLow() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        int bufferMin = 0;

        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("The min amount of executions in memory buffer before the poller gets to add more must be higher than 0!");

        classUnderTest.updateBufferMin(workhorseConfig, bufferMin);
    }

    @Test
    public void testUpdateBufferMin_logMessage() throws Exception {

        // Default is 1
        WorkhorseConfig workhorseConfigDefaults = new MemoryConfigBuilder().build();
        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        int bufferMin = 4;

        classUnderTest.updateBufferMin(workhorseConfig, bufferMin);

        verify(workhorseLogService).logChange(null, null, "Min amount of executions in memory buffer before the poller gets to add more",
                        workhorseConfigDefaults.getBufferMin(), bufferMin, null);
    }

    @Test
    public void testUpdateBufferMin_dontUpdateIfEquals() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        int bufferMin = 50;

        workhorseConfig.setBufferMin(bufferMin);
        classUnderTest.updateBufferMin(workhorseConfig, bufferMin);

        // if nothing has changed, there will be no log and no restart
        verify(workhorseLogService, never()).logChange(anyLong(), any(JobStatus.class), anyString(), anyObject(), anyObject(), anyString());
    }

    @Test
    public void testUpdateBufferPollInterval() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        int bufferPollInterval = 1;

        // Default is 5
        assertEquals(5, workhorseConfig.getBufferPollInterval());

        classUnderTest.updateBufferPollInterval(workhorseConfig, bufferPollInterval);

        assertEquals(bufferPollInterval, workhorseConfig.getBufferPollInterval());
    }

    @Test
    public void testUpdateBufferPollInterval_staticConfigIsChanged() throws Exception {

        StaticConfig.BUFFER_POLL_INTERVAL = 33;

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        int bufferPollInterval = 3;

        classUnderTest.updateBufferPollInterval(workhorseConfig, bufferPollInterval);

        assertEquals(bufferPollInterval, StaticConfig.BUFFER_POLL_INTERVAL);
    }

    @Test
    public void testUpdateBufferPollInterval_dontUpdateIfEquals() throws Exception {

        int bufferPollInterval = 3;

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        workhorseConfig.setBufferPollInterval(bufferPollInterval);

        classUnderTest.updateBufferPollInterval(workhorseConfig, bufferPollInterval);

        // if nothing has changed, there will be no log and no restart
        verify(workhorseLogService, never()).logChange(anyLong(), any(JobStatus.class), anyString(), anyObject(), anyObject(), anyString());
        verify(workhorse, never()).isRunning();
    }

    @Test(expected = RuntimeException.class)
    public void testUpdateBufferPollInterval_toLow() throws Exception {
        classUnderTest.updateBufferPollInterval(new MemoryConfigBuilder().build(), 0);
    }

    @Test
    public void testUpdateBufferPollInterval_toLowMessage() throws Exception {
        try {
            classUnderTest.updateBufferPollInterval(new MemoryConfigBuilder().build(), 0);
            fail("bufferPollInterval to low!");
        } catch (Exception e) {
            assertEquals(RuntimeException.class, e.getClass());
            assertEquals("The buffer poller interval must be between 1 and 60!", e.getMessage());
        }
    }

    @Test
    public void testUpdateBufferPollInterval_Lowest() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        int bufferPollInterval = 1;

        classUnderTest.updateBufferPollInterval(workhorseConfig, bufferPollInterval);
        assertEquals(bufferPollInterval, workhorseConfig.getBufferPollInterval());
    }

    @Test(expected = RuntimeException.class)
    public void testUpdateBufferPollInterval_toHigh() throws Exception {
        classUnderTest.updateBufferPollInterval(new MemoryConfigBuilder().build(), 61);
    }

    @Test
    public void testUpdateBufferPollInterval_toHighMessage() throws Exception {
        try {
            classUnderTest.updateBufferPollInterval(new MemoryConfigBuilder().build(), 61);
            fail("bufferPollInterval to high!");
        } catch (Exception e) {
            assertEquals(RuntimeException.class, e.getClass());
            assertEquals("The buffer poller interval must be between 1 and 60!", e.getMessage());
        }
    }

    @Test
    public void testUpdateBufferPollInterval_Highest() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        int bufferPollInterval = 60;

        classUnderTest.updateBufferPollInterval(workhorseConfig, bufferPollInterval);
        assertEquals(bufferPollInterval, workhorseConfig.getBufferPollInterval());
    }

    @Test
    public void testUpdateBufferPollInterval_restart() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        int bufferPollInterval = 3;

        given(workhorse.isRunning()).willReturn(true);

        classUnderTest.updateBufferPollInterval(workhorseConfig, bufferPollInterval);

        verify(workhorse, times(1)).start();
    }

    @Test
    public void testUpdateBufferPollInterval_restartNotNeeded() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        int bufferPollInterval = 3;

        given(workhorse.isRunning()).willReturn(false);

        classUnderTest.updateBufferPollInterval(workhorseConfig, bufferPollInterval);

        verify(workhorse, never()).start();
    }

    @Test
    public void testUpdateBufferPollInterval_logMessage() throws Exception {

        // Default is 5
        WorkhorseConfig workhorseConfigDefaults = new MemoryConfigBuilder().build();
        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        int bufferPollInterval = 3;

        classUnderTest.updateBufferPollInterval(workhorseConfig, bufferPollInterval);

        verify(workhorseLogService).logChange(null, null, "Buffer poller interval", workhorseConfigDefaults.getBufferPollInterval(), bufferPollInterval, null);
    }

    @Test
    public void testUpdateBufferPollInterval_with_too_high_bufferPollInterval() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        int bufferPollInterval = 61;

        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("The buffer poller interval must be between 1 and 60!");

        classUnderTest.updateBufferPollInterval(workhorseConfig, bufferPollInterval);

    }

    @Test
    public void testUpdateBufferPollInterval_with_too_small_bufferPollInterval() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        int bufferPollInterval = 0;

        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("The buffer poller interval must be between 1 and 60!");

        classUnderTest.updateBufferPollInterval(workhorseConfig, bufferPollInterval);

    }

    @Test
    public void testUpdateBufferPushFallbackPollInterval() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        int bufferPushFallbackPollInterval = 60;

        classUnderTest.updateBufferPushFallbackPollInterval(workhorseConfig, bufferPushFallbackPollInterval);

        assertEquals(bufferPushFallbackPollInterval, workhorseConfig.getBufferPushFallbackPollInterval());
        assertEquals(bufferPushFallbackPollInterval, StaticConfig.BUFFER_PUSH_FALL_BACK_POLL_INTERVAL);
    }

    @Test
    public void testUpdateBufferPushFallbackPollInterval_dontUpdateIfEquals() throws Exception {

        int bufferPushFallbackPollInterval = 3;

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        workhorseConfig.setBufferPushFallbackPollInterval(bufferPushFallbackPollInterval);

        classUnderTest.updateBufferPushFallbackPollInterval(workhorseConfig, bufferPushFallbackPollInterval);

        // if nothing has changed, there will be no log and no restart
        verify(workhorseLogService, never()).logChange(anyLong(), any(JobStatus.class), anyString(), anyObject(), anyObject(), anyString());
        verify(workhorse, never()).isRunning();
    }

    @Test
    public void testUpdateBufferPushFallbackPollInterval_restart() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        int bufferPushFallbackPollInterval = 3;

        given(workhorse.isRunning()).willReturn(true);

        classUnderTest.updateBufferPushFallbackPollInterval(workhorseConfig, bufferPushFallbackPollInterval);

        verify(workhorse, times(1)).start();
    }

    @Test
    public void testUpdateBufferPushFallbackPollInterval_tooLow() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        int bufferPushFallbackPollInterval = 0;

        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("The buffer push fallback poller interval must be higher than 0!");

        classUnderTest.updateBufferPushFallbackPollInterval(workhorseConfig, bufferPushFallbackPollInterval);

    }

    @Test
    public void testUpdateTimeZone() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();

        // Test all available zoneIds of the machine
        for (String timeZone : ZoneId.getAvailableZoneIds()) {

            classUnderTest.updateTimeZone(workhorseConfig, timeZone);

            assertEquals(timeZone, workhorseConfig.getTimeZone());
            assertEquals(timeZone, StaticConfig.TIME_ZONE);
        }

    }

    @Test
    public void testUpdateTimeZone_dontUpdateIfEquals() throws Exception {

        String timeZone = ZoneId.systemDefault().getId();

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        workhorseConfig.setTimeZone(timeZone);

        classUnderTest.updateTimeZone(workhorseConfig, timeZone);

        // if nothing has changed, there will be no log and no restart
        verify(workhorseLogService, never()).logChange(anyLong(), any(JobStatus.class), anyString(), anyObject(), anyObject(), anyString());

    }

    @Test
    public void testUpdateTimeZone_withNullValue() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        String timeZone = null;

        String systemDefault = ZoneId.systemDefault().getId();

        classUnderTest.updateTimeZone(workhorseConfig, timeZone);

        assertEquals(systemDefault, workhorseConfig.getTimeZone());
        assertEquals(systemDefault, StaticConfig.TIME_ZONE);

    }

    @Test
    public void testUpdateTimeZone_withNullValue_logChange() throws Exception {

        WorkhorseConfig workhorseConfigDefaults = new MemoryConfigBuilder().build();
        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        String timeZone = null;
        ZoneId systemDefault = ZoneId.systemDefault();

        classUnderTest.updateTimeZone(workhorseConfig, timeZone);

        verify(workhorseLogService).logChange(null, null, "Time zone", workhorseConfigDefaults.getTimeZone(), systemDefault.getId(),
                        "System default time-zone is used: " + systemDefault);

    }

    @Test
    public void testUpdateTimeZone_NotAvailableZoneIds() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        String timeZone = "Not a valide Time Zone";

        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("Time zone '" + timeZone + "' is not available!");

        classUnderTest.updateTimeZone(workhorseConfig, timeZone);
    }

    @Test
    public void testUpdateTimeZone_logMessage() throws Exception {

        WorkhorseConfig workhorseConfigDefaults = new MemoryConfigBuilder().build();
        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        // Take the first time zone in the list of time zone the system knows
        String timeZone = ZoneId.getAvailableZoneIds().stream().findFirst().get();

        classUnderTest.updateTimeZone(workhorseConfig, timeZone);

        verify(workhorseLogService).logChange(null, null, "Time zone", workhorseConfigDefaults.getTimeZone(), timeZone, null);
    }

    @Test
    public void testUpdateLogChange() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        String logChange = "%s updated from '%s' to '%s'";

        classUnderTest.updateLogChange(workhorseConfig, logChange);

        assertEquals(logChange, workhorseConfig.getLogChange());
        assertEquals(logChange, StaticConfig.LOG_CHANGE);
    }

    @Test
    public void testUpdateLogChange_withNullValue() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        String logChange = null;

        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("The log change pattern is needed!");

        classUnderTest.updateLogChange(workhorseConfig, logChange);

    }

    @Test
    public void testUpdateLogChange_dontUpdateIfEquals() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        String logChange = "%s changed from '%s' to '%s'";
        workhorseConfig.setLogChange(logChange);

        classUnderTest.updateLogChange(workhorseConfig, logChange);

        verify(workhorseLogService, never()).logChange(anyLong(), any(JobStatus.class), anyString(), anyObject(), anyObject(), anyString());

    }

    @Test
    public void testUpdateLogChange_lowNumberOfPlaceholder() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        String logChange = " changed from '%s' to '%s'";

        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("The log change pattern needs the placeholder '%s' three times!");

        classUnderTest.updateLogChange(workhorseConfig, logChange);
    }

    @Test
    public void testUpdateLogChange_TooHighNumberOfPlaceholder() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        String logChange = "'%s' changed from '%s' to '%s' and '%s'";

        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("The log change pattern needs the placeholder '%s' three times!");

        classUnderTest.updateLogChange(workhorseConfig, logChange);
    }

    @Test
    public void testUpdateLogInfoMarker() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        String logInfoMarker = "INFO";

        classUnderTest.updateLogInfoMarker(workhorseConfig, logInfoMarker);

        assertEquals(logInfoMarker, workhorseConfig.getLogInfoMarker());
        assertEquals(logInfoMarker, StaticConfig.LOG_INFO_MARKER);
    }

    @Test
    public void testUpdateLogInfoMarker_dontUpdateIfEquals() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        String logInfoMarker = "info";
        workhorseConfig.setLogInfoMarker(logInfoMarker);

        classUnderTest.updateLogInfoMarker(workhorseConfig, logInfoMarker);

        verify(workhorseLogService, never()).logChange(anyLong(), any(JobStatus.class), anyString(), anyObject(), anyObject(), anyString());
    }

    @Test
    public void testUpdateLogWarnMarker() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        String logWarnMarker = "Attention";

        classUnderTest.updateLogWarnMarker(workhorseConfig, logWarnMarker);

        assertEquals(logWarnMarker, workhorseConfig.getLogWarnMarker());
        assertEquals(logWarnMarker, StaticConfig.LOG_WARN_MARKER);
    }

    @Test
    public void testUpdateLogWarnMarker_dontUpdateIfEquals() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        String logWarnMarker = "warn";
        workhorseConfig.setLogWarnMarker(logWarnMarker);

        classUnderTest.updateLogWarnMarker(workhorseConfig, logWarnMarker);

        verify(workhorseLogService, never()).logChange(anyLong(), any(JobStatus.class), anyString(), anyObject(), anyObject(), anyString());
    }

    @Test
    public void testUpdateLogErrorMarker() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        String logErrorMarker = "Errorr";

        classUnderTest.updateLogErrorMarker(workhorseConfig, logErrorMarker);

        assertEquals(logErrorMarker, workhorseConfig.getLogErrorMarker());
        assertEquals(logErrorMarker, StaticConfig.LOG_ERROR_MARKER);

    }

    @Test
    public void testUpdateLogErrorMarker_dontUpdateIfEquals() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        String logErrorMarker = "error";
        workhorseConfig.setLogErrorMarker(logErrorMarker);

        classUnderTest.updateLogErrorMarker(workhorseConfig, logErrorMarker);

        verify(workhorseLogService, never()).logChange(anyLong(), any(JobStatus.class), anyString(), anyObject(), anyObject(), anyString());

    }

    @Test
    public void testUpdateLogTimeFormatter() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        String logTimeFormat = "'['HH:mm:ss.SS']'";

        classUnderTest.updateLogTimeFormatter(workhorseConfig, logTimeFormat);

        assertEquals(logTimeFormat, workhorseConfig.getLogTimeFormat());
        assertEquals(logTimeFormat, StaticConfig.LOG_TIME_FORMATTER);

    }

    @Test
    public void testUpdateLogTimeFormatter_withNullValue() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        String logTimeFormat = null;

        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("The execution log timestamp pattern is needed!");

        classUnderTest.updateLogTimeFormatter(workhorseConfig, logTimeFormat);
    }

    @Test
    public void testUpdateLogTimeFormatter_dontUpdateIfEquals() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        String logTimeFormat = "'['HH:mm:ss.SSS']'";
        workhorseConfig.setLogTimeFormat(logTimeFormat);

        classUnderTest.updateLogTimeFormatter(workhorseConfig, logTimeFormat);

        verify(workhorseLogService, never()).logChange(anyLong(), any(JobStatus.class), anyString(), anyObject(), anyObject(), anyString());

    }

    @Test
    public void testGetWorkhorseConfig() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().bufferMaximumSize(100L).bufferMinimumSize(3).bufferPollInterval(4).build();

        when(configPersistence.get()).thenReturn(workhorseConfig);

        WorkhorseConfig foundWorkhorseConfig = classUnderTest.getWorkhorseConfig();

        assertEquals(foundWorkhorseConfig, workhorseConfig);

    }

    @Test
    public void testGetWorkhorseConfig_withNulValue() throws Exception {

        WorkhorseConfig workhorseConfig = classUnderTest.getWorkhorseConfig();

        assertEquals(workhorseConfig.toString(), new MemoryConfigBuilder().build().toString());
    }

    @Test
    public void testUpdateWorkhorseConfig() throws Exception {

        WorkhorseConfig workhorseConfigDefaults = new MemoryConfigBuilder().build();
        Long bufferMax = 100L;
        int bufferMix = 3;
        int bufferPollInterval = 4;
        int bufferPushFallbackPollInterval = 60;
        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().bufferMaximumSize(bufferMax).bufferMinimumSize(bufferMix)
                        .bufferPollInterval(bufferPollInterval).bufferPushFallbackPollInterval(bufferPushFallbackPollInterval).build();

        when(configPersistence.get()).thenReturn(workhorseConfigDefaults);

        WorkhorseConfig newWorkhorseConfig = classUnderTest.updateWorkhorseConfig(workhorseConfig);

        assertEquals(newWorkhorseConfig.toString(), workhorseConfig.toString());

    }

    @Test
    public void testUpdateWorkhorseConfig_withNonValidBufferMax() throws Exception {

        WorkhorseConfig workhorseConfigDefaults = new MemoryConfigBuilder().build();

        Long bufferMax = 0L;
        int bufferMix = 3;
        int bufferPollInterval = 4;
        int bufferPushFallbackPollInterval = 60;
        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().bufferMaximumSize(bufferMax).bufferMinimumSize(bufferMix)
                        .bufferPollInterval(bufferPollInterval).bufferPushFallbackPollInterval(bufferPushFallbackPollInterval).build();

        when(configPersistence.get()).thenReturn(workhorseConfigDefaults);

        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("The max amount of executions to load into the memory buffer per job must be higher than 0!");

        classUnderTest.updateWorkhorseConfig(workhorseConfig);

    }

    @Test
    public void testInitializeStaticConfig() throws Exception {

        Long bufferMax = 100L;
        int bufferMix = 3;
        int bufferPollInterval = 4;
        int bufferPushFallbackPollInterval = 60;
        int executionTimeout = 1000;
        String timeZone = "UTC";
        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().bufferMaximumSize(bufferMax).bufferMinimumSize(bufferMix)
                        .bufferPollInterval(bufferPollInterval).bufferPushFallbackPollInterval(bufferPushFallbackPollInterval)
                        .executionTimeout(executionTimeout).timeZone(timeZone).build();

        when(configPersistence.get()).thenReturn(workhorseConfig);
        classUnderTest.initializeStaticConfig(workhorseConfig);

        assertEquals(StaticConfig.BUFFER_MAX, bufferMax);
        assertEquals(StaticConfig.BUFFER_MIN, bufferMix);
        assertEquals(StaticConfig.BUFFER_POLL_INTERVAL, bufferPollInterval);
        assertEquals(StaticConfig.BUFFER_PUSH_FALL_BACK_POLL_INTERVAL, bufferPushFallbackPollInterval);
        assertEquals(StaticConfig.EXECUTION_TIMEOUT, executionTimeout);
        assertEquals(StaticConfig.TIME_ZONE, timeZone);

    }

    @Test
    public void testUpdateMinutesUntilCleanup() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();

        int minutesUntilCleanup = 1;

        classUnderTest.updateMinutesUntilCleanup(workhorseConfig, minutesUntilCleanup);

        assertEquals(StaticConfig.MINUTES_UNTIL_CLEANUP, minutesUntilCleanup);
        assertEquals(workhorseConfig.getMinutesUntilCleanup(), minutesUntilCleanup);

    }

    @Test
    public void testUpdateMinutesUntilCleanup_toLow() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();

        int minutesUntilCleanup = -1;

        classUnderTest.updateMinutesUntilCleanup(workhorseConfig, minutesUntilCleanup);

        assertEquals(0, StaticConfig.MINUTES_UNTIL_CLEANUP);
        assertEquals(0, workhorseConfig.getMinutesUntilCleanup());
    }

    @Test
    public void testUpdateMinutesUntilCleanup_deactiveFeature() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();

        int minutesUntilCleanup = 0;

        classUnderTest.updateMinutesUntilCleanup(workhorseConfig, minutesUntilCleanup);

        assertEquals(minutesUntilCleanup, StaticConfig.MINUTES_UNTIL_CLEANUP);
        assertEquals(minutesUntilCleanup, workhorseConfig.getMinutesUntilCleanup());

    }

    @Test
    public void testUpdateMinutesUntilCleanup_deactiveFeature_logChange() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        WorkhorseConfig workhorseConfigDefault = new MemoryConfigBuilder().build();

        long minutesUntilCleanup = 0L;

        classUnderTest.updateMinutesUntilCleanup(workhorseConfig, minutesUntilCleanup);

        String message = "MinutesUntilCleanup is set to '0', so the cleanup is off!";

        verify(workhorseLogService).logChange(null, null, "minutesUntilCleanup", workhorseConfigDefault.getMinutesUntilCleanup(), minutesUntilCleanup, message);
    }

    @Test
    public void testUpdateExecutionTimeout() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();

        int executionTimeout = 1;

        classUnderTest.updateExecutionTimeout(workhorseConfig, executionTimeout);

        assertEquals(StaticConfig.EXECUTION_TIMEOUT, executionTimeout);
        assertEquals(workhorseConfig.getExecutionTimeout(), executionTimeout);

    }

    @Test
    public void testUpdateExecutionTimeout_logChange() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        WorkhorseConfig workhorseConfigDefault = new MemoryConfigBuilder().build();

        int executionTimeout = 1;

        classUnderTest.updateExecutionTimeout(workhorseConfig, executionTimeout);

        verify(workhorseLogService).logChange(null, null, "Execution timeout", workhorseConfigDefault.getExecutionTimeout(), executionTimeout, null);
    }

    @Test
    public void testUpdateExecutionTimeout_toLow() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();

        int executionTimeout = -1;

        classUnderTest.updateExecutionTimeout(workhorseConfig, executionTimeout);

        assertEquals(0, StaticConfig.EXECUTION_TIMEOUT);
        assertEquals(0, workhorseConfig.getExecutionTimeout());
    }

    @Test
    public void testUpdateExecutionTimeout_deactiveFeature() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();

        int executionTimeout = 0;

        classUnderTest.updateExecutionTimeout(workhorseConfig, executionTimeout);

        assertEquals(executionTimeout, StaticConfig.EXECUTION_TIMEOUT);
        assertEquals(executionTimeout, workhorseConfig.getExecutionTimeout());
    }

    @Test
    public void testUpdateExecutionTimeout_deactiveFeature_logChange() throws Exception {

        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();
        WorkhorseConfig workhorseConfigDefault = new MemoryConfigBuilder().build();

        int executionTimeout = 0;

        classUnderTest.updateExecutionTimeout(workhorseConfig, executionTimeout);

        String message = "Execution timeout is set to '0', so the hunt is off!";

        verify(workhorseLogService).logChange(null, null, "Execution timeout", workhorseConfigDefault.getExecutionTimeout(), executionTimeout, message);
    }

}
