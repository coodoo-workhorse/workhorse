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
@InitialJobConfig(name = "Workhorse System: Check running executions", schedule = "20 */5 * * * *", failRetries = 1,
                description = "Check for stuck running executions that are no longer executed.", tags = "System")
public class CheckRunningExecutionsWorker extends Worker {

    private final Logger log = LoggerFactory.getLogger(CheckRunningExecutionsWorker.class);

    @Inject
    @ExecutionQualifier
    ExecutionPersistence executionPersistence;

    @Override
    public String doWork() throws Exception {
        if (StaticConfig.EXECUTION_TIMEOUT <= 0) {
            return "The system job is deactivate";
        }

        LocalDateTime time = WorkhorseUtil.timestamp().minusSeconds(StaticConfig.EXECUTION_TIMEOUT);
        List<Execution> timeoutExecutions = executionPersistence.findTimeoutExecutions(time);

        if (timeoutExecutions.isEmpty()) {
            return "They are no running executions in timeout ";
        }

        for (Execution timeoutExecution : timeoutExecutions) {
            log.warn("Stuck running execution found! {}", timeoutExecution);

            ExecutionStatus statusTransition = StaticConfig.EXECUTION_TIMEOUT_STATUS;
            String logMessage = "Stuck running execution found (ID: " + timeoutExecution.getId() + "): ";

            switch (statusTransition) {
                case QUEUED:
                    Execution retryExecution = workhorseController.createRetryExecution(timeoutExecution);
                    workhorseController.setExecutionStatusToFailed(timeoutExecution, ExecutionFailStatus.TIMEOUT);
                    log.trace("Execution in timeout killed and risen from the death! Now it is {}", retryExecution);
                    workhorseLogService.logMessage(logMessage + "Marked as failed and queued a clone (ID: " + retryExecution.getId() + ")",
                                    timeoutExecution.getJobId(), false);
                    break;
                case RUNNING:
                    log.warn("Execution in timeout will remain in status {}", statusTransition);
                    workhorseLogService.logMessage(logMessage + "No action is taken", timeoutExecution.getJobId(), false);
                    break;
                default:
                    workhorseController.updateExecutionStatus(timeoutExecution.getJobId(), timeoutExecution.getId(), statusTransition);
                    log.trace("Execution in timeout is put in status {}", statusTransition);
                    workhorseLogService.logMessage(logMessage + "Put in status " + statusTransition, timeoutExecution.getJobId(), false);
                    break;
            }
        }
        return "The check was successfull ";
    }
}
