package io.coodoo.workhorse.jobengine.control.events;

import io.coodoo.workhorse.jobengine.entity.Job;

public class AllJobExecutionsDoneJobEvent {
    
    private Job job;

    public AllJobExecutionsDoneJobEvent(Job job) {
        this.job = job;
    }

    public Job getJob() {
        return job;
    }
}
