package io.coodoo.workhorse.saas;

public class DoWorkParameters {

    private Long jobId;
    private Long executionId;
    private String parameters;

    public DoWorkParameters() {}

    public DoWorkParameters(Long jobId, Long executionId, String parameters) {
        this.jobId = jobId;
        this.executionId = executionId;
        this.parameters = parameters;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Long getExecutionId() {
        return executionId;
    }

    public void setExecutionId(Long executionId) {
        this.executionId = executionId;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

}
