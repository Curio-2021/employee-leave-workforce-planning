package com.anudip.employee_leave.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Immutable system audit event entity for tracking critical operational,
 * administrative, and configuration activities across the platform.
 */
@Entity
@Table(name = "system_audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Timestamp is required")
    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime eventTimestamp = LocalDateTime.now();

    @NotBlank(message = "Actor is required")
    @Size(max = 150, message = "Actor description cannot exceed 150 characters")
    @Column(name = "actor", nullable = false, length = 150)
    private String actor;

    @NotBlank(message = "Action is required")
    @Size(max = 60, message = "Action cannot exceed 60 characters")
    @Column(name = "action", nullable = false, length = 60)
    private String action;

    @NotBlank(message = "Entity type is required")
    @Size(max = 60, message = "Entity type cannot exceed 60 characters")
    @Column(name = "entity_type", nullable = false, length = 60)
    private String entityType;

    @Size(max = 100, message = "Entity ID cannot exceed 100 characters")
    @Column(name = "entity_id", length = 100)
    private String entityId;

    @Size(max = 1500, message = "Details cannot exceed 1500 characters")
    @Column(name = "details", length = 1500)
    private String details;

    @Column(name = "department_id")
    private Long departmentId;

    public AuditEvent() {
    }

    public AuditEvent(String actor, String action, String entityType, String entityId, Long departmentId, String details) {
        this.eventTimestamp = LocalDateTime.now();
        this.actor = actor;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.departmentId = departmentId;
        this.details = details;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(LocalDateTime eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }
}

