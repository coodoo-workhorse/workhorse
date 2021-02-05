// package io.coodoo.workhorse.core.boundary;
//
// import static org.junit.Assert.assertEquals;
// import static org.junit.Assert.assertFalse;
// import static org.junit.Assert.assertTrue;
// import static org.mockito.BDDMockito.given;
// import static org.mockito.Mockito.mock;
// import static org.mockito.Mockito.times;
// import static org.mockito.Mockito.verify;
//
// import org.junit.Test;
// import org.junit.runner.RunWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.runners.MockitoJUnitRunner;
// import org.slf4j.Logger;
//
// import io.coodoo.workhorse.config.entity.WorkhorseConfig;
// import io.coodoo.workhorse.core.entity.Execution;
// import io.coodoo.workhorse.core.entity.Job;
// import jdk.nashorn.internal.ir.annotations.Ignore;
//
// @RunWith(MockitoJUnitRunner.class)
// public class JobContextTest {
//
// @Mock
// WorkhorseConfig jobEngineConfig;
//
// @Mock
// private Job job;
//
// @Mock
// private Execution jobExecution;
//
// @InjectMocks
// private JobContext jobContext;
//
// @Test
// public void testLogLine() throws Exception {
//
// given(jobExecution.getLog()).willReturn(null);
// String message = "xxx";
//
// jobContext.init(jobExecution);
// jobContext.logLine(message);
//
// String result = jobContext.getLog();
//
// assertTrue(result.startsWith("xxx"));
// assertEquals(3, result.length());
// }
//
// @Test
// public void testLogLine_3lines() throws Exception {
//
// given(jobExecution.getLog()).willReturn(null);
// String message = "xxx";
//
// jobContext.init(jobExecution);
// jobContext.logLine(message);
// jobContext.logLine(message);
// jobContext.logLine(message);
//
// String result = jobContext.getLog();
//
// assertTrue(result.startsWith("xxx"));
// assertEquals(11, result.length());
//
// assertEquals("xxxxxxxxx", result.replace(System.lineSeparator(), ""));
// }
//
// @Ignore
// @Test
// public void testLogLineWithTimestamp() throws Exception {
//
// given(jobExecution.getLog()).willReturn(null);
// String message = "xxx";
//
// jobContext.init(jobExecution);
// jobContext.logLineWithTimestamp(message);
//
// String result = jobContext.getLog();
//
// assertTrue(result.startsWith("["));
// assertTrue(result.endsWith("] xxx"));
// assertEquals(18, result.length());
// assertFalse(result.contains(System.lineSeparator()));
// }
//
// @Ignore
// @Test
// public void testLogInfo() throws Exception {
//
// given(jobExecution.getLog()).willReturn(null);
// Logger logger = mock(LoggerFactory.class);
// String message = "xxx";
//
// jobContext.init(jobExecution);
// jobContext.logInfo(logger, message);
//
// verify(logger).info(message);
//
// String result = jobContext.getLog();
//
// assertTrue(result.startsWith("["));
// assertTrue(result.endsWith("] xxx"));
// assertEquals(18, result.length());
// }
//
// @Ignore
// @Test
// public void testLogWarn() throws Exception {
//
// given(jobExecution.getLog()).willReturn(null);
// Logger logger = mock(LoggerFactory.class);
// String message = "xxx";
//
// jobContext.init(jobExecution);
// jobContext.logWarn(logger, message);
//
// verify(logger).warn(message);
//
// String result = jobContext.getLog();
//
// assertTrue(result.startsWith("["));
// assertTrue(result.endsWith("] [WARN] xxx"));
// assertEquals(25, result.length());
// }
//
// @Ignore
// @Test
// public void testLogWarn_2lines() throws Exception {
//
// given(jobExecution.getLog()).willReturn(null);
// Logger logger = mock(LoggerFactory.class);
// String message = "xxx";
//
// jobContext.init(jobExecution);
// jobContext.logWarn(logger, message);
// jobContext.logWarn(logger, message);
//
// verify(logger, times(2)).warn(message);
//
// String result = jobContext.getLog();
//
// assertTrue(result.startsWith("["));
// assertTrue(result.endsWith("] [WARN] xxx"));
// assertTrue(result.contains(System.lineSeparator()));
// assertEquals(51, result.length());
// }
//
// @Ignore
// @Test
// public void testLogError() throws Exception {
//
// given(jobExecution.getLog()).willReturn(null);
// Logger logger = mock(LoggerFactory.class);
// String message = "xxx";
//
// jobContext.init(jobExecution);
// jobContext.logError(logger, message);
//
// verify(logger).error(message);
//
// String result = jobContext.getLog();
//
// assertTrue(result.startsWith("["));
// assertTrue(result.endsWith("] [ERROR] xxx"));
// assertEquals(26, result.length());
// }
//
// @Ignore
// @Test
// public void testLogError_2lines() throws Exception {
//
// given(jobExecution.getLog()).willReturn(null);
// Logger logger = mock(LoggerFactory.class);
// String message = "xxx";
//
// jobContext.init(jobExecution);
// jobContext.logError(logger, message);
// jobContext.logError(logger, message);
//
// verify(logger, times(2)).error(message);
//
// String result = jobContext.getLog();
//
// assertTrue(result.startsWith("["));
// assertTrue(result.endsWith("] [ERROR] xxx"));
// assertTrue(result.contains(System.lineSeparator()));
// assertEquals(53, result.length());
// }
//
// }
