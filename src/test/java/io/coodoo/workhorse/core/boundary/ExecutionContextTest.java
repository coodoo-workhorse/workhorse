package io.coodoo.workhorse.core.boundary;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import io.coodoo.workhorse.core.control.StaticConfig;
import io.coodoo.workhorse.core.entity.Execution;
import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.core.entity.WorkhorseConfig;
import io.coodoo.workhorse.persistence.memory.MemoryConfigBuilder;

@RunWith(MockitoJUnitRunner.class)
public class ExecutionContextTest {

    @Mock
    WorkhorseConfig jobEngineConfig;

    @Mock
    private Job job;

    @Mock
    private Execution execution;

    @InjectMocks
    private ExecutionContext classUnderTest;

    @Before
    public void prepareTest() {
        WorkhorseConfig workhorseConfig = new MemoryConfigBuilder().build();

        StaticConfig.TIME_ZONE = workhorseConfig.getTimeZone();
        StaticConfig.BUFFER_MAX = workhorseConfig.getBufferMax();
        StaticConfig.BUFFER_MIN = workhorseConfig.getBufferMin();
        StaticConfig.BUFFER_POLL_INTERVAL = workhorseConfig.getBufferPollInterval();
        StaticConfig.BUFFER_PUSH_FALL_BACK_POLL_INTERVAL = workhorseConfig.getBufferPushFallbackPollInterval();
        StaticConfig.MINUTES_UNTIL_CLEANUP = workhorseConfig.getMinutesUntilCleanup();
        StaticConfig.EXECUTION_TIMEOUT = workhorseConfig.getExecutionTimeout();
        StaticConfig.EXECUTION_TIMEOUT_STATUS = workhorseConfig.getExecutionTimeoutStatus();
        StaticConfig.LOG_CHANGE = workhorseConfig.getLogChange();
        StaticConfig.LOG_TIME_FORMATTER = workhorseConfig.getLogTimeFormat();
        StaticConfig.LOG_INFO_MARKER = workhorseConfig.getLogInfoMarker();
        StaticConfig.LOG_WARN_MARKER = workhorseConfig.getLogWarnMarker();
        StaticConfig.LOG_ERROR_MARKER = workhorseConfig.getLogErrorMarker();
    }

    @Test
    public void testLogLine() throws Exception {

        given(execution.getLog()).willReturn(null);
        String message = "xxx";

        classUnderTest.init(execution);
        classUnderTest.logLine(message);

        String result = classUnderTest.getLog();

        assertTrue(result.startsWith("xxx"));
        assertEquals(3, result.length());
    }

    @Test
    public void testLogLine_3lines() throws Exception {

        given(execution.getLog()).willReturn(null);
        String message = "xxx";

        classUnderTest.init(execution);
        classUnderTest.logLine(message);
        classUnderTest.logLine(message);
        classUnderTest.logLine(message);

        String result = classUnderTest.getLog();

        assertTrue(result.startsWith("xxx"));
        assertEquals(11, result.length());

        assertEquals("xxxxxxxxx", result.replace(System.lineSeparator(), ""));
    }

