package io.coodoo.workhorse.core.entity;

import java.time.LocalDateTime;

/**
 * @author coodoo GmbH (coodoo.io)
 */
public abstract class BaseEntity {

    protected Long id;

    protected LocalDateTime createdAt;

    protected LocalDateTime updatedAt;

    protected BaseEntity() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
