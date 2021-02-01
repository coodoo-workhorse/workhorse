package io.coodoo.workhorse.core.control;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;

import io.coodoo.workhorse.config.boundary.WorkhorseConfigService;
import io.coodoo.workhorse.config.control.WorkhorseConfigControl;
import io.coodoo.workhorse.config.entity.WorkhorseConfig;
import io.coodoo.workhorse.core.boundary.WorkhorseLogService;
import io.coodoo.workhorse.core.boundary.WorkhorseService;
import io.coodoo.workhorse.core.boundary.job.ExecutionCleanupWorker;
import io.coodoo.workhorse.persistence.PersistenceManager;
import io.coodoo.workhorse.persistence.interfaces.ConfigPersistence;
import io.coodoo.workhorse.persistence.interfaces.ExecutionPersistence;
import io.coodoo.workhorse.persistence.interfaces.JobPersistence;
import io.coodoo.workhorse.persistence.memory.MemoryConfigPersistence;
import io.coodoo.workhorse.persistence.memory.MemoryExecutionPersistence;
import io.coodoo.workhorse.persistence.memory.MemoryJobPersistence;
import io.coodoo.workhorse.persistence.memory.MemoryLogPersistence;
import io.coodoo.workhorse.persistence.memory.MemoryPersistence;

/**
 * @author coodoo GmbH (coodoo.io)
 */
public class WorkhorseCDIExtension {

    public void register(@Observes BeforeBeanDiscovery bbdEvent) {
        bbdEvent.addAnnotatedType(WorkhorseService.class, WorkhorseService.class.getName());
        bbdEvent.addAnnotatedType(Workhorse.class, Workhorse.class.getName());
        bbdEvent.addAnnotatedType(WorkhorseConfig.class, WorkhorseConfig.class.getName());
        bbdEvent.addAnnotatedType(WorkhorseConfigControl.class, WorkhorseConfigControl.class.getName());
        bbdEvent.addAnnotatedType(WorkhorseConfigService.class, WorkhorseConfigService.class.getName());
        bbdEvent.addAnnotatedType(ExecutionBuffer.class, ExecutionBuffer.class.getName());
        bbdEvent.addAnnotatedType(ExecutionBuffer.class, ExecutionBuffer.class.getName());
        bbdEvent.addAnnotatedType(ExecutionPersistence.class, ExecutionPersistence.class.getName());
        bbdEvent.addAnnotatedType(WorkhorseController.class, WorkhorseController.class.getName());
        bbdEvent.addAnnotatedType(ConfigPersistence.class, ConfigPersistence.class.getName());
        bbdEvent.addAnnotatedType(ExecutionPersistence.class, ExecutionPersistence.class.getName());
        bbdEvent.addAnnotatedType(JobPersistence.class, JobPersistence.class.getName());
        bbdEvent.addAnnotatedType(JobScheduler.class, JobScheduler.class.getName());
        bbdEvent.addAnnotatedType(WorkhorseLogService.class, WorkhorseLogService.class.getName());
        bbdEvent.addAnnotatedType(PersistenceManager.class, PersistenceManager.class.getName());
        bbdEvent.addAnnotatedType(ExecutionCleanupWorker.class, ExecutionCleanupWorker.class.getName());
        bbdEvent.addAnnotatedType(JobThread.class, JobThread.class.getName());
        bbdEvent.addAnnotatedType(MemoryPersistence.class, MemoryPersistence.class.getName());
        bbdEvent.addAnnotatedType(MemoryJobPersistence.class, MemoryJobPersistence.class.getName());
        bbdEvent.addAnnotatedType(MemoryExecutionPersistence.class, MemoryExecutionPersistence.class.getName());
        bbdEvent.addAnnotatedType(MemoryLogPersistence.class, MemoryLogPersistence.class.getName());
        bbdEvent.addAnnotatedType(MemoryConfigPersistence.class, MemoryConfigPersistence.class.getName());
    }
}
