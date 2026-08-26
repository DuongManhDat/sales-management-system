package com.shop.model;

import java.time.LocalDateTime;

public class ActivityLog {
    private int id;
    private String action;
    private String entity;
    private Integer entityId;
    private String detail;
    private LocalDateTime createdAt;

    public ActivityLog() {
    }

    public ActivityLog(String action, String entity, Integer entityId, String detail, LocalDateTime createdAt) {
        this.action = action;
        this.entity = entity;
        this.entityId = entityId;
        this.detail = detail;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getEntity() { return entity; }
    public void setEntity(String entity) { this.entity = entity; }

    public Integer getEntityId() { return entityId; }
    public void setEntityId(Integer entityId) { this.entityId = entityId; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
