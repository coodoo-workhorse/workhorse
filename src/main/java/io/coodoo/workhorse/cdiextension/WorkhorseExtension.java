package io.coodoo.workhorse.cdiextension;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;

import io.coodoo.workhorse.config.boundary.JobEngineConfigService;
import io.coodoo.workhorse.config.control.JobEngineConfigControl;
import io.coodoo.workhorse.config.entity.JobEngineConfig;
import io.coodoo.workhorse.jobengine.boundary.JobEngineService;
import io.coodoo.workhorse.jobengine.boundary.job.JobExecutionCleanupJobWorker;
import io.coodoo.workhorse.jobengine.control.JobEngine;
import io.coodoo.workhorse.jobengine.control.JobEngineController;
import io.coodoo.workhorse.jobengine.control.JobExecutionBuffer;
import io.coodoo.workhorse.jobengine.control.JobScheduler;
import io.coodoo.workhorse.jobengine.control.JobThread;
import io.coodoo.workhorse.log.boundary.JobEngineLogService;
import io.coodoo.workhorse.log.control.JobEngineLogControl;
import io.coodoo.workhorse.persistence.PersistenceManager;
import io.coodoo.workhorse.persistence.interfaces.JobEngineConfigPersistence;
import io.coodoo.workhorse.persistence.interfaces.JobExecutionPersistence;
import io.coodoo.workhorse.persistence.interfaces.JobPersistence;
import io.coodoo.workhorse.persistence.memory.Memory;
import io.coodoo.workhorse.persistence.memory.MemoryJobDAO;
import io.coodoo.workhorse.persistence.memory.MemoryJobEngineConfig;
import io.coodoo.workhorse.persistence.memory.MemoryJobEngineLog;
import io.coodoo.workhorse.persistence.memory.MemoryJobExecutionDAO;

public class WorkhorseExtension {

    public void registerJobEngineService(@Observes BeforeBeanDiscovery bbdEvent) {
        bbdEvent.addAnnotatedType(JobEngineService.class, JobEngineService.class.getName());
        bbdEvent.addAnnotatedType(JobEngine.class, JobEngine.class.getName());
        bbdEvent.addAnnotatedType(JobEngineConfig.class, JobEngineConfig.class.getName());
        bbdEvent.addAnnotatedType(JobEngineConfigControl.class, JobEngineConfigControl.class.getName());
        bbdEvent.addAnnotatedType(JobEngineConfigService.class, JobEngineConfigService.class.getName());
        bbdEvent.addAnnotatedType(JobExecutionBuffer.class, JobExecutionBuffer.class.getName());
        bbdEvent.addAnnotatedType(JobExecutionBuffer.class, JobExecutionBuffer.class.getName());
        bbdEvent.addAnnotatedType(JobExecutionPersistence.class, JobExecutionPersistence.class.getName());
        bbdEvent.addAnnotatedType(JobEngineController.class, JobEngineController.class.getName());
        bbdEvent.addAnnotatedType(JobEngineLogControl.class, JobEngineLogControl.class.getName());
        bbdEvent.addAnnotatedType(JobEngineConfigPersistence.class, JobEngineConfigPersistence.class.getName());
        bbdEvent.addAnnotatedType(JobExecutionPersistence.class, JobExecutionPersistence.class.getName());
        bbdEvent.addAnnotatedType(JobPersistence.class, JobPersistence.class.getName());
        bbdEvent.addAnnotatedType(JobScheduler.class, JobScheduler.class.getName());
        bbdEvent.addAnnotatedType(JobEngineLogService.class, JobEngineLogService.class.getName());
        bbdEvent.addAnnotatedType(PersistenceManager.class, PersistenceManager.class.getName());
        bbdEvent.addAnnotatedType(JobExecutionCleanupJobWorker.class, JobExecutionCleanupJobWorker.class.getName());
        bbdEvent.addAnnotatedType(JobThread.class, JobThread.class.getName());
        bbdEvent.addAnnotatedType(Memory.class, Memory.class.getName());
        bbdEvent.addAnnotatedType(MemoryJobDAO.class, MemoryJobDAO.class.getName());
        bbdEvent.addAnnotatedType(MemoryJobExecutionDAO.class, MemoryJobExecutionDAO.class.getName());
        bbdEvent.addAnnotatedType(MemoryJobEngineLog.class, MemoryJobEngineLog.class.getName());
        bbdEvent.addAnnotatedType(MemoryJobEngineConfig.class, MemoryJobEngineConfig.class.getName());

    }
}
