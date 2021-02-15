package io.coodoo.workhorse.core.boundary.job;

import java.time.LocalDateTime;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.coodoo.workhorse.core.boundary.Worker;
import io.coodoo.workhorse.core.boundary.WorkhorseLogService;
import io.coodoo.workhorse.core.boundary.annotation.InitialJobConfig;
import io.coodoo.workhorse.core.control.StaticConfig;
import io.coodoo.workhorse.core.entity.Execution;
import io.coodoo.workhorse.core.entity.ExecutionStatus;
import io.coodoo.workhorse.persistence.interfaces.ExecutionPersistence;
import io.coodoo.workhorse.persistence.interfaces.qualifier.ExecutionQualifier;
import io.coodoo.workhorse.util.WorkhorseUtil;

/**
 * @author coodoo GmbH (coodoo.io)
 */
@ApplicationScoped
@InitialJobConfig(name = "Hunt expired executions", schedule = "5 * * * * *", failRetries = 1, description = "Hunt expired executions and cure them.")
public class HuntExpiredExecutionsWorker extends Worker {

    private final Logger log = LoggerFactory.getLogger(HuntExpiredExecutionsWorker.class);

    @Inject
    @ExecutionQualifier
    ExecutionPersistence executionPersistence;

    @Inject
    WorkhorseLogService workhorseLogService;

    @Override
    public void doWork() throws Exception {
        if (StaticConfig.EXECUTION_TIMEOUT <= 0) {
            return;
        }

        LocalDateTime time = WorkhorseUtil.timestamp().minusSeconds(StaticConfig.EXECUTION_TIMEOUT);
        List<Execution> expiredExecutions = executionPersistence.findExpiredExecutions(time);

        if (expiredExecutions.isEmpty()) {
            return;
        }

        for (Execution expiredExecution : expiredExecutions) {
            log.warn("Zombie found! {}", expiredExecution);

            ExecutionStatus cure = StaticConfig.EXECUTION_TIMEOUT_STATUS;
            String logMessage = "Zombie execution found (ID: " + expiredExecution.getId() + "): ";

            switch (cure) {
                case QUEUED:
                    Execution retryExecution = workhorseController.createRetryExecution(expiredExecution);
                    expiredExecution.setStatus(ExecutionStatus.FAILED);
                    log.info("Zombie killed and risen from the death! Now it is {}", retryExecution);
                    workhorseLogService.logMessage(logMessage + "Marked as failed and queued a clone",
                            expiredExecution.getJobId(), false);
                    break;
                case RUNNING:
                    log.warn("Zombie will still walk free with status {}", cure);
                    workhorseLogService.logMessage(logMessage + "No action is taken", expiredExecution.getJobId(),
                            false);
                    break;
                default:
                    expiredExecution.setStatus(cure);
                    log.info("Zombie is cured with status {}", cure);
                    workhorseLogService.logMessage(logMessage + "Put in status " + cure, expiredExecution.getJobId(),
                            false);
                    break;
            }

            executionPersistence.update(expiredExecution.getJobId(), expiredExecution.getId(), expiredExecution);

        }
    }

}
