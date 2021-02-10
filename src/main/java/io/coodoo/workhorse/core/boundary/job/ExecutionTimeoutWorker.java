package io.coodoo.workhorse.core.boundary.job;

import javax.enterprise.context.ApplicationScoped;

import io.coodoo.workhorse.core.boundary.Worker;
import io.coodoo.workhorse.core.boundary.annotation.InitialJobConfig;

/**
 * @author coodoo GmbH (coodoo.io)
 */
@ApplicationScoped
@InitialJobConfig(name = "Cure expired Execution", schedule = "5 * * * * *", failRetries = 1, description = "Fix all expired Executions")
public class ExecutionTimeoutWorker extends Worker {

    @Override
    public void doWork() throws Exception {
        workhorseController.huntExpiredExecutions();
    }

}
