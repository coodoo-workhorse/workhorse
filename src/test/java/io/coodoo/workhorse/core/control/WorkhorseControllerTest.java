package io.coodoo.workhorse.core.control;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.enterprise.event.Event;
import javax.enterprise.inject.spi.BeanManager;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import io.coodoo.workhorse.core.boundary.Worker;
import io.coodoo.workhorse.core.boundary.WorkhorseLogService;
import io.coodoo.workhorse.core.boundary.annotation.InitialJobConfig;
import io.coodoo.workhorse.core.control.event.JobErrorEvent;
import io.coodoo.workhorse.core.entity.Execution;
import io.coodoo.workhorse.core.entity.ExecutionStatus;
import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.core.entity.JobStatus;
import io.coodoo.workhorse.persistence.interfaces.ExecutionPersistence;
import io.coodoo.workhorse.persistence.interfaces.JobPersistence;
import io.coodoo.workhorse.util.WorkhorseUtil;

@RunWith(MockitoJUnitRunner.class)
public class WorkhorseControllerTest {

    private final static String TEST_WORKER_WITH_INITIAL_JOB_CONFIG_NAME = "Test Worker with InitialJobConfig";
    private final static int THREADS = 3;
    private final static String SCHEDULE = "30 * * * * *";
    private final static String NON_VALID_SCHEDULE = " ";
    private final static String TAGS = "unit, test, java";
    private final static int MAX_PER_MINUTE = 1000;
    private final static int MINUTES_UNTIL_CLEANUP = 400;

    public class TestWorker extends Worker {
        @Override
        public void doWork() throws Exception {
            return;
        }
    }

    @InitialJobConfig(name = TEST_WORKER_WITH_INITIAL_JOB_CONFIG_NAME, threads = THREADS)
    public class TestWorkerWithInitialConfig extends Worker {
        @Override
        public void doWork() throws Exception {
            return;
        }
    }

    @InitialJobConfig(schedule = SCHEDULE)
    public class TestWorkerScheduler extends Worker {
        @Override
        public void doWork() throws Exception {
            return;
        }
    }

    @InitialJobConfig(schedule = NON_VALID_SCHEDULE)
    public class TestWorkerNonValidScheduler extends Worker {
        @Override
        public void doWork() throws Exception {
            return;
        }
    }

    @InitialJobConfig(tags = TAGS)
    public class TestWorkerWithTags extends Worker {
        @Override
        public void doWork() throws Exception {
            return;
        }
    }

    @InitialJobConfig(maxPerMinute = MAX_PER_MINUTE)
    public class TestWorkerWithMaxPerMinute extends Worker {
        @Override
        public void doWork() throws Exception {
            return;
        }
    }

    @InitialJobConfig(minutesUntilCleanUp = MINUTES_UNTIL_CLEANUP)
    public class TestWorkerWithInitialConfigMinuteUntilCleanup extends Worker {
        @Override
        public void doWork() throws Exception {
            return;
        }
    }

    @Mock
    ExecutionPersistence executionPersistence;

    @Mock
    WorkhorseLogService workhorseLogService;

    @Mock
    JobPersistence jobPersistence;

    @Mock
    Event<JobErrorEvent> jobErrorEvent;

    @Mock
    BeanManager beanManager;

    @InjectMocks
    WorkhorseController classUnderTest;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void testHuntTimeoutExecutions() throws Exception {

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

        classUnderTest.huntTimeoutExecution();

        for (Execution zombiExecution : timeoutExecutions) {
            assertEquals(ExecutionStatus.ABORTED, zombiExecution.getStatus());

            String logMessage = "Zombie execution found (ID: " + zombiExecution.getId() + "): ";
            verify(workhorseLogService).logMessage(logMessage + "Put in status " + ExecutionStatus.ABORTED, zombiExecution.getJobId(), false);

            verify(executionPersistence).update(zombiExecution);
        }

    }

