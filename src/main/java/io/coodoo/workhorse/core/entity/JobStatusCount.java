package io.coodoo.workhorse.core.entity;

/**
 * Class that contain the number of job by status
 * 
 * @author coodoo GmbH (coodoo.io)
 */
public class JobStatusCount {

    private long total;
    private long active;
    private long inactive;
    private long error;
    private long noWorker;

    public JobStatusCount() {}

    public JobStatusCount(long active, long inactive, long error, long noWorker) {
        this.active = active;
        this.inactive = inactive;
        this.error = error;
        this.noWorker = noWorker;
        this.total = this.active + this.inactive + this.error + this.noWorker;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getActive() {
        return active;
    }

    public void setActive(long active) {
        this.active = active;
    }

    public long getInactive() {
        return inactive;
    }

    public void setInactive(long inactive) {
        this.inactive = inactive;
    }

    public long getError() {
        return error;
    }

    public void setError(long error) {
        this.error = error;
    }

    public long getNoWorker() {
        return noWorker;
    }

    public void setNoWorker(long noWorker) {
        this.noWorker = noWorker;
    }

}
