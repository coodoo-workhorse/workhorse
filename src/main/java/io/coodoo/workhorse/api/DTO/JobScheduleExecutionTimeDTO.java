package io.coodoo.workhorse.api.DTO;

import java.time.LocalDateTime;
import java.util.List;

public class JobScheduleExecutionTimeDTO {

    public Long jobId;
    public String jobName;
    public String schedule;
    public List<LocalDateTime> executions;

}
