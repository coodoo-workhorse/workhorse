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
public class WorkhorsePersistence {

    private static final Logger log = LoggerFactory.getLogger(WorkhorsePersistence.class);

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
    public void initialize(WorkhorseConfig workhorseConfig) {

        if (workhorseConfig == null) {
            workhorseConfig = new MemoryConfigBuilder().build();
            log.warn("The WorkhorseConfig can not be null. The default persistence {} is used", workhorseConfig);
        }

        log.trace("Workhorse using {} {}", workhorseConfig.getPersistenceName(), workhorseConfig.getPersistenceVersion());

        initializeConfigPersistence(workhorseConfig);
        initializeJobPersistence(workhorseConfig);
        initializeExecutionPersistence(workhorseConfig);
        initializeLogPersistence(workhorseConfig);

        if (!isInitialized()) {
            workhorseConfig = new MemoryConfigBuilder().build();
            log.error("The given WorkhorseConfig could not be loaded, the default persistence is used: {}", workhorseConfig);
            initialize(workhorseConfig);
        }
    }

    protected ConfigPersistence initializeConfigPersistence(WorkhorseConfig workhorseConfig) {
        log.trace("ExecutionPersistence initialization");
        for (ConfigPersistence configPersistenceInstance : configPersistenceInstances) {
            if (configPersistenceInstance != null && configPersistenceInstance.getPersistenceName().equals(workhorseConfig.getPersistenceName())) {
                configPersistence = configPersistenceInstance;

                logFoundPersistence(ConfigPersistence.class, configPersistence.getClass());
                configPersistence.initialize(workhorseConfig);
                return configPersistence;
            }
        }
        return null;
    }

    protected JobPersistence initializeJobPersistence(WorkhorseConfig workhorseConfig) {
        log.trace("JobPersistence initialization");
        for (JobPersistence jobPersistenceInstance : jobPersistenceInstances) {
            if (jobPersistenceInstance != null && jobPersistenceInstance.getPersistenceName().equals(workhorseConfig.getPersistenceName())) {
                jobPersistence = jobPersistenceInstance;
                logFoundPersistence(JobPersistence.class, jobPersistence.getClass());
                return jobPersistence;
            }
        }
        return null;
    }

    protected ExecutionPersistence initializeExecutionPersistence(WorkhorseConfig workhorseConfig) {
        log.trace("ExecutionPersistence initialization");
        for (ExecutionPersistence executionPersistenceInstance : executionPersistenceInstances) {
            if (executionPersistenceInstance != null && executionPersistenceInstance.getPersistenceName().equals(workhorseConfig.getPersistenceName())) {
                executionPersistence = executionPersistenceInstance;
                logFoundPersistence(ExecutionPersistence.class, executionPersistence.getClass());
                return executionPersistence;
            }
        }
        return null;
    }

    protected LogPersistence initializeLogPersistence(WorkhorseConfig workhorseConfig) {
        log.trace("LogPersistence initialization");
        for (LogPersistence logPersistenceInstance : logPersistenceInstances) {
            if (logPersistenceInstance != null && logPersistenceInstance.getPersistenceName().equals(workhorseConfig.getPersistenceName())) {
                logPersistence = logPersistenceInstance;
                logFoundPersistence(LogPersistence.class, logPersistence.getClass());
                return logPersistence;
            }
        }
        return null;
    }

    private void logFoundPersistence(Class<?> persistenceInterface, Class<?> persistenceImplementation) {
        String intName = persistenceInterface.getSimpleName();
        String implName = persistenceImplementation.getName();
        log.trace("{} ⮕ {}", intName, implName);
    }

}
