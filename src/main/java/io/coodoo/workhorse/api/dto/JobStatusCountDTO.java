package io.coodoo.workhorse.api.dto;

import io.coodoo.workhorse.core.entity.JobStatusCount;

public class JobStatusCountDTO {

    public long total;
    public long active;
    public long inactive;
    public long error;
    public long noWorker;

    public JobStatusCountDTO() {}

    public JobStatusCountDTO(JobStatusCount jobStatusCount) {
        this.active = jobStatusCount.getActive();
        this.inactive = jobStatusCount.getInactive();
        this.error = jobStatusCount.getError();
        this.noWorker = jobStatusCount.getNoWorker();
        this.total = jobStatusCount.getTotal();
    }
}
