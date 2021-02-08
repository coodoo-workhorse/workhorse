package io.coodoo.workhorse.persistence;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.coodoo.workhorse.core.boundary.Config;
import io.coodoo.workhorse.persistence.interfaces.ConfigPersistence;
import io.coodoo.workhorse.persistence.interfaces.ExecutionPersistence;
import io.coodoo.workhorse.persistence.interfaces.JobPersistence;
import io.coodoo.workhorse.persistence.interfaces.LogPersistence;

import io.coodoo.workhorse.persistence.interfaces.qualifier.ConfigQualifier;
import io.coodoo.workhorse.persistence.interfaces.qualifier.ExecutionQualifier;
import io.coodoo.workhorse.persistence.interfaces.qualifier.JobQualifier;
import io.coodoo.workhorse.persistence.interfaces.qualifier.LogQualifier;

@ApplicationScoped
public class PersistenceManager {

    private static final Logger log = LoggerFactory.getLogger(PersistenceManager.class);

    @Inject
    @Any
    Instance<JobPersistence> jobPersistenceInstances;

    @Inject
    @Any
    Instance<ExecutionPersistence> executionPersistenceInstances;

    @Inject
    @Any
    Instance<ConfigPersistence> configPersistenceInstances;

    @Inject
    @Any
    Instance<LogPersistence> logPersistenceInstances;

    @Produces
    @JobQualifier
    private JobPersistence jobPersistence;

    @Produces
    @ExecutionQualifier
    private ExecutionPersistence executionPersistence;

    @Produces
    @ConfigQualifier
    private ConfigPersistence configPersistence;

    @Produces
    @LogQualifier
    private LogPersistence logPersistence;

    public void initializeStorage() {
        initializePersistence(Config.PERSISTENCE_NAME, null);
    }

    public void initializePersistence(String persistenceName, Object persistenceConfiguration) {

        if (persistenceName == null) {
            persistenceName = Config.PERSISTENCE_NAME;
            log.warn("The PersistenceName can not be null. The default persistence {} will be use", persistenceName);
        }

        initializeJobEngineConfigPersistence(persistenceName, persistenceConfiguration);
        initializeJobPersistence(persistenceName);
        initializeExecutionPersistence(persistenceName);
        initializeLogPersistence(persistenceName);

        if (jobPersistence == null || executionPersistence == null || configPersistence == null
                || logPersistence == null) {
            throw new RuntimeException("The Persistence could not be load!!");
        }
    }

    public void destroyStorage() {
        jobPersistence = null;
        executionPersistence = null;
        configPersistence = null;
        logPersistence = null;

    }

    public JobPersistence initializeJobPersistence(String persistenceName) {
        log.trace("Start of JobPersistence initialization");
        for (JobPersistence jobPersistenceInstance : jobPersistenceInstances) {
            if (jobPersistenceInstance != null && jobPersistenceInstance.getPersistenceName().equals(persistenceName)) {
                jobPersistence = jobPersistenceInstance;
                log.info("JobPersistence: {}", jobPersistence);

                log.trace("End of JobPersistence initialization");
                jobPersistence.connect();
                return jobPersistence;
            }
        }
        return null;
    }

    public ExecutionPersistence initializeExecutionPersistence(String persistenceName) {
        log.trace("Start of ExecutionPersistence initialization");
        for (ExecutionPersistence executionPersistenceInstance : executionPersistenceInstances) {
            if (executionPersistenceInstance != null
                    && executionPersistenceInstance.getPersistenceName().equals(persistenceName)) {
                executionPersistence = executionPersistenceInstance;
                log.info("ExecutionPersistence: {}", executionPersistence);
                log.trace("End of ExecutionPersistence initialization");
                executionPersistence.connect();
                return executionPersistence;
            }
        }
        return null;
    }

    public ConfigPersistence initializeJobEngineConfigPersistence(String persistenceName,
            Object persistenceConfiguration) {
        log.trace("Start of ExecutionPersistence initialization");
        for (ConfigPersistence configPersistenceInstance : configPersistenceInstances) {
            if (configPersistenceInstance != null
                    && configPersistenceInstance.getPersistenceName().equals(persistenceName)) {
                configPersistence = configPersistenceInstance;
                log.info("configPersistence: {}", configPersistence);
                log.trace("End of configPersistence initialization");
                configPersistence.connect(persistenceConfiguration);
                return configPersistence;
            }
        }
        return null;
    }

    public LogPersistence initializeLogPersistence(String persistenceName) {
        log.trace("Start of LogPersistence initialization");
        for (LogPersistence logPersistenceInstance : logPersistenceInstances) {
            if (logPersistenceInstance != null && logPersistenceInstance.getPersistenceName().equals(persistenceName)) {
                logPersistence = logPersistenceInstance;
                log.info("LogPersistence: {}", logPersistence);
                log.trace("End of LogPersistence initialization");
                logPersistence.connect();
                return logPersistence;
            }
        }
        return null;

    }

}
