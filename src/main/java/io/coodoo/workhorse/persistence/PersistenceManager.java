package io.coodoo.workhorse.persistence;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.coodoo.workhorse.core.entity.WorkhorseConfig;
import io.coodoo.workhorse.persistence.interfaces.ConfigPersistence;
import io.coodoo.workhorse.persistence.interfaces.ExecutionPersistence;
import io.coodoo.workhorse.persistence.interfaces.JobPersistence;
import io.coodoo.workhorse.persistence.interfaces.LogPersistence;
import io.coodoo.workhorse.persistence.interfaces.qualifier.ConfigQualifier;
import io.coodoo.workhorse.persistence.interfaces.qualifier.ExecutionQualifier;
import io.coodoo.workhorse.persistence.interfaces.qualifier.JobQualifier;
import io.coodoo.workhorse.persistence.interfaces.qualifier.LogQualifier;
import io.coodoo.workhorse.persistence.memory.MemoryConfigBuilder;

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
        initializePersistence(new MemoryConfigBuilder().build());
    }

    /**
     * Check if all persistences are initialized
     */
    public boolean isInitialized() {
        return jobPersistence != null && executionPersistence != null && configPersistence != null && logPersistence != null;
    }

    /**
     * Initialize the classes that enable the access to the persistence
     * 
     * @param Configuration Configuration of the persistence.
     */
    public void initializePersistence(WorkhorseConfig configuration) {

        if (configuration == null) {
            configuration = new MemoryConfigBuilder().build();
            log.warn("The persistence configuration can not be null. The default persistence {} is used", configuration);
        }

        log.info("Initialize Persistence: {} {}", configuration.getPersistenceName(), configuration.getPersistenceVersion());

        initializeConfigPersistence(configuration);
        initializeJobPersistence(configuration);
        initializeExecutionPersistence(configuration);
        initializeLogPersistence(configuration);

        if (!isInitialized()) {
            configuration = new MemoryConfigBuilder().build();
            log.error("The given persistence configuration could not be loaded, the default persistence is used: {}", configuration);
            initializePersistence(configuration);
        }
    }

    protected JobPersistence initializeJobPersistence(WorkhorseConfig persistenceConfiguration) {
        log.trace("JobPersistence initialization");
        for (JobPersistence jobPersistenceInstance : jobPersistenceInstances) {
            if (jobPersistenceInstance != null && jobPersistenceInstance.getPersistenceName().equals(persistenceConfiguration.getPersistenceName())) {
                jobPersistence = jobPersistenceInstance;
                logFoundPersistence(JobPersistence.class, jobPersistence.getClass());
                jobPersistence.connect();
                return jobPersistence;
            }
        }
        return null;
    }

    protected ExecutionPersistence initializeExecutionPersistence(WorkhorseConfig persistenceConfiguration) {
        log.trace("ExecutionPersistence initialization");
        for (ExecutionPersistence executionPersistenceInstance : executionPersistenceInstances) {
            if (executionPersistenceInstance != null
                            && executionPersistenceInstance.getPersistenceName().equals(persistenceConfiguration.getPersistenceName())) {
                executionPersistence = executionPersistenceInstance;
                logFoundPersistence(ExecutionPersistence.class, executionPersistence.getClass());
                executionPersistence.connect();
                return executionPersistence;
            }
        }
        return null;
    }

    protected ConfigPersistence initializeConfigPersistence(WorkhorseConfig persistenceConfiguration) {
        log.trace("ExecutionPersistence initialization");
        for (ConfigPersistence configPersistenceInstance : configPersistenceInstances) {
            if (configPersistenceInstance != null && configPersistenceInstance.getPersistenceName().equals(persistenceConfiguration.getPersistenceName())) {
                configPersistence = configPersistenceInstance;

                logFoundPersistence(ConfigPersistence.class, configPersistence.getClass());
                configPersistence.connect(persistenceConfiguration);
                return configPersistence;
            }
        }
        return null;
    }

    protected LogPersistence initializeLogPersistence(WorkhorseConfig persistenceConfiguration) {
        log.trace("LogPersistence initialization");
        for (LogPersistence logPersistenceInstance : logPersistenceInstances) {
            if (logPersistenceInstance != null && logPersistenceInstance.getPersistenceName().equals(persistenceConfiguration.getPersistenceName())) {
                logPersistence = logPersistenceInstance;
                logFoundPersistence(LogPersistence.class, logPersistence.getClass());

                logPersistence.connect();
                return logPersistence;
            }
        }
        return null;

    }

    private void logFoundPersistence(Class<?> persistenceInterface, Class<?> persistenceImplementation) {
        String intName = persistenceInterface.getSimpleName();
        String implName = persistenceImplementation.getName();
        log.info("{} --> {}", intName, implName);
    }
}
