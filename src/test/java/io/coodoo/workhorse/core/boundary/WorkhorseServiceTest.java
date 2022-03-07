package io.coodoo.workhorse.core.boundary;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
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

import io.coodoo.workhorse.core.control.ExecutionBuffer;
import io.coodoo.workhorse.core.control.JobScheduler;
import io.coodoo.workhorse.core.control.StaticConfig;
import io.coodoo.workhorse.core.control.Workhorse;
import io.coodoo.workhorse.core.control.WorkhorseConfigController;
import io.coodoo.workhorse.core.control.WorkhorseController;
import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.core.entity.ExecutionStatusCounts;
import io.coodoo.workhorse.core.entity.JobStatus;
import io.coodoo.workhorse.core.entity.WorkhorseConfig;
import io.coodoo.workhorse.persistence.WorkhorsePersistence;
import io.coodoo.workhorse.persistence.memory.MemoryConfigBuilder;

@RunWith(MockitoJUnitRunner.class)
public class WorkhorseServiceTest {

    @InjectMocks
    WorkhorseService classUnderTest;

    @Mock
    Workhorse workhorse;

    @Mock
    ExecutionBuffer executionBuffer;

    @Mock
    WorkhorsePersistence persistenceManager;

    @Mock
    WorkhorseLogService workhorseLogService;

    @Mock
    WorkhorseController workhorseController;

    @Mock
    WorkhorseConfigController workhorseConfigController;

    @Mock
    JobScheduler jobScheduler;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
        public void testGetExecutionStatusCounts_with_FROM_in_the_future() throws Exception {
    
            Long jobId = 1L;
            StaticConfig.TIME_ZONE = "UTC";
            LocalDateTime from = LocalDateTime.now(ZoneId.of(StaticConfig.TIME_ZONE)).plusHours(5);
            LocalDateTime to = LocalDateTime.now(ZoneId.of(StaticConfig.TIME_ZONE));
    
            ExecutionStatusCounts result = new ExecutionStatusCounts();
    
            when(workhorseController.getExecutionStatusCounts(jobId, from, to)).thenReturn(result);
    
            classUnderTest.getExecutionStatusCounts(jobId, from, to);
    
            verify(workhorseController).getExecutionStatusCounts(jobId, to.minusHours(24), to);
    
        }

    @Test
        public void testGetExecutionStatusCounts_with_FROM_null() throws Exception {
    
            Long jobId = 1L;
            StaticConfig.TIME_ZONE = "UTC";
            LocalDateTime from = null;
            LocalDateTime to = LocalDateTime.now(ZoneId.of(StaticConfig.TIME_ZONE));
    
            ExecutionStatusCounts result = new ExecutionStatusCounts();
    
            when(workhorseController.getExecutionStatusCounts(jobId, from, to)).thenReturn(result);
    
            classUnderTest.getExecutionStatusCounts(jobId, from, to);
    
            verify(workhorseController).getExecutionStatusCounts(jobId, to.minusHours(24), to);
    
        }

    @Test
        public void testGetExecutionStatusCounts_with_TO_null() throws Exception {
    
            Long jobId = 1L;
            StaticConfig.TIME_ZONE = "UTC";
            LocalDateTime from = null;
            LocalDateTime to = LocalDateTime.now(ZoneId.of(StaticConfig.TIME_ZONE));
    
            ExecutionStatusCounts result = new ExecutionStatusCounts();
    
            when(workhorseController.getExecutionStatusCounts(jobId, from, to)).thenReturn(result);
    
            classUnderTest.getExecutionStatusCounts(jobId, from, to);
    
            verify(workhorseController).getExecutionStatusCounts(jobId, to.minusHours(24), to);
    
        }

    @Test
    public void testStart_with_parameter_without_initialized_persisitence() throws Exception {

        WorkhorseConfig config = new MemoryConfigBuilder().build();

        when(persistenceManager.isInitialized()).thenReturn(false);

        classUnderTest.start(config);

        assertEquals(config, classUnderTest.currentWorkhorseConfig);

        verify(persistenceManager).initialize(config);
        verify(workhorseConfigController).initializeStaticConfig(config);

        verify(workhorseController).loadWorkers();
        verify(executionBuffer).initialize();
        verify(workhorse).start();
        verify(jobScheduler).startScheduler();

        verify(workhorseLogService).logMessage(anyString(), anyLong(), anyBoolean());
    }

    @Test
    public void testStart_with_parameter_and_initialized_persisitence() throws Exception {

        WorkhorseConfig config = new MemoryConfigBuilder().build();

        when(persistenceManager.isInitialized()).thenReturn(true);

        classUnderTest.start(config);

        verify(workhorseController).loadWorkers();
        verify(executionBuffer).initialize();
        verify(workhorse).start();
        verify(jobScheduler).startScheduler();

        verify(workhorseLogService).logMessage(anyString(), anyLong(), anyBoolean());
    }

