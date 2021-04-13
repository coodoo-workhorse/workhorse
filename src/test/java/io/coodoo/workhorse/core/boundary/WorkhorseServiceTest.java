package io.coodoo.workhorse.core.boundary;

import static org.junit.Assert.assertEquals;
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
import io.coodoo.workhorse.core.entity.JobExecutionCount;
import io.coodoo.workhorse.core.entity.WorkhorseConfig;
import io.coodoo.workhorse.persistence.PersistenceManager;
import io.coodoo.workhorse.persistence.memory.MemoryConfigBuilder;

@RunWith(MockitoJUnitRunner.class)
public class WorkhorseServiceTest {

    @InjectMocks
    WorkhorseService classUnderTest;

    @Mock
    WorkhorseController workhorseController;

    @Mock
    WorkhorseConfigController workhorseConfigController;

    @Mock
    PersistenceManager persistenceManager;

    @Mock
    ExecutionBuffer executionBuffer;

    @Mock
    Workhorse workhorse;

    @Mock
    JobScheduler jobScheduler;

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

    @Test
    public void testStart_with_parameter_without_initialized_persisitence() throws Exception {

        WorkhorseConfig config = new MemoryConfigBuilder().build();

        when(persistenceManager.isInitialized()).thenReturn(false);

        classUnderTest.start(config);

        assertEquals(config, classUnderTest.currentWorkhorseConfig);

        verify(persistenceManager).initializePersistence(config);
        verify(workhorseConfigController).initializeStaticConfig(config);

        verify(workhorseController).loadWorkers();
        verify(executionBuffer).initialize();
        verify(workhorse).start();
        verify(jobScheduler).startScheduler();
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
    }

    @Test
    public void testStart_without_parameter() throws Exception {

        WorkhorseConfig config = new MemoryConfigBuilder().build();

        classUnderTest.start();

        assertEquals(config.toString(), classUnderTest.currentWorkhorseConfig.toString());

        verify(persistenceManager).initializePersistence(classUnderTest.currentWorkhorseConfig);
        verify(workhorseConfigController).initializeStaticConfig(classUnderTest.currentWorkhorseConfig);

        verify(workhorseController).loadWorkers();
        verify(executionBuffer).initialize();
        verify(workhorse).start();
        verify(jobScheduler).startScheduler();
    }

    @Test
    public void testStart_without_parameter_and_with_currentWorkhorseConfig() throws Exception {

        WorkhorseConfig config = new MemoryConfigBuilder().build();

        classUnderTest.currentWorkhorseConfig = config;
        classUnderTest.start();

        verify(persistenceManager).initializePersistence(config);
        verify(workhorseConfigController).initializeStaticConfig(config);

        verify(workhorseController).loadWorkers();
        verify(executionBuffer).initialize();
        verify(workhorse).start();
        verify(jobScheduler).startScheduler();
    }

}