    @Test
    public void testLogLineWithTimestamp() throws Exception {

        given(execution.getLog()).willReturn(null);
        String message = "xxx";

        classUnderTest.init(execution);
        classUnderTest.logLineWithTimestamp(message);

        String result = classUnderTest.getLog();

        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("] xxx"));
        assertEquals(18, result.length());
        assertFalse(result.contains(System.lineSeparator()));
    }

    @Test
    public void testLogInfo() throws Exception {

        given(execution.getLog()).willReturn(null);
        Logger logger = mock(Logger.class);
        String message = "xxx";

        classUnderTest.init(execution);
        classUnderTest.logInfo(logger, message);

        verify(logger).info(message);

        String result = classUnderTest.getLog();

        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("]  xxx"));
        assertEquals(19, result.length());
    }

    @Test
    public void testLogWarn() throws Exception {

        given(execution.getLog()).willReturn(null);
        Logger logger = mock(Logger.class);
        String message = "xxx";

        classUnderTest.init(execution);
        classUnderTest.logWarn(logger, message);

        verify(logger).warn(message);

        String result = classUnderTest.getLog();

        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("] [WARN] xxx"));
        assertEquals(25, result.length());
    }

    @Test
    public void testLogWarn_2lines() throws Exception {

        given(execution.getLog()).willReturn(null);
        Logger logger = mock(Logger.class);
        String message = "xxx";

        classUnderTest.init(execution);
        classUnderTest.logWarn(logger, message);
        classUnderTest.logWarn(logger, message);

        verify(logger, times(2)).warn(message);

        String result = classUnderTest.getLog();

        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("] [WARN] xxx"));
        assertTrue(result.contains(System.lineSeparator()));
        assertEquals(51, result.length());
    }

    @Test
    public void testLogError() throws Exception {

        given(execution.getLog()).willReturn(null);
        Logger logger = mock(Logger.class);
        String message = "xxx";

        classUnderTest.init(execution);
        classUnderTest.logError(logger, message);

        verify(logger).error(message);

        String result = classUnderTest.getLog();

        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("] [ERROR] xxx"));
        assertEquals(26, result.length());
    }

    @Test
    public void testLogError_2lines() throws Exception {

        given(execution.getLog()).willReturn(null);
        Logger logger = mock(Logger.class);
        String message = "xxx";

        classUnderTest.init(execution);
        classUnderTest.logError(logger, message);
        classUnderTest.logError(logger, message);

        verify(logger, times(2)).error(message);

        String result = classUnderTest.getLog();

        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("] [ERROR] xxx"));
        assertTrue(result.contains(System.lineSeparator()));
        assertEquals(53, result.length());
    }

    @Test
    public void testInit_withExecutionNull() throws Exception {

        classUnderTest.init(null);

        assertNotNull(classUnderTest.logBuffer);
        assertEquals("", classUnderTest.logBuffer.toString());
    }

    @Test
    public void testInit_positiv() throws Exception {

        Execution execution = new Execution();
        execution.setLog("Example log to test the method");

        classUnderTest.init(execution);

        assertEquals(execution.getLog(), classUnderTest.logBuffer.toString());
    }

    @Test
    public void testInit_withLogNull() throws Exception {

        Execution execution = new Execution();

        classUnderTest.init(execution);

        assertEquals("", classUnderTest.logBuffer.toString());
        assertNotNull(classUnderTest.logBuffer);
    }

    @Test
    public void testAppendLog_withLogBufferNull() {

        String message = "Message to test";
        boolean timestamp = true;
        String mode = "e";

        classUnderTest.appendLog(message, timestamp, mode);

        assertNull(classUnderTest.logBuffer);
    }

    @Test
    public void testAppendLog_with_LOG_ERROR_MARKER_NULL() {

        String message = "Message to test";
        boolean timestamp = false;
        String mode = "e";

        classUnderTest.init(execution);

        StaticConfig.LOG_ERROR_MARKER = null;

        classUnderTest.appendLog(message, timestamp, mode);

        assertEquals(message, classUnderTest.getLog());
    }

    @Test
    public void testAppendLog_with_LOG_INFO_MARKER_NULL() {
        String message = "Message to test";
        boolean timestamp = false;
        String mode = "i";

        classUnderTest.init(execution);
        StaticConfig.LOG_INFO_MARKER = null;

        classUnderTest.appendLog(message, timestamp, mode);

        assertEquals(message, classUnderTest.getLog());

    }

    @Test
    public void testAppendLog_with_LOG_WARN_MARKER_NULL() {
        String message = "Message to test";
        boolean timestamp = false;
        String mode = "w";

        classUnderTest.init(execution);
        StaticConfig.LOG_WARN_MARKER = null;

        classUnderTest.appendLog(message, timestamp, mode);

        assertEquals(message, classUnderTest.getLog());

    }

    @Test
    public void getLog_with_logBuffer_NULL() {

        String log = classUnderTest.getLog();

        assertNull(classUnderTest.logBuffer);
        assertNull(log);
    }

    @Test
    public void getLog_with_logBuffer_toLow() {

        classUnderTest.logBuffer = new StringBuffer();

        String log = classUnderTest.getLog();

        assertNotNull(classUnderTest.logBuffer);
        assertNull(log);
    }

}