    // The method executionPersistence.persist() of the called method createRetryExecution() can not be mocked.
    @Ignore
    @Test
    public void testHuntTimeoutExecutions_ExecutionTimeoutStatus_is_QUEUED() throws Exception {

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

        // when(WorkhorseUtil.timestamp()).thenReturn(null);
        when(executionPersistence.findTimeoutExecutions(anyObject())).thenReturn(timeoutExecutions);

        classUnderTest.huntTimeoutExecution();

        for (Execution zombiExecution : timeoutExecutions) {
            assertEquals(ExecutionStatus.FAILED, zombiExecution.getStatus());

            String logMessage = "Zombie execution found (ID: " + zombiExecution.getId() + "): ";
            verify(workhorseLogService).logMessage(logMessage + "Marked as failed and queued a clone", zombiExecution.getJobId(), false);

            verify(executionPersistence).update(zombiExecution);
        }
    }

    @Test
    public void testHuntTimeoutExecutions_ExecutionTimeoutStatus_is_RUNNING() throws Exception {

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

        classUnderTest.huntTimeoutExecution();

        for (Execution zombiExecution : timeoutExecutions) {
            assertEquals(ExecutionStatus.RUNNING, zombiExecution.getStatus());

            String logMessage = "Zombie execution found (ID: " + zombiExecution.getId() + "): ";
            verify(workhorseLogService).logMessage(logMessage + "No action is taken", zombiExecution.getJobId(), false);

            verify(executionPersistence).update(zombiExecution);
        }
    }

    @Test
    public void testHuntTimeoutExecutions_withEmptyList() throws Exception {

        StaticConfig.EXECUTION_TIMEOUT = 12;
        StaticConfig.EXECUTION_TIMEOUT_STATUS = ExecutionStatus.ABORTED;
        StaticConfig.TIME_ZONE = "UTC";

        List<Execution> timeoutExecutions = new ArrayList<>();

        when(executionPersistence.findTimeoutExecutions(anyObject())).thenReturn(timeoutExecutions);

        classUnderTest.huntTimeoutExecution();

        verify(executionPersistence).findTimeoutExecutions(anyObject());

        verify(workhorseLogService, never()).logMessage(anyString(), anyLong(), anyBoolean());

        verify(executionPersistence, never()).update(anyObject());
    }

    @Test
    public void testHuntTimeoutExecutions_ToLow_ExecutionTimeout() throws Exception {

        StaticConfig.EXECUTION_TIMEOUT = 0;

        classUnderTest.huntTimeoutExecution();

        verify(executionPersistence, never()).findTimeoutExecutions(anyObject());

        verify(workhorseLogService, never()).logMessage(anyString(), anyLong(), anyBoolean());

        verify(executionPersistence, never()).update(anyObject());
    }

    @Test
    public void testCreateExecution_uniqueQueued_deactivated() throws Exception {

        Long jobId = 1L;
        Long executionId = 1L;
        String parameters = "parameter";
        boolean priority = false;
        LocalDateTime plannedFor = null;
        LocalDateTime expiresAt = null;
        Long batchId = null;
        Long chainId = null;

        boolean uniqueQueued = false;

        Execution newexecution = new Execution();

        newexecution.setId(executionId);
        newexecution.setJobId(jobId);
        newexecution.setParameters(parameters);
        newexecution.setStatus(ExecutionStatus.QUEUED);

        when(executionPersistence.persist(anyObject())).thenReturn(newexecution);

        Execution execution = classUnderTest.createExecution(jobId, parameters, priority, plannedFor, expiresAt, batchId, chainId, uniqueQueued);

        verify(executionPersistence, never()).getFirstCreatedByJobIdAndParametersHash(jobId, parameters.hashCode());

        verify(executionPersistence).persist(anyObject());
        assertEquals(parameters, execution.getParameters());
    }

    @Test
    public void testCreateExecution_uniqueQueued_activ() throws Exception {

        Long jobId = 1L;
        Long executionId = 1L;
        String parameters = "parameter";
        boolean priority = false;
        LocalDateTime plannedFor = null;
        LocalDateTime expiresAt = null;
        Long batchId = null;
        Long chainId = null;

        boolean uniqueQueued = true;

        Execution newexecution = new Execution();

        newexecution.setId(executionId);
        newexecution.setJobId(jobId);
        newexecution.setParameters(parameters);
        newexecution.setStatus(ExecutionStatus.QUEUED);

        when(executionPersistence.persist(anyObject())).thenReturn(newexecution);

        classUnderTest.createExecution(jobId, parameters, priority, plannedFor, expiresAt, batchId, chainId, uniqueQueued);

        verify(executionPersistence).getFirstCreatedByJobIdAndParametersHash(jobId, parameters.hashCode());

    }

