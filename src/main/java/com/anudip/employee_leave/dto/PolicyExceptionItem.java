package com.anudip.employee_leave.dto;

import java.time.LocalDateTime;

public class PolicyExceptionItem {

    private String id;
    private String category;
    private String categoryDisplayName;
    private String severity;
    private LocalDateTime timestamp;
    private String referenceNumber;
    private String employeeName;
    private String employeeIdentifier;
    private String departmentName;
    private Long departmentId;
    private String details;
    private String justification;
    private String actionUrl;

    public PolicyExceptionItem() {
    }

    public PolicyExceptionItem(String id, String category, String categoryDisplayName,
                               String severity, LocalDateTime timestamp, String referenceNumber,
                               String employeeName, String employeeIdentifier, String departmentName,
                               Long departmentId, String details, String justification, String actionUrl) {
        this.id = id;
        this.category = category;
        this.categoryDisplayName = categoryDisplayName;
        this.severity = severity;
        this.timestamp = timestamp;
        this.referenceNumber = referenceNumber;
        this.employeeName = employeeName;
        this.employeeIdentifier = employeeIdentifier;
        this.departmentName = departmentName;
        this.departmentId = departmentId;
        this.details = details;
        this.justification = justification;
        this.actionUrl = actionUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategoryDisplayName() {
        return categoryDisplayName;
    }

    public void setCategoryDisplayName(String categoryDisplayName) {
        this.categoryDisplayName = categoryDisplayName;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getEmployeeIdentifier() {
        return employeeIdentifier;
    }

    public void setEmployeeIdentifier(String employeeIdentifier) {
        this.employeeIdentifier = employeeIdentifier;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public void setActionUrl(String actionUrl) {
        this.actionUrl = actionUrl;
    }
}

