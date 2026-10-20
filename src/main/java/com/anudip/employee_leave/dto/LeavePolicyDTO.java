package com.anudip.employee_leave.dto;

import com.anudip.employee_leave.entity.EmploymentType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class LeavePolicyDTO {

    private Long id;

    @NotBlank(message = "Policy name is required")
    @Size(min = 3, max = 100, message = "Policy name must be between 3 and 100 characters")
    private String policyName;

    @NotNull(message = "Leave type selection is required")
    private Long leaveTypeId;

    private Long departmentId;

    private EmploymentType applicableEmploymentType;

    @Min(value = 1, message = "Annual allocation must be at least 1 day")
    @Max(value = 365, message = "Annual allocation cannot exceed 365 days")
    private int annualAllocation = 12;

    private boolean allowCarryForward = false;

    @Min(value = 0, message = "Max carry forward days cannot be negative")
    @Max(value = 100, message = "Max carry forward days cannot exceed 100 days")
    private int maxCarryForwardDays = 0;

    @Min(value = 0, message = "Minimum service days cannot be negative")
    private int minServiceDays = 0;

    @NotNull(message = "Effective year is required")
    @Min(value = 2000, message = "Effective year must be 2000 or later")
    @Max(value = 2100, message = "Effective year must be 2100 or earlier")
    private Integer effectiveYear = 2026;

    private boolean active = true;

    public LeavePolicyDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public Long getLeaveTypeId() {
        return leaveTypeId;
    }

    public void setLeaveTypeId(Long leaveTypeId) {
        this.leaveTypeId = leaveTypeId;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public EmploymentType getApplicableEmploymentType() {
        return applicableEmploymentType;
    }

    public void setApplicableEmploymentType(EmploymentType applicableEmploymentType) {
        this.applicableEmploymentType = applicableEmploymentType;
    }

    public int getAnnualAllocation() {
        return annualAllocation;
    }

    public void setAnnualAllocation(int annualAllocation) {
        this.annualAllocation = annualAllocation;
    }

    public boolean isAllowCarryForward() {
        return allowCarryForward;
    }

    public void setAllowCarryForward(boolean allowCarryForward) {
        this.allowCarryForward = allowCarryForward;
        if (!allowCarryForward) {
            this.maxCarryForwardDays = 0;
        }
    }

    public int getMaxCarryForwardDays() {
        return maxCarryForwardDays;
    }

    public void setMaxCarryForwardDays(int maxCarryForwardDays) {
        this.maxCarryForwardDays = maxCarryForwardDays;
    }

    public int getMinServiceDays() {
        return minServiceDays;
    }

    public void setMinServiceDays(int minServiceDays) {
        this.minServiceDays = minServiceDays;
    }

    public Integer getEffectiveYear() {
        return effectiveYear;
    }

    public void setEffectiveYear(Integer effectiveYear) {
        this.effectiveYear = effectiveYear;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