    @Test
    public void testCreateExecution_uniqueQueued_activ_and_execution_found() throws Exception {

        Long jobId = 1L;
        Long executionId = 1L;
        String parameters = "parameter";
        boolean priority = false;
        LocalDateTime plannedFor = null;
        LocalDateTime expiresAt = null;
        Long batchId = null;
        Long chainId = null;
        boolean uniqueQueued = true;

        Execution foundExecution = new Execution();
        when(executionPersistence.getFirstCreatedByJobIdAndParametersHash(jobId, parameters.hashCode())).thenReturn(foundExecution);

        Execution newexecution = new Execution();

        newexecution.setId(executionId);
        newexecution.setJobId(jobId);
        newexecution.setParameters(parameters);
        newexecution.setStatus(ExecutionStatus.QUEUED);

        when(executionPersistence.persist(anyObject())).thenReturn(newexecution);

        Execution execution = classUnderTest.createExecution(jobId, parameters, priority, plannedFor, expiresAt, batchId, chainId, uniqueQueued);

        // check if the search after an execution with given parameter succeed
        verify(executionPersistence).getFirstCreatedByJobIdAndParametersHash(jobId, parameters.hashCode());

        // This execution don't have to be persisted
        verify(executionPersistence, never()).persist(anyObject());

        assertEquals(foundExecution, execution);
    }

    @Test
    public void testCreateExecution_uniqueQueued_activ_parameters_null() throws Exception {

        Long jobId = 1L;
        Long executionId = 1L;
        String parameters = null;
        boolean priority = false;
        LocalDateTime plannedFor = null;
        LocalDateTime expiresAt = null;
        Long batchId = null;
        Long chainId = null;
        boolean uniqueQueued = true;

        Execution newexecution = new Execution();

        newexecution.setId(executionId);
        newexecution.setJobId(jobId);
        newexecution.setStatus(ExecutionStatus.QUEUED);

        when(executionPersistence.persist(anyObject())).thenReturn(newexecution);

        Execution execution = classUnderTest.createExecution(jobId, parameters, priority, plannedFor, expiresAt, batchId, chainId, uniqueQueued);

        verify(executionPersistence).getFirstCreatedByJobIdAndParametersHash(jobId, null);

        verify(executionPersistence).persist(anyObject());

        assertNull(execution.getParameters());
        assertNull(execution.getParametersHash());

    }

    @Test
    public void testCreateExecution_uniqueQueued_activ_and_parameters_with_space() throws Exception {

        Long jobId = 1L;
        Long executionId = 1L;
        String parameters = "  ";
        boolean uniqueQueued = true;

        Execution newexecution = new Execution();

        newexecution.setId(executionId);
        newexecution.setJobId(jobId);
        newexecution.setStatus(ExecutionStatus.QUEUED);

        when(executionPersistence.persist(anyObject())).thenReturn(newexecution);

        Execution execution = classUnderTest.createExecution(jobId, parameters, false, null, null, null, null, uniqueQueued);

        verify(executionPersistence).getFirstCreatedByJobIdAndParametersHash(jobId, null);

        verify(executionPersistence).persist(anyObject());

        assertNull(execution.getParameters());
        assertNull(execution.getParametersHash());
    }

    @Test
    public void testCreateExecution_uniqueQueued_activ_and_parameters() throws Exception {

        Long jobId = 1L;
        Long executionId = 1L;
        String parameters = "";

        // feature to test
        boolean uniqueQueued = true;

        Execution newexecution = new Execution();

        newexecution.setId(executionId);
        newexecution.setJobId(jobId);
        newexecution.setStatus(ExecutionStatus.QUEUED);

        when(executionPersistence.persist(anyObject())).thenReturn(newexecution);

        Execution execution = classUnderTest.createExecution(jobId, parameters, false, null, null, null, null, uniqueQueued);

        verify(executionPersistence).getFirstCreatedByJobIdAndParametersHash(jobId, null);

        verify(executionPersistence).persist(anyObject());

        assertNull(execution.getParameters());
        assertNull(execution.getParametersHash());
    }