    @Test
    public void testStart_without_parameter() throws Exception {

        WorkhorseConfig config = new MemoryConfigBuilder().build();

        classUnderTest.start();

        assertEquals(config.toString(), classUnderTest.currentWorkhorseConfig.toString());

        verify(persistenceManager).initialize(classUnderTest.currentWorkhorseConfig);
        verify(workhorseConfigController).initializeStaticConfig(classUnderTest.currentWorkhorseConfig);

        verify(workhorseController).loadWorkers();
        verify(executionBuffer).initialize();
        verify(workhorse).start();
        verify(jobScheduler).startScheduler();

        verify(workhorseLogService).logMessage(anyString(), anyLong(), anyBoolean());
    }

    @Test
    public void testStart_without_parameter_and_with_currentWorkhorseConfig() throws Exception {

        WorkhorseConfig config = new MemoryConfigBuilder().build();

        classUnderTest.currentWorkhorseConfig = config;
        classUnderTest.start();

        verify(persistenceManager).initialize(config);
        verify(workhorseConfigController).initializeStaticConfig(config);

        verify(workhorseController).loadWorkers();
        verify(executionBuffer).initialize();
        verify(workhorse).start();
        verify(jobScheduler).startScheduler();

        verify(workhorseLogService).logMessage(anyString(), anyLong(), anyBoolean());
    }

    @Test
    public void testStop() throws Exception {

        classUnderTest.stop();

        verify(workhorse).stop();
        verify(jobScheduler).stopScheduler();
        verify(executionBuffer).clearMemoryQueue();

        verify(workhorseLogService).logMessage(anyString(), anyLong(), anyBoolean());
    }

    @Test
    public void testDeleteJob() throws Exception {

        Long jobId = 1L;
        Job job = new Job();
        job.setId(jobId);

        when(workhorseController.getJobById(jobId)).thenReturn(job);
        classUnderTest.deleteJob(jobId);

        verify(jobScheduler).stop(job);
        verify(executionBuffer).clearMemoryQueue(job);
        verify(workhorseController).deleteJob(jobId);
    }

    @Test
    public void testDeleteJob_with_non_found_job() throws Exception {

        Long jobId = 1L;
        Job job = new Job();
        job.setId(jobId);

        classUnderTest.deleteJob(jobId);

        verify(jobScheduler, times(0)).stop(anyObject());
        verify(executionBuffer, times(0)).clearMemoryQueue(anyObject());
        verify(workhorseController, times(0)).deleteJob(anyObject());
    }

    @Test
    public void testUpdateJob_with_jobstatus_ACTIVE() throws Exception {

        Long jobId = 1L;
        JobStatus status = JobStatus.ACTIVE;

        Job job = new Job();
        job.setId(1L);
        job.setStatus(JobStatus.ACTIVE);

        when(workhorseController.getJobById(jobId)).thenReturn(job);
        when(workhorseController.updateJob(jobId, null, null, null, null, status, 2, 1000, 1, 4, 30, false)).thenReturn(job);

        classUnderTest.updateJob(jobId, null, null, null, null, status, 2, 1000, 1, 4, 30, false);

        verify(executionBuffer).initialize(job);
        verify(workhorse).poll(job);
    }

    @Test
    public void testUpdateJob_with_jobstatus_and_Schedule_ACTIVE() throws Exception {

        Long jobId = 1L;
        JobStatus status = JobStatus.ACTIVE;
        String schedule = "24 * * * * *";

        Job job = new Job();
        job.setId(jobId);
        job.setStatus(status);
        job.setSchedule(schedule);

        when(workhorseController.getJobById(jobId)).thenReturn(job);
        when(workhorseController.updateJob(jobId, null, null, null, schedule, status, 2, 1000, 1, 4, 30, false)).thenReturn(job);

        classUnderTest.updateJob(jobId, null, null, null, schedule, status, 2, 1000, 1, 4, 30, false);

        verify(executionBuffer).initialize(job);
        verify(jobScheduler).start(job);
        verify(workhorse).poll(job);
    }

    @Test
    public void testUpdateJob_with_jobstatus_INACTIVE() throws Exception {

        Long jobId = 1L;
        JobStatus status = JobStatus.INACTIVE;

        Job job = new Job();
        job.setId(1L);
        job.setStatus(JobStatus.INACTIVE);

        when(workhorseController.getJobById(jobId)).thenReturn(job);
        when(workhorseController.updateJob(jobId, null, null, null, null, status, 2, 1000, 1, 4, 30, false)).thenReturn(job);

        classUnderTest.updateJob(jobId, null, null, null, null, status, 2, 1000, 1, 4, 30, false);

        verify(executionBuffer, times(0)).initialize(job);
        verify(jobScheduler, times(0)).start(job);
        verify(workhorse, times(0)).poll(job);
    }

}
