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
@InitialJobConfig(name = "Cure expired executions", schedule = "5 * * * * *", failRetries = 1, description = "Fix all expired Executions")
public class HuntExpiredExecutionsWorker extends Worker {

    @Override
    public void doWork() throws Exception {
        workhorseController.huntExpiredExecutions();
    }

}