    @Test
    public void testCreateExecution_priority_NULL() throws Exception {

        Long jobId = 1L;
        Long executionId = 1L;

        // Null value is tested
        Boolean priority = null;

        Execution newexecution = new Execution();

        newexecution.setId(executionId);
        newexecution.setJobId(jobId);
        newexecution.setStatus(ExecutionStatus.QUEUED);

        when(executionPersistence.persist(anyObject())).thenReturn(newexecution);

        Execution execution = classUnderTest.createExecution(jobId, null, priority, null, null, null, null, false);

        verify(executionPersistence).persist(anyObject());
        assertFalse(execution.isPriority());

    }

    @Test
    public void testCreateExecution_chainId() throws Exception {

        Long jobId = 1L;
        Long executionId = 1L;
        String parameters = "parameter";

        // Value to test
        Long chainId = 1L;

        Execution newexecution = new Execution();

        newexecution.setId(executionId);
        newexecution.setJobId(jobId);
        newexecution.setChainId(chainId);
        newexecution.setStatus(ExecutionStatus.QUEUED);

        when(executionPersistence.persist(anyObject())).thenReturn(newexecution);

        Execution persistedExecution = classUnderTest.createExecution(jobId, parameters, false, null, null, chainId, null, false);

        verify(executionPersistence).persist(anyObject());
        assertNotNull(persistedExecution);
        assertEquals(chainId, persistedExecution.getChainId());

    }

    @Test
    public void testCreateExecution_with_plannedFor_in_the_past() throws Exception {

        StaticConfig.TIME_ZONE = "UTC";

        Long jobId = 1L;
        Long executionId = 1L;

        LocalDateTime plannedFor = LocalDateTime.now(ZoneId.of(StaticConfig.TIME_ZONE));

        Execution newexecution = new Execution();

        newexecution.setId(executionId);
        newexecution.setJobId(jobId);
        newexecution.setPlannedFor(plannedFor);
        newexecution.setStatus(ExecutionStatus.PLANNED);

        when(executionPersistence.persist(anyObject())).thenReturn(newexecution);
        Execution persistedExecution = classUnderTest.createExecution(jobId, null, false, plannedFor, null, null, null, false);

        verify(executionPersistence).persist(anyObject());
        assertNotNull(persistedExecution);
        assertEquals(ExecutionStatus.PLANNED, persistedExecution.getStatus());
        assertEquals(plannedFor, persistedExecution.getPlannedFor());
    }

    @Test
    public void testCreateExecution_with_plannedFor_in_the_future() throws Exception {

        StaticConfig.TIME_ZONE = "UTC";

        Long jobId = 1L;
        Long executionId = 1L;

        LocalDateTime plannedFor = LocalDateTime.now(ZoneId.of(StaticConfig.TIME_ZONE)).plusSeconds(30);

        Execution newexecution = new Execution();

        newexecution.setId(executionId);
        newexecution.setJobId(jobId);
        newexecution.setPlannedFor(plannedFor);
        newexecution.setStatus(ExecutionStatus.PLANNED);

        when(executionPersistence.persist(anyObject())).thenReturn(newexecution);

        Execution persistedExecution = classUnderTest.createExecution(jobId, null, false, plannedFor, null, null, null, false);

        verify(executionPersistence).persist(anyObject());
        assertNotNull(persistedExecution);
        assertEquals(ExecutionStatus.PLANNED, persistedExecution.getStatus());
        assertEquals(plannedFor, persistedExecution.getPlannedFor());
    }

    @Test
    public void testCreateExecution_with_persisited_execution_null() throws Exception {

        Long jobId = 1L;

        Execution newexecution = new Execution();

        newexecution.setJobId(jobId);
        newexecution.setStatus(ExecutionStatus.QUEUED);

        when(executionPersistence.persist(anyObject())).thenReturn(null);

        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("The execution " + newexecution + " couldn't be persisited by the persisitence.");

        classUnderTest.createExecution(jobId, null, false, null, null, null, null, false);
    }

