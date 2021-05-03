package io.coodoo.workhorse.api.dto;

import java.time.LocalDateTime;

public class JobExecutionCountDTO {
    public Long jobId;
    public LocalDateTime from;
    public LocalDateTime to;
    public Long total;
    public Long planned;
    public Long queued;
    public Long running;
    public Long finished;
    public Long failed;
    public Long aborted;

    public JobExecutionCountDTO() {}

    public JobExecutionCountDTO(Long jobId, LocalDateTime from, LocalDateTime to, Long total, Long planned, Long queued, Long running, Long finished,
                    Long failed, Long aborted) {
        this.jobId = jobId;
        this.from = from;
        this.to = to;
        this.total = total;
        this.planned = planned;
        this.queued = queued;
        this.running = running;
        this.finished = finished;
        this.failed = failed;
        this.aborted = aborted;
    }

}
