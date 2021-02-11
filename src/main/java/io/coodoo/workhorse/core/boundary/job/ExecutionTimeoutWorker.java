package io.coodoo.workhorse.core.boundary.job;

import javax.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.coodoo.workhorse.core.boundary.Worker;
import io.coodoo.workhorse.core.boundary.annotation.InitialJobConfig;

/**
 * @author coodoo GmbH (coodoo.io)
 */
@ApplicationScoped
@InitialJobConfig(name = "Cure expired Execution", schedule = "5 * * * * *", failRetries = 1, description = "Fix all expired Executions")
public class ExecutionTimeoutWorker extends Worker {

    private final Logger logger = LoggerFactory.getLogger(ExecutionTimeoutWorker.class);

    @Override
    public void doWork() throws Exception {
        logInfo(logger, "Start of the hunt after zombies");
        workhorseController.huntExpiredExecutions();
    }

}