    @Test
    public void testCreateExecution_without_returned_execution_id() throws Exception {

        Long jobId = 1L;

        Execution newexecution = new Execution();

        newexecution.setJobId(jobId);
        newexecution.setStatus(ExecutionStatus.QUEUED);

        when(executionPersistence.persist(any())).thenReturn(newexecution);

        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("The execution " + newexecution + " couldn't be persisited by the persisitence.");

        classUnderTest.createExecution(jobId, null, false, null, null, null, null, false);
    }

    @Test
    public void testCreateJob() throws Exception {

        Class<?> workerClass = TestWorker.class;
        StaticConfig.MINUTES_UNTIL_CLEANUP = 4000;

        // Job with Id to bypass the trigger of exception
        Job job = new Job();
        job.setId(1L);
        when(jobPersistence.persist(anyObject())).thenReturn(job);

        classUnderTest.createJob(workerClass);

        ArgumentCaptor<Job> argument = ArgumentCaptor.forClass(Job.class);
        verify(jobPersistence).persist(argument.capture());
        assertEquals(workerClass.getSimpleName(), argument.getValue().getName());
        assertEquals(workerClass.getName(), argument.getValue().getWorkerClassName());
        assertEquals(InitialJobConfig.JOB_CONFIG_UNIQUE_IN_QUEUE, argument.getValue().isUniqueQueued());
        assertEquals(JobStatus.ACTIVE, argument.getValue().getStatus());
        assertEquals(InitialJobConfig.JOB_CONFIG_THREADS, argument.getValue().getThreads());
        assertEquals(StaticConfig.MINUTES_UNTIL_CLEANUP, argument.getValue().getMinutesUntilCleanUp());
        assertNull(argument.getValue().getSchedule());
    }

    @Test
    public void testCreateJob_with_Exception() throws Exception {

        Class<?> workerClass = TestWorker.class;
        Job job = new Job();
        job.setName(workerClass.getSimpleName());

        String exceptionMessage = "The job " + job.getName() + " couldn't be persisited by the persisitence " + jobPersistence.getPersistenceName();

        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage(exceptionMessage);

        classUnderTest.createJob(workerClass);

        ArgumentCaptor<JobErrorEvent> argument = ArgumentCaptor.forClass(JobErrorEvent.class);
        verify(jobErrorEvent).fireAsync(argument.capture());
        assertEquals(exceptionMessage, argument.getValue().getMessage());

        ArgumentCaptor<JobErrorEvent> argument2 = ArgumentCaptor.forClass(JobErrorEvent.class);
        verify(workhorseLogService).logException(argument2.capture());
        assertEquals(exceptionMessage, argument2.getValue().getMessage());
    }

    @Test
    public void testCreateJob_with_Exception2() throws Exception {

        Class<?> workerClass = TestWorker.class;
        Job job = new Job();
        job.setName(workerClass.getSimpleName());

        when(jobPersistence.persist(anyObject())).thenReturn(job);

        String exceptionMessage = "The job " + job.getName() + " couldn't be persisited by the persisitence " + jobPersistence.getPersistenceName();

        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage(exceptionMessage);

        classUnderTest.createJob(workerClass);
    }

    @Test
    public void testCreateJob_with_InitialJobConfig() throws Exception {

        Class<?> workerClass = TestWorkerWithInitialConfig.class;
        StaticConfig.MINUTES_UNTIL_CLEANUP = 40000l;
        // Job with Id to bypass the trigger of exception
        Job job = new Job();
        job.setId(1L);
        when(jobPersistence.persist(anyObject())).thenReturn(job);

        classUnderTest.createJob(workerClass);

        ArgumentCaptor<Job> argument = ArgumentCaptor.forClass(Job.class);
        verify(jobPersistence).persist(argument.capture());
        assertEquals(TEST_WORKER_WITH_INITIAL_JOB_CONFIG_NAME, argument.getValue().getName());
        assertEquals(THREADS, argument.getValue().getThreads());
        assertEquals(workerClass.getName(), argument.getValue().getWorkerClassName());
        assertEquals(InitialJobConfig.JOB_CONFIG_UNIQUE_IN_QUEUE, argument.getValue().isUniqueQueued());
        assertEquals(JobStatus.ACTIVE, argument.getValue().getStatus());
        assertEquals(StaticConfig.MINUTES_UNTIL_CLEANUP, argument.getValue().getMinutesUntilCleanUp());
        assertNull(argument.getValue().getSchedule());
    }

