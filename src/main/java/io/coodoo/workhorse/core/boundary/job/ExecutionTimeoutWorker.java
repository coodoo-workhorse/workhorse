package io.coodoo.workhorse.core.boundary.job;

import java.time.LocalDateTime;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.coodoo.workhorse.core.boundary.Worker;
import io.coodoo.workhorse.core.boundary.annotation.InitialJobConfig;
import io.coodoo.workhorse.core.control.StaticConfig;
import io.coodoo.workhorse.core.entity.Execution;
import io.coodoo.workhorse.core.entity.ExecutionFailStatus;
import io.coodoo.workhorse.core.entity.ExecutionStatus;
import io.coodoo.workhorse.persistence.interfaces.ExecutionPersistence;
import io.coodoo.workhorse.persistence.interfaces.qualifier.ExecutionQualifier;
import io.coodoo.workhorse.util.WorkhorseUtil;

/**
 * @author coodoo GmbH (coodoo.io)
 */
@ApplicationScoped
@InitialJobConfig(name = "Hunt timeout executions ", schedule = "5 * * * * *", failRetries = 1, description = "Hunt too long running executions and cure them.")
public class ExecutionTimeoutWorker extends Worker {

    private final Logger log = LoggerFactory.getLogger(ExecutionTimeoutWorker.class);

    @Inject
    @ExecutionQualifier
    ExecutionPersistence executionPersistence;

    @Override
    public void doWork() throws Exception {
        if (StaticConfig.EXECUTION_TIMEOUT <= 0) {
            return;
        }

        LocalDateTime time = WorkhorseUtil.timestamp().minusSeconds(StaticConfig.EXECUTION_TIMEOUT);
        List<Execution> timeoutExecutions = executionPersistence.findTimeoutExecutions(time);

        if (timeoutExecutions.isEmpty()) {
            return;
        }

        for (Execution timeoutExecution : timeoutExecutions) {
            log.warn("Execution in timeout found! {}", timeoutExecution);

            ExecutionStatus cure = StaticConfig.EXECUTION_TIMEOUT_STATUS;
            String logMessage = "Execution in timeout found (ID: " + timeoutExecution.getId() + "): ";

            switch (cure) {
                case QUEUED:
                    Execution retryExecution = workhorseController.createRetryExecution(timeoutExecution);
                    workhorseController.setExecutionStatusToFailed(timeoutExecution, ExecutionFailStatus.TIMEOUT);
                    log.trace("Execution in timeout killed and risen from the death! Now it is {}", retryExecution);
                    workhorseLogService.logMessage(logMessage + "Marked as failed and queued a clone", timeoutExecution.getJobId(), false);
                    break;
                case RUNNING:
                    log.warn("Execution in timeout will still walk free with status {}", cure);
                    workhorseLogService.logMessage(logMessage + "No action is taken", timeoutExecution.getJobId(), false);
                    break;
                default:
                    workhorseController.updateExecutionStatus(timeoutExecution.getJobId(), timeoutExecution.getId(), cure);
                    log.trace("Execution in timeout is cured with status {}", cure);
                    workhorseLogService.logMessage(logMessage + "Put in status " + cure, timeoutExecution.getJobId(), false);
                    break;
            }

        }
    }

}
