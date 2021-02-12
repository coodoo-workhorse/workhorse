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
import io.coodoo.workhorse.persistence.memory.MemoryConfig;
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
        initializePersistence(new MemoryConfig());
    }

    /**
     * 
     * @param Configuration Configuration of a persistence.
     */
    @SuppressWarnings("unchecked")
    public <T extends WorkhorseConfig> void initializePersistence(T configuration) {

        if (configuration == null) {
            configuration = (T) new MemoryConfigBuilder().build();
            log.warn("The persistence configuration can not be null. The default persistence {} is used",
                    configuration);
        }

        initializeJobEngineConfigPersistence(configuration);
        initializeJobPersistence(configuration);
        initializeExecutionPersistence(configuration);
        initializeLogPersistence(configuration);

        if (jobPersistence == null || executionPersistence == null || configPersistence == null
                || logPersistence == null) {
            configuration = (T) new MemoryConfig();
            log.error("The given persistence configuration could not be loaded, the default persistence is used: {}",
                    configuration);
            initializePersistence(configuration);
        }

    }

    protected <T extends WorkhorseConfig> JobPersistence initializeJobPersistence(T persistenceConfiguration) {
        log.trace("Start of JobPersistence initialization");
        for (JobPersistence jobPersistenceInstance : jobPersistenceInstances) {
            if (jobPersistenceInstance != null && jobPersistenceInstance.getPersistenceName()
                    .equals(persistenceConfiguration.getPersistenceName())) {
                jobPersistence = jobPersistenceInstance;
                log.info("JobPersistence: {}", jobPersistence);

                log.trace("End of JobPersistence initialization");
                jobPersistence.connect();
                return jobPersistence;
            }
        }
        return null;
    }

    protected <T extends WorkhorseConfig> ExecutionPersistence initializeExecutionPersistence(
            T persistenceConfiguration) {
        log.trace("Start of ExecutionPersistence initialization");
        for (ExecutionPersistence executionPersistenceInstance : executionPersistenceInstances) {
            if (executionPersistenceInstance != null && executionPersistenceInstance.getPersistenceName()
                    .equals(persistenceConfiguration.getPersistenceName())) {
                executionPersistence = executionPersistenceInstance;
                log.info("ExecutionPersistence: {}", executionPersistence);
                log.trace("End of ExecutionPersistence initialization");
                executionPersistence.connect();
                return executionPersistence;
            }
        }
        return null;
    }

    protected <T extends WorkhorseConfig> ConfigPersistence initializeJobEngineConfigPersistence(
            T persistenceConfiguration) {
        log.trace("Start of ExecutionPersistence initialization");
        for (ConfigPersistence configPersistenceInstance : configPersistenceInstances) {
            if (configPersistenceInstance != null && configPersistenceInstance.getPersistenceName()
                    .equals(persistenceConfiguration.getPersistenceName())) {
                configPersistence = configPersistenceInstance;
                log.info("configPersistence: {}", configPersistence);
                log.trace("End of configPersistence initialization");
                configPersistence.connect(persistenceConfiguration);
                return configPersistence;
            }
        }
        return null;
    }

    protected <T extends WorkhorseConfig> LogPersistence initializeLogPersistence(T persistenceConfiguration) {
        log.trace("Start of LogPersistence initialization");
        for (LogPersistence logPersistenceInstance : logPersistenceInstances) {
            if (logPersistenceInstance != null && logPersistenceInstance.getPersistenceName()
                    .equals(persistenceConfiguration.getPersistenceName())) {
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