    @Test
    public void testCreateJob_with_InitialJobConfig_MINUTE_UNTIL_CLEANUP() throws Exception {

        Class<?> workerClass = TestWorkerWithInitialConfigMinuteUntilCleanup.class;

        // Job with Id to bypass the trigger of exception
        Job job = new Job();
        job.setId(1L);
        when(jobPersistence.persist(anyObject())).thenReturn(job);

        classUnderTest.createJob(workerClass);

        ArgumentCaptor<Job> argument = ArgumentCaptor.forClass(Job.class);
        verify(jobPersistence).persist(argument.capture());
        assertEquals(workerClass.getName(), argument.getValue().getWorkerClassName());
        assertEquals(InitialJobConfig.JOB_CONFIG_UNIQUE_IN_QUEUE, argument.getValue().isUniqueQueued());
        assertEquals(JobStatus.ACTIVE, argument.getValue().getStatus());
        assertEquals(MINUTES_UNTIL_CLEANUP, argument.getValue().getMinutesUntilCleanUp());
        assertNull(argument.getValue().getSchedule());
    }

    @Test
    public void testCreateJob_with_schedule() throws Exception {

        Class<?> workerClass = TestWorkerScheduler.class;

        // Job with Id to bypass the trigger of exception
        Job job = new Job();
        job.setId(1L);
        when(jobPersistence.persist(anyObject())).thenReturn(job);

        classUnderTest.createJob(workerClass);

        ArgumentCaptor<Job> argument = ArgumentCaptor.forClass(Job.class);
        verify(jobPersistence).persist(argument.capture());
        assertEquals(SCHEDULE, argument.getValue().getSchedule());

    }

    @Test
    public void testCreateJob_with_non_valid_schedule() throws Exception {

        Class<?> workerClass = TestWorkerNonValidScheduler.class;

        String exceptionMessage = "The job with worker's name " + workerClass.getName() + " could not be created due to invalid schedule: " + NON_VALID_SCHEDULE
                        + "\n" + anyString();
        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage(exceptionMessage);

        classUnderTest.createJob(workerClass);

    }

    @Test
    public void testCreateJob_with_tags() throws Exception {

        Class<?> workerClass = TestWorkerWithTags.class;

        // Job with Id to bypass the trigger of exception
        Job job = new Job();
        job.setId(1L);
        when(jobPersistence.persist(anyObject())).thenReturn(job);

        classUnderTest.createJob(workerClass);

        List<String> tagsList = new ArrayList<>(Arrays.asList(TAGS.split(",")));

        ArgumentCaptor<Job> argument = ArgumentCaptor.forClass(Job.class);
        verify(jobPersistence).persist(argument.capture());

        assertEquals(tagsList.size(), argument.getValue().getTags().size());
        assertEquals(tagsList.get(0), argument.getValue().getTags().get(0));
        assertEquals(tagsList.get(1), argument.getValue().getTags().get(1));
        assertEquals(tagsList.get(2), argument.getValue().getTags().get(2));

    }

    @Test
    public void testCreateJob_with_MaxPerminute() throws Exception {

        Class<?> workerClass = TestWorkerWithMaxPerMinute.class;

        Job job = new Job();
        job.setId(1L);
        when(jobPersistence.persist(anyObject())).thenReturn(job);

        classUnderTest.createJob(workerClass);

        ArgumentCaptor<Job> argument = ArgumentCaptor.forClass(Job.class);
        verify(jobPersistence).persist(argument.capture());

        assertEquals(MAX_PER_MINUTE, argument.getValue().getMaxPerMinute().intValue());

    }

    @Test
    public void testHandleFailedExecution_with_unexisting_execution() throws Exception {

        Job job = new Job();
        job.setId(1L);
        job.setName("TestJob");
        Execution execution = new Execution();
        execution.setId(1L);
        Exception exception = new Exception();
        Long duration = 50L;

        when(executionPersistence.getById(job.getId(), execution.getId())).thenReturn(null);

        Execution result = classUnderTest.handleFailedExecution(job, execution.getId(), exception, duration, null);

        String message = "The execution with ID: " + execution.getId() + " of job: " + job.getName() + " with JobID: " + job.getId()
                        + " could not be found in the persistence.";
        verify(workhorseLogService).logMessage(message, job.getId(), false);
        assertNull(result);
    }

