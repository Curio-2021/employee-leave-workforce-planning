package com.anudip.employee_leave.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class DepartmentStaffingRuleDTO {

    private Long id;

    @NotNull(message = "Department selection is required")
    private Long departmentId;

    private String departmentName;

    @Min(value = 0, message = "Minimum staff count cannot be negative")
    @Max(value = 1000, message = "Minimum staff count exceeds reasonable limit")
    private int minimumStaffCount = 1;

    @Min(value = 0, message = "Percentage cannot be negative")
    @Max(value = 100, message = "Percentage cannot exceed 100%")
    private Integer minimumStaffPercentage;

    private boolean strictEnforcement = false;

    private boolean includeWeekends = false;

    private boolean active = true;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    public DepartmentStaffingRuleDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public int getMinimumStaffCount() {
        return minimumStaffCount;
    }

    public void setMinimumStaffCount(int minimumStaffCount) {
        this.minimumStaffCount = minimumStaffCount;
    }

    public Integer getMinimumStaffPercentage() {
        return minimumStaffPercentage;
    }

    public void setMinimumStaffPercentage(Integer minimumStaffPercentage) {
        this.minimumStaffPercentage = minimumStaffPercentage;
    }

    public boolean isStrictEnforcement() {
        return strictEnforcement;
    }

    public void setStrictEnforcement(boolean strictEnforcement) {
        this.strictEnforcement = strictEnforcement;
    }

    public boolean isIncludeWeekends() {
        return includeWeekends;
    }

    public void setIncludeWeekends(boolean includeWeekends) {
        this.includeWeekends = includeWeekends;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

