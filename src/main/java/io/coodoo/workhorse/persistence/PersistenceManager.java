package io.coodoo.workhorse.persistence;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.coodoo.workhorse.config.entity.WorkhorseConfig;
import io.coodoo.workhorse.persistence.interfaces.ConfigPersistence;
import io.coodoo.workhorse.persistence.interfaces.LogPersistence;
import io.coodoo.workhorse.persistence.interfaces.ExecutionPersistence;
import io.coodoo.workhorse.persistence.interfaces.JobPersistence;
import io.coodoo.workhorse.persistence.interfaces.PersistenceTyp;
import io.coodoo.workhorse.persistence.interfaces.qualifier.JobQualifier;
import io.coodoo.workhorse.persistence.interfaces.qualifier.ConfigQualifier;
import io.coodoo.workhorse.persistence.interfaces.qualifier.LogQualifier;
import io.coodoo.workhorse.persistence.interfaces.qualifier.ExecutionQualifier;

@ApplicationScoped
public class PersistenceManager {

    private static final Logger log = Logger.getLogger(PersistenceManager.class);

    @Inject
    @Any
    Instance<JobPersistence> jobPersistenceInstances;

    @Inject
    @Any
    Instance<ExecutionPersistence> jobExecutionPersistenceInstances;

    @Inject
    @Any
    Instance<ConfigPersistence> jobEngineConfigPersistenceInstances;

    @Inject
    @Any
    Instance<LogPersistence> jobEngineLogPersistenceInstances;

    @Inject
    WorkhorseConfig jobEngineConfig;

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
        initializePersistence(jobEngineConfig.getPersistenceTyp(), null);
    }

    public void initializePersistence(PersistenceTyp persistenceTyp, Object persistenceConfiguration) {

        if (persistenceTyp == null) {
            persistenceTyp = jobEngineConfig.getPersistenceTyp();
            log.warn("persistenceTyp can not be null. The default persistence " + persistenceTyp + " will be use");
        }

        initializeJobEngineConfigPersistence(persistenceTyp, persistenceConfiguration);

        initializeJobPersistence(persistenceTyp);

        initializeJobExecutionPersistence(persistenceTyp);

        initializeJobEnigneLogPersistence(persistenceTyp);

        if (jobPersistence == null || executionPersistence == null || configPersistence == null || logPersistence == null) {
            throw new RuntimeException("The Persistence could not be load!!");
        }
    }

    public void destroyStorage() {
        jobPersistence = null;
        executionPersistence = null;
        configPersistence = null;
        logPersistence = null;

    }

    public JobPersistence initializeJobPersistence(PersistenceTyp persistenceTyp) {
        log.info("Start of JobPersistence initialization");
        for (JobPersistence persistence : jobPersistenceInstances) {
            if (persistence != null && persistence.getPersistenceTyp().equals(persistenceTyp)) {
                jobPersistence = persistence;
                log.info("JobPersistence: " + jobPersistence);

                log.info("End of JobPersistence initialization");
                jobPersistence.connect();
                return jobPersistence;
            }
        }
        return null;
    }

    public ExecutionPersistence initializeJobExecutionPersistence(PersistenceTyp persistenceTyp) {
        log.info("Start of ExecutionPersistence initialization");
        for (ExecutionPersistence persistenceJobExecution : jobExecutionPersistenceInstances) {
            if (persistenceJobExecution != null && persistenceJobExecution.getPersistenceTyp().equals(persistenceTyp)) {
                executionPersistence = persistenceJobExecution;
                log.info("ExecutionPersistence: " + executionPersistence);
                log.info("End of ExecutionPersistence initialization");
                executionPersistence.connect();
                return executionPersistence;
            }
        }
        return null;
    }

    public ConfigPersistence initializeJobEngineConfigPersistence(PersistenceTyp persistenceTyp, Object persistenceConfiguration) {
        log.info("Start of ExecutionPersistence initialization");
        for (ConfigPersistence persistenceJobEngineConfig : jobEngineConfigPersistenceInstances) {
            if (persistenceJobEngineConfig != null && persistenceJobEngineConfig.getPersistenceTyp().equals(persistenceTyp)) {
                configPersistence = persistenceJobEngineConfig;
                log.info("configPersistence: " + configPersistence);
                log.info("End of configPersistence initialization");
                configPersistence.connect(persistenceConfiguration);
                return configPersistence;
            }
        }
        return null;
    }

    public LogPersistence initializeJobEnigneLogPersistence(PersistenceTyp persistenceTyp) {
        log.info("Start of LogPersistence initialization");
        for (LogPersistence persistenceJobEngineLog : jobEngineLogPersistenceInstances) {
            if (persistenceJobEngineLog != null && persistenceJobEngineLog.getPersistenceTyp().equals(persistenceTyp)) {
                logPersistence = persistenceJobEngineLog;
                log.info("LogPersistence: " + logPersistence);
                log.info("End of LogPersistence initialization");
                logPersistence.connect();
                return logPersistence;
            }
        }
        return null;

    }

    // @Produces
    // @JobQualifier
    // public JobPersistence getJobPersistence() {
    // return jobPersistence;
    // }

    // @Produces
    // @ExecutionQualifier
    // public ExecutionPersistence getJobExecutionPersistence() {
    // return executionPersistence;
    // }

    // @Produces
    // @ConfigQualifier
    // public ConfigPersistence getJobEngineConfigPersistence() {
    // return configPersistence;
    // }

    // @Produces
    // @LogQualifier
    // public LogPersistence getJobEngineLogPersistence() {
    // return logPersistence;
    // }

}
