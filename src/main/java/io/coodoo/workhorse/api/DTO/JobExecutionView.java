package io.coodoo.workhorse.api.DTO;

import java.time.LocalDateTime;

import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.core.entity.Execution;
import io.coodoo.workhorse.core.entity.ExecutionStatus;
import io.coodoo.workhorse.core.entity.JobStatus;

public class JobExecutionView {

    public Long id;

    public LocalDateTime createdAt;

    public LocalDateTime updatedAt;

    public Long jobId;

    public String jobName;

    public String jobDescription;

    public JobStatus jobStatus;

    public int jobFailRetries;

    public int jobThreads;

    public ExecutionStatus status;

    public LocalDateTime startedAt;

    public LocalDateTime endedAt;

    public boolean priority;

    public LocalDateTime maturity;

    public Long batchId;

    public Long chainId;

    public Long chainPreviousExecutionId;

    public Long duration;

    public String parameters;

    public int failRetry;

    public Long failRetryExecutionId;

    public String failMessage;

   public JobExecutionView () {}

   public JobExecutionView(Job job, Execution jobExecution) {
       this.id = jobExecution.getId();
       this.createdAt = jobExecution.getCreatedAt();
       this.updatedAt = jobExecution.getUpdatedAt();
       this.jobId = jobExecution.getJobId();
       this.jobName = job.getName();
       this.jobDescription = job.getDescription();
       this.jobStatus = job.getStatus();
       this.jobFailRetries = job.getFailRetries();
       this.jobThreads = job.getThreads();
       this.status = jobExecution.getStatus();
       this.startedAt = jobExecution.getStartedAt();
       this.endedAt = jobExecution.getEndedAt();
       this.priority = jobExecution.getPriority();
       this.maturity = jobExecution.getMaturity();
       this.batchId = jobExecution.getBatchId();
       this.chainId = jobExecution.getChainId();
       this.chainPreviousExecutionId = jobExecution.getChainedPreviousExecutionId();
       this.duration = jobExecution.getDuration();
       this.parameters = jobExecution.getParameters();
       this.failRetry = jobExecution.getFailRetry();
       this.failRetryExecutionId = jobExecution.getFailRetryJobExecutionId();
       this.failMessage = jobExecution.getFailMessage();
   }
   
}
