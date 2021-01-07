package io.coodoo.workhorse.jobengine.control;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.coodoo.workhorse.config.entity.JobEngineConfig;
import io.coodoo.workhorse.jobengine.entity.Job;
import io.coodoo.workhorse.jobengine.entity.JobExecution;
import io.coodoo.workhorse.storage.persistenceInterface.JobPersistence;
import io.coodoo.workhorse.storage.qualifiers.JobDAO;
import io.coodoo.workhorse.util.JobEngineUtil;

public abstract class BaseJobWorker {

  @Inject
  @JobDAO
  protected JobPersistence jobPersistence;

  @Inject
  protected JobEngineController jobEngineController;

  @Inject
  protected JobEngineConfig jobEngineConfig;

  private Job job;

  private StringBuffer logBuffer;

  public abstract void doWork(JobExecution jobExecution) throws Exception;

  /**
   * The job engine will call this callback method after the job execution is
   * finished. <br>
   * <i>If needed, this method can be overwritten to react on a finished job
   * execution.</i>
   * 
   * @param jobExecutionId ID of current job execution that is finished
   */
  public void onFinished(Long jobExecutionId) {
  }

  /**
   * The job engine will call this callback method after the last job execution of
   * a batch is finished. <br>
   * <i>If needed, this method can be overwritten to react on a finished
   * batch.</i>
   * 
   * @param batchId        batch ID
   * @param jobExecutionId ID of last job execution of a batch that is finished
   */
  public void onFinishedBatch(Long batchId, Long jobExecutionId) {
  }

  /**
   * The job engine will call this callback method after the last job execution of
   * a chain is finished. <br>
   * <i>If needed, this method can be overwritten to react on a finished
   * chain.</i>
   * 
   * @param chainId        chain ID
   * @param jobExecutionId ID of last job execution of a chain that is finished
   */
  public void onFinishedChain(Long chainId, Long jobExecutionId) {
  }

  /**
   * The job engine will call this callback method after the job execution has
   * failed and there will be a retry of the failed job execution. <br>
   * <i>If needed, this method can be overwritten to react on a retry job
   * execution.</i>
   * 
   * @param failedExecutionId ID of current job execution that has failed
   * @param retryExecutionId  ID of new job execution that that will retry the
   *                          failed one
   */
  public void onRetry(Long failedExecutionId, Long retryExecutionId) {
  }

  /**
   * The job engine will call this callback method after the job execution has
   * failed. <br>
   * <i>If needed, this method can be overwritten to react on a failed job
   * execution.</i>
   * 
   * @param jobExecutionId ID of current job execution that has failed
   */
  public void onFailed(Long jobExecutionId) {
  }

  /**
   * The job engine will call this callback method after a batch has failed. <br>
   * <i>If needed, this method can be overwritten to react on a failed batch.</i>
   * 
   * @param batchId        chain ID
   * @param jobExecutionId ID of last job execution of a batch that has failed
   */
  public void onFailedBatch(Long batchId, Long jobExecutionId) {
  }

  /**
   * The job engine will call this callback method after a chain has failed. <br>
   * <i>If needed, this method can be overwritten to react on a failed chain.</i>
   * 
   * @param chainId        chain ID
   * @param jobExecutionId ID of last job execution of a chain that has failed
   */
  public void onFailedChain(Long chainId, Long jobExecutionId) {
  }

  public Long createJobExecution() {
    return createJobExecution(null, null, null, null, null, null).getId();
  }

  protected JobExecution createJobExecution(Object parameters, Boolean priority, LocalDateTime maturity, Long batchId,
      Long chainId, Long chainedPreviousExecutionId) {
    Long jobId = getJob().getId();
    boolean uniqueInQueue = getJob().isUniqueInQueue();

    String parametersAsJson = JobEngineUtil.parametersToJson(parameters);

    return jobEngineController.createJobExecution(jobId, parametersAsJson, priority, maturity, batchId, chainId,
        chainedPreviousExecutionId, uniqueInQueue);

  }

  public Job getJob() {
    if (job == null) {
      job = jobPersistence.getByWorkerClassName(getClass().getName());
    }
    return job;
  }

  /**
   * This method retrieves the exact class of this job worker. Without this, the
   * proxy-client class will be retrieves.
   */
  public Class<? extends BaseJobWorker> getWorkerClass() {
    return getClass();
  }

  /**
   * @return Current Time by zone defined in {@link JobEngineConfig#TIME_ZONE}
   */
  public LocalDateTime timestamp() {
    return LocalDateTime.now(ZoneId.of(jobEngineConfig.getTimeZone()));
  }

  /**
   * Calculates the timestamp of the given delay from now ({@link #timestamp()})
   * 
   * @param delayValue delay value, e.g. <tt>30</tt>
   * @param delayUnit  delay unit, e.g. {@link ChronoUnit#MINUTES}
   * @return delay as timestamp
   */
  public LocalDateTime delayToMaturity(Long delayValue, ChronoUnit delayUnit) {

    LocalDateTime maturity = null;
    if (delayValue != null && delayUnit != null) {
      maturity = timestamp().plus(delayValue, delayUnit);
    }
    return maturity;
  }

  public void init(JobExecution jobExecution) {

    if (jobExecution != null && jobExecution.getLog() != null) {
      this.logBuffer = new StringBuffer(jobExecution.getLog());
    } else {
      this.logBuffer = new StringBuffer();
    }
  }

  public void logInfo(String message) {

    if (logBuffer != null) {
      if (logBuffer.length() > 0) {
        logBuffer.append(System.lineSeparator());
      }

      logBuffer.append(timestamp().format(DateTimeFormatter.ofPattern(jobEngineConfig.getLogTimeFormatter())));
      logBuffer.append(" ");

      if (jobEngineConfig.getLogInfoMarker() != null) {
        logBuffer.append(jobEngineConfig.getLogInfoMarker());
        logBuffer.append(" ");
      }

      logBuffer.append(message);
    }

  }

  public void logInfo(Logger logger, String message) {
    logger.info(message);
    logInfo(message);
  }

  public String getLog() {

    if (logBuffer != null && logBuffer.length() > 0) {
      return logBuffer.toString();
    }
    return null;
  }
}