    @Test
    public void testHandleFailedExecution_with_created_retry_execution() throws Exception {

        StaticConfig.TIME_ZONE = "UTC";

        Job job = new Job();
        job.setId(1L);
        job.setFailRetries(2);
        Execution failedExecution = new Execution();
        failedExecution.setId(1L);
        failedExecution.setFailRetry(1);
        Exception exception = new Exception();
        Long duration = 50L;
        Worker worker = new TestWorkerWithMaxPerMinute();

        Execution retryExecution = new Execution();
        retryExecution.setJobId(failedExecution.getJobId());
        retryExecution.setFailRetry(failedExecution.getFailRetry() + 1);

        Execution persistedExecution = new Execution();
        persistedExecution.setId(2L);
        persistedExecution.setJobId(retryExecution.getJobId());
        persistedExecution.setFailRetry(retryExecution.getFailRetry() + 1);

        when(executionPersistence.getById(job.getId(), failedExecution.getId())).thenReturn(failedExecution);

        // anyObject() is used because the parameter to pass contains a timestamp.
        when(executionPersistence.persist(anyObject())).thenReturn(persistedExecution);

        Execution result = classUnderTest.handleFailedExecution(job, failedExecution.getId(), exception, duration, worker);

        verify(executionPersistence).log(job.getId(), failedExecution.getId(), WorkhorseUtil.getMessagesFromException(exception),
                        WorkhorseUtil.stacktraceToString(exception));

        ArgumentCaptor<Execution> argument = ArgumentCaptor.forClass(Execution.class);
        verify(executionPersistence).update(argument.capture());
        assertEquals(ExecutionStatus.FAILED, argument.getValue().getStatus());
        assertEquals(duration, argument.getValue().getDuration());

        assertEquals(persistedExecution, result);
    }

    @Test
    public void testHandleFailedExecution_without_created_retry_execution() throws Exception {

        StaticConfig.TIME_ZONE = "UTC";

        Job job = new Job();
        job.setId(1L);
        job.setFailRetries(2);
        Execution failedExecution = new Execution();
        failedExecution.setId(1L);
        failedExecution.setFailRetry(3);
        Exception exception = new Exception();
        Long duration = 50L;
        Worker worker = mock(TestWorkerWithMaxPerMinute.class);

        when(executionPersistence.getById(job.getId(), failedExecution.getId())).thenReturn(failedExecution);

        Execution result = classUnderTest.handleFailedExecution(job, failedExecution.getId(), exception, duration, worker);

        verify(worker).onFailed(failedExecution.getId());

        ArgumentCaptor<Execution> argument = ArgumentCaptor.forClass(Execution.class);
        verify(executionPersistence).update(argument.capture());
        assertEquals(ExecutionStatus.FAILED, argument.getValue().getStatus());
        assertEquals(duration, argument.getValue().getDuration());

        assertNull(result);
    }

    @Test
    public void testHandleFailedExecution_with_abort_chain() throws Exception {

        StaticConfig.TIME_ZONE = "UTC";

        Job job = new Job();
        job.setId(1L);
        job.setFailRetries(2);
        Execution failedExecution = new Execution();
        failedExecution.setId(1L);
        failedExecution.setChainId(1L);
        failedExecution.setFailRetry(3);
        Exception exception = new Exception();
        Long duration = 50L;
        Worker worker = mock(TestWorkerWithMaxPerMinute.class);

        when(executionPersistence.getById(job.getId(), failedExecution.getId())).thenReturn(failedExecution);

        Execution result = classUnderTest.handleFailedExecution(job, failedExecution.getId(), exception, duration, worker);

        verify(executionPersistence).abortChain(job.getId(), failedExecution.getChainId());

        verify(worker).onFailedChain(failedExecution.getChainId(), failedExecution.getId());

        ArgumentCaptor<Execution> argument = ArgumentCaptor.forClass(Execution.class);
        verify(executionPersistence).update(argument.capture());
        assertEquals(ExecutionStatus.FAILED, argument.getValue().getStatus());
        assertEquals(duration, argument.getValue().getDuration());

        assertNull(result);
    }

}
