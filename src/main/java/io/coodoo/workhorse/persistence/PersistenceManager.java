package io.coodoo.workhorse.persistence;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.coodoo.workhorse.config.entity.WorkhorseConfig;
import io.coodoo.workhorse.persistence.interfaces.ConfigPersistence;
import io.coodoo.workhorse.persistence.interfaces.ExecutionPersistence;
import io.coodoo.workhorse.persistence.interfaces.JobPersistence;
import io.coodoo.workhorse.persistence.interfaces.LogPersistence;
import io.coodoo.workhorse.persistence.interfaces.PersistenceTyp;
import io.coodoo.workhorse.persistence.interfaces.qualifier.ConfigQualifier;
import io.coodoo.workhorse.persistence.interfaces.qualifier.ExecutionQualifier;
import io.coodoo.workhorse.persistence.interfaces.qualifier.JobQualifier;
import io.coodoo.workhorse.persistence.interfaces.qualifier.LogQualifier;

@ApplicationScoped
public class PersistenceManager {

    private static final Logger log = Logger.getLogger(PersistenceManager.class);

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

    @Inject
    WorkhorseConfig workhorseConfig;

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
        initializePersistence(workhorseConfig.getPersistenceTyp(), null);
    }

    public void initializePersistence(PersistenceTyp persistenceTyp, Object persistenceConfiguration) {

        if (persistenceTyp == null) {
            persistenceTyp = workhorseConfig.getPersistenceTyp();
            log.warn("persistenceTyp can not be null. The default persistence " + persistenceTyp + " will be use");
        }

        initializeJobEngineConfigPersistence(persistenceTyp, persistenceConfiguration);
        initializeJobPersistence(persistenceTyp);
        initializeExecutionPersistence(persistenceTyp);
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
        for (JobPersistence jobPersistenceInstance : jobPersistenceInstances) {
            if (jobPersistenceInstance != null && jobPersistenceInstance.getPersistenceTyp().equals(persistenceTyp)) {
                jobPersistence = jobPersistenceInstance;
                log.info("JobPersistence: " + jobPersistence);

                log.info("End of JobPersistence initialization");
                jobPersistence.connect();
                return jobPersistence;
            }
        }
        return null;
    }

    public ExecutionPersistence initializeExecutionPersistence(PersistenceTyp persistenceTyp) {
        log.info("Start of ExecutionPersistence initialization");
        for (ExecutionPersistence executionPersistenceInstance : executionPersistenceInstances) {
            if (executionPersistenceInstance != null && executionPersistenceInstance.getPersistenceTyp().equals(persistenceTyp)) {
                executionPersistence = executionPersistenceInstance;
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
        for (ConfigPersistence configPersistenceInstance : configPersistenceInstances) {
            if (configPersistenceInstance != null && configPersistenceInstance.getPersistenceTyp().equals(persistenceTyp)) {
                configPersistence = configPersistenceInstance;
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
        for (LogPersistence logPersistenceInstance : logPersistenceInstances) {
            if (logPersistenceInstance != null && logPersistenceInstance.getPersistenceTyp().equals(persistenceTyp)) {
                logPersistence = logPersistenceInstance;
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
