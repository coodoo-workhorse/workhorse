package io.coodoo.workhorse.core.boundary;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import io.coodoo.workhorse.core.control.StaticConfig;
import io.coodoo.workhorse.core.entity.Execution;
import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.core.entity.WorkhorseConfig;
import io.coodoo.workhorse.persistence.interfaces.ExecutionPersistence;
import io.coodoo.workhorse.persistence.memory.MemoryConfigBuilder;

@RunWith(MockitoJUnitRunner.class)
public class ExecutionContextTest {

    @Mock
    ExecutionPersistence executionPersistence;

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

        String given = "xxx";
        String expected = "xxx";

        classUnderTest.init(execution);
        classUnderTest.logLine(given);

        verify(executionPersistence).log(execution.getJobId(), execution.getId(), expected);
    }

    @Test
    public void testLogLine_serverLog() throws Exception {

        String message = "xxx";
        Logger logger = mock(Logger.class);

        classUnderTest.init(execution);
        classUnderTest.logLine(logger, message);

        verify(logger).info(message);
    }

    @Test
    public void testLogInfo() throws Exception {

        String message = "xxx";
        String expected = StaticConfig.LOG_INFO_MARKER + " xxx";

        classUnderTest.init(execution);
        classUnderTest.appendLog(message, false, "i");

        verify(executionPersistence).log(execution.getJobId(), execution.getId(), expected);

    }

    @Test
    public void testLogInfo_serverLog() throws Exception {

        String message = "xxx";
        Logger logger = mock(Logger.class);

        classUnderTest.init(execution);
        classUnderTest.logInfo(logger, message);

        verify(logger).info(message);
    }

    @Test
    public void testLogWarn() throws Exception {

        String message = "xxx";
        String expected = StaticConfig.LOG_WARN_MARKER + " xxx";

        classUnderTest.init(execution);
        classUnderTest.appendLog(message, false, "w");

        verify(executionPersistence).log(execution.getJobId(), execution.getId(), expected);

    }

    @Test
    public void testLogWarn_serverLog() throws Exception {

        String message = "xxx";
        Logger logger = mock(Logger.class);

        classUnderTest.init(execution);
        classUnderTest.logWarn(logger, message);

        verify(logger).warn(message);
    }

    @Test
    public void testLogError() throws Exception {

        String message = "xxx";
        String expected = StaticConfig.LOG_ERROR_MARKER + " xxx";

        classUnderTest.init(execution);
        classUnderTest.appendLog(message, false, "e");

        verify(executionPersistence).log(execution.getJobId(), execution.getId(), expected);

    }

    @Test
    public void testLogError_serverLog() throws Exception {

        String message = "xxx";
        Logger logger = mock(Logger.class);

        classUnderTest.init(execution);
        classUnderTest.logError(logger, message);

        verify(logger).error(message);

    }

    @Test
    public void testLogError_withThrowable_serverLog() throws Exception {

        String message = "xxx";
        Logger logger = mock(Logger.class);
        Throwable throwable = new Throwable();

        classUnderTest.init(execution);
        classUnderTest.logError(logger, message, throwable);

        verify(logger).error(message, throwable);

    }

    @Test
    public void testAppendLog_with_LOG_ERROR_MARKER_NULL() {

        String message = "xxx";
        StaticConfig.LOG_ERROR_MARKER = null;

        classUnderTest.init(execution);
        classUnderTest.appendLog(message, false, "e");

        verify(executionPersistence).log(execution.getJobId(), execution.getId(), message);
    }

    @Test
    public void testAppendLog_with_LOG_INFO_MARKER_NULL() {

        String message = "Message to test";
        StaticConfig.LOG_INFO_MARKER = null;

        classUnderTest.init(execution);
        classUnderTest.appendLog(message, false, "i");

        verify(executionPersistence).log(execution.getJobId(), execution.getId(), message);

    }

    @Test
    public void testAppendLog_with_LOG_WARN_MARKER_NULL() {

        String message = "Message to test";
        StaticConfig.LOG_WARN_MARKER = null;

        classUnderTest.init(execution);
        classUnderTest.appendLog(message, false, "w");

        verify(executionPersistence).log(execution.getJobId(), execution.getId(), message);

    }

    @Test
    public void testSummarize_positiv_szenario() throws Exception {

        String summary = "Message";
        StaticConfig.MAX_EXECUTION_SUMMARY_LENGTH = 10;

        Execution executionOfTheContext = new Execution();
        executionOfTheContext.setJobId(1L);
        executionOfTheContext.setId(1L);

        classUnderTest.init(executionOfTheContext);
        classUnderTest.summarize(summary);

        ArgumentCaptor<Execution> argument = ArgumentCaptor.forClass(Execution.class);
        verify(executionPersistence).update(argument.capture());
        assertEquals(summary, argument.getValue().getSummary());
    }

    @Test
    public void testSummarize_with_too_long_summary() throws Exception {

        String summary = "Message to test.";
        StaticConfig.MAX_EXECUTION_SUMMARY_LENGTH = 10;

        String expectedSummary = "Message t…";
        assertEquals("Summary length must not exceed value of MAX_EXECUTION_SUMMARY_LENGTH!", StaticConfig.MAX_EXECUTION_SUMMARY_LENGTH,
                        expectedSummary.length());

        Execution executionOfTheContext = new Execution();
        executionOfTheContext.setJobId(1L);
        executionOfTheContext.setId(1L);

        classUnderTest.init(executionOfTheContext);
        classUnderTest.summarize(summary);

        Execution expectedExecution = execution;
        expectedExecution.setSummary(summary);

        ArgumentCaptor<Execution> argument = ArgumentCaptor.forClass(Execution.class);
        verify(executionPersistence).update(argument.capture());

        assertEquals(expectedSummary, argument.getValue().getSummary());
    }

    @Test
    public void testSummarize_persist_as_log_summary_too_long() throws Exception {

        String summary = "Message to test.";
        StaticConfig.MAX_EXECUTION_SUMMARY_LENGTH = 10;

        Execution executionOfTheContext = new Execution();
        executionOfTheContext.setJobId(1L);
        executionOfTheContext.setId(1L);

        classUnderTest.init(executionOfTheContext);
        classUnderTest.summarize(summary);

        Execution expectedExecution = execution;
        expectedExecution.setSummary(summary);

        ArgumentCaptor<String> argumentString = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> argumentJobId = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> argumentExecutionId = ArgumentCaptor.forClass(Long.class);
        verify(executionPersistence).log(argumentJobId.capture(), argumentExecutionId.capture(), argumentString.capture());

        assertTrue(argumentString.getValue().endsWith("[SUMMARY]" + " " + summary));

    }

    @Test
    public void testSummarize_with_NULL_as_parameter() throws Exception {

        Execution executionOfTheContext = new Execution();
        executionOfTheContext.setJobId(1L);
        executionOfTheContext.setId(1L);

        classUnderTest.init(executionOfTheContext);
        classUnderTest.summarize(null);

        verify(executionPersistence, never()).update(anyObject());

    }

    @Test
    public void testSummarize_with_EMPTY_parameter() throws Exception {

        Execution executionOfTheContext = new Execution();
        executionOfTheContext.setJobId(1L);
        executionOfTheContext.setId(1L);

        classUnderTest.init(executionOfTheContext);
        classUnderTest.summarize("         ");

        verify(executionPersistence, never()).update(anyObject());

    }
}
