package io.coodoo.workhorse.core.control.event;

import io.coodoo.workhorse.core.entity.Job;

/**
 * @author coodoo GmbH (coodoo.io)
 */
public class AllJobExecutionsDoneEvent {

    private Job job;

    public AllJobExecutionsDoneEvent(Job job) {
        this.job = job;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AllJobExecutionsDoneEvent [job=");
        builder.append(job);
        builder.append("]");
        return builder.toString();
    }

}
