package io.coodoo.workhorse.saas;

import java.time.LocalDateTime;
import java.util.List;

public class GroupExecutions {

    List<String> parametersList;
    Boolean priority;
    LocalDateTime plannedFor;
    LocalDateTime expiresAt;

    public GroupExecutions() {}

    public GroupExecutions(List<String> parametersList, Boolean priority, LocalDateTime plannedFor, LocalDateTime expiresAt) {
        this.parametersList = parametersList;
        this.priority = priority;
        this.plannedFor = plannedFor;
        this.expiresAt = expiresAt;
    }

    public List<String> getParametersList() {
        return parametersList;
    }

    public void setParametersList(List<String> parametersList) {
        this.parametersList = parametersList;
    }

    public Boolean getPriority() {
        return priority;
    }

    public void setPriority(Boolean priority) {
        this.priority = priority;
    }

    public LocalDateTime getPlannedFor() {
        return plannedFor;
    }

    public void setPlannedFor(LocalDateTime plannedFor) {
        this.plannedFor = plannedFor;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
