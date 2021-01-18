package io.coodoo.workhorse.core.control.event;

/**
 * @author coodoo GmbH (coodoo.io)
 */
public class NewExecutionEvent {

    public Long jobId;

    public Long executionId;

    public NewExecutionEvent(Long jobId, Long executionId) {
        this.jobId = jobId;
        this.executionId = executionId;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("NewExecutionEvent [jobId=");
        builder.append(jobId);
        builder.append(", executionId=");
        builder.append(executionId);
        builder.append("]");
        return builder.toString();
    }

}
