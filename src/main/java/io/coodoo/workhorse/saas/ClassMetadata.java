package io.coodoo.workhorse.saas;

import io.coodoo.workhorse.core.entity.JobStatus;

public class ClassMetadata {

    public String workerClassName;
    public String parameterClassName;
    public String name;
    public String description;
    public JobStatus status;
    public int maxPerMinute;
    public boolean uniqueQueued;

    public ClassMetadata() {}

}
