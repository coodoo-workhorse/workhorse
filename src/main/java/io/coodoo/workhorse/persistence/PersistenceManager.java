package io.coodoo.workhorse.persistence;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.coodoo.workhorse.config.entity.JobEngineConfig;
import io.coodoo.workhorse.persistence.interfaces.JobEngineConfigPersistence;
import io.coodoo.workhorse.persistence.interfaces.JobEngineLogPersistence;
import io.coodoo.workhorse.persistence.interfaces.JobExecutionPersistence;
import io.coodoo.workhorse.persistence.interfaces.JobPersistence;
import io.coodoo.workhorse.persistence.interfaces.PersistenceTyp;
import io.coodoo.workhorse.persistence.interfaces.qualifiers.JobDAO;
import io.coodoo.workhorse.persistence.interfaces.qualifiers.JobEngineConfigDAO;
import io.coodoo.workhorse.persistence.interfaces.qualifiers.JobEngineLogDAO;
import io.coodoo.workhorse.persistence.interfaces.qualifiers.JobExecutionDAO;

@ApplicationScoped
public class PersistenceManager {

    private static final Logger log = Logger.getLogger(PersistenceManager.class);

    @Inject
    @Any
    Instance<JobPersistence> jobPersistenceInstances;

    @Inject
    @Any
    Instance<JobExecutionPersistence> jobExecutionPersistenceInstances;

    @Inject
    @Any
    Instance<JobEngineConfigPersistence> jobEngineConfigPersistenceInstances;

    @Inject
    @Any
    Instance<JobEngineLogPersistence> jobEngineLogPersistenceInstances;

    @Inject
    JobEngineConfig jobEngineConfig;

    @Produces
    @JobDAO
    private JobPersistence jobPersistence;

    @Produces
    @JobExecutionDAO
    private JobExecutionPersistence jobExecutionPersistence;

    @Produces
    @JobEngineConfigDAO
    private JobEngineConfigPersistence jobEngineConfigPersistence;

    @Produces
    @JobEngineLogDAO
    private JobEngineLogPersistence jobEngineLogPersistence;

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

        if (jobPersistence == null || jobExecutionPersistence == null || jobEngineConfigPersistence == null || jobEngineLogPersistence == null) {
            throw new RuntimeException("The Persistence could not be load!!");
        }
    }

    public void destroyStorage() {
        jobPersistence = null;
        jobExecutionPersistence = null;
        jobEngineConfigPersistence = null;
        jobEngineLogPersistence = null;

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

    public JobExecutionPersistence initializeJobExecutionPersistence(PersistenceTyp persistenceTyp) {
        log.info("Start of JobExecutionPersistence initialization");
        for (JobExecutionPersistence persistenceJobExecution : jobExecutionPersistenceInstances) {
            if (persistenceJobExecution != null && persistenceJobExecution.getPersistenceTyp().equals(persistenceTyp)) {
                jobExecutionPersistence = persistenceJobExecution;
                log.info("JobExecutionPersistence: " + jobExecutionPersistence);
                log.info("End of JobExecutionPersistence initialization");
                jobExecutionPersistence.connect();
                return jobExecutionPersistence;
            }
        }
        return null;
    }

    public JobEngineConfigPersistence initializeJobEngineConfigPersistence(PersistenceTyp persistenceTyp, Object persistenceConfiguration) {
        log.info("Start of JobExecutionPersistence initialization");
        for (JobEngineConfigPersistence persistenceJobEngineConfig : jobEngineConfigPersistenceInstances) {
            if (persistenceJobEngineConfig != null && persistenceJobEngineConfig.getPersistenceTyp().equals(persistenceTyp)) {
                jobEngineConfigPersistence = persistenceJobEngineConfig;
                log.info("jobEngineConfigPersistence: " + jobEngineConfigPersistence);
                log.info("End of jobEngineConfigPersistence initialization");
                jobEngineConfigPersistence.connect(persistenceConfiguration);
                return jobEngineConfigPersistence;
            }
        }
        return null;
    }

    public JobEngineLogPersistence initializeJobEnigneLogPersistence(PersistenceTyp persistenceTyp) {
        log.info("Start of JobEngineLogPersistence initialization");
        for (JobEngineLogPersistence persistenceJobEngineLog : jobEngineLogPersistenceInstances) {
            if (persistenceJobEngineLog != null && persistenceJobEngineLog.getPersistenceTyp().equals(persistenceTyp)) {
                jobEngineLogPersistence = persistenceJobEngineLog;
                log.info("JobEngineLogPersistence: " + jobEngineLogPersistence);
                log.info("End of JobEngineLogPersistence initialization");
                jobEngineLogPersistence.connect();
                return jobEngineLogPersistence;
            }
        }
        return null;

    }

    // @Produces
    // @JobDAO
    // public JobPersistence getJobPersistence() {
    // return jobPersistence;
    // }

    // @Produces
    // @JobExecutionDAO
    // public JobExecutionPersistence getJobExecutionPersistence() {
    // return jobExecutionPersistence;
    // }

    // @Produces
    // @JobEngineConfigDAO
    // public JobEngineConfigPersistence getJobEngineConfigPersistence() {
    // return jobEngineConfigPersistence;
    // }

    // @Produces
    // @JobEngineLogDAO
    // public JobEngineLogPersistence getJobEngineLogPersistence() {
    // return jobEngineLogPersistence;
    // }

}
