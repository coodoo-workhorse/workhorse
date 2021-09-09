package io.coodoo.workhorse.core.control.event;

import io.coodoo.workhorse.core.entity.Execution;

/**
 * @author coodoo GmbH (coodoo.io)
 */
public class NewExecutionEvent {

    public Execution execution;

    public NewExecutionEvent() {}

    public NewExecutionEvent(Execution execution) {
        this.execution = execution;
    }

    @Override
    public String toString() {
        return "NewExecutionEvent [execution=" + execution == null ? null : (execution.getId()) + "]";
    }

}
