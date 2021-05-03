package io.coodoo.workhorse.api.dto;

import java.util.List;

import io.coodoo.workhorse.core.entity.ExecutionStatus;
import io.coodoo.workhorse.core.entity.JobExecutionStatusSummary;

public class JobExecutionStatusSummariesDTO {

    public ExecutionStatus status;
    /**
     * Summe aller Executions im oben benannten status
     */
    public Long count;
    public List<JobExecutionStatusSummary> jobExecutionStatusSummaries;

    public JobExecutionStatusSummariesDTO() {}

    public JobExecutionStatusSummariesDTO(ExecutionStatus status, Long count, List<JobExecutionStatusSummary> jobExecutionStatusSummaries) {
        this.status = status;
        this.count = count;
        this.jobExecutionStatusSummaries = jobExecutionStatusSummaries;
    }

}
