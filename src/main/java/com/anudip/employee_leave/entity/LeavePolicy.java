package com.anudip.employee_leave.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "leave_policies")
public class LeavePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Policy name is required")
    @Size(min = 3, max = 100, message = "Policy name must be between 3 and 100 characters")
    @Column(name = "policy_name", nullable = false, length = 100)
    private String policyName;

    @NotNull(message = "Leave type is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    // Optional: null means applies to all departments
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department applicableDepartment;

    // Optional: null means applies to all employment types
    @Enumerated(EnumType.STRING)
    @Column(name = "applicable_employment_type", length = 30)
    private EmploymentType applicableEmploymentType;

    @Min(value = 1, message = "Annual allocation must be at least 1 day")
    @Max(value = 365, message = "Annual allocation cannot exceed 365 days")
    @Column(name = "annual_allocation", nullable = false)
    private int annualAllocation;

    @Column(name = "allow_carry_forward", nullable = false)
    private boolean allowCarryForward = false;

    @Min(value = 0, message = "Max carry forward days cannot be negative")
    @Max(value = 100, message = "Max carry forward days cannot exceed 100 days")
    @Column(name = "max_carry_forward_days", nullable = false)
    private int maxCarryForwardDays = 0;

    @Min(value = 0, message = "Minimum service days cannot be negative")
    @Column(name = "min_service_days", nullable = false)
    private int minServiceDays = 0;

    @NotNull(message = "Effective year is required")
    @Min(value = 2000, message = "Year must be 2000 or later")
    @Max(value = 2100, message = "Year must be 2100 or earlier")
    @Column(name = "effective_year", nullable = false)
    private int effectiveYear;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public LeavePolicy() {
    }

    public LeavePolicy(String policyName, LeaveType leaveType, Department applicableDepartment,
                       EmploymentType applicableEmploymentType, int annualAllocation,
                       boolean allowCarryForward, int maxCarryForwardDays,
                       int minServiceDays, int effectiveYear, boolean active) {
        this.policyName = policyName;
        this.leaveType = leaveType;
        this.applicableDepartment = applicableDepartment;
        this.applicableEmploymentType = applicableEmploymentType;
        this.annualAllocation = annualAllocation;
        this.allowCarryForward = allowCarryForward;
        this.maxCarryForwardDays = allowCarryForward ? maxCarryForwardDays : 0;
        this.minServiceDays = minServiceDays;
        this.effectiveYear = effectiveYear;
        this.active = active;
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

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(LeaveType leaveType) {
        this.leaveType = leaveType;
    }

    public Department getApplicableDepartment() {
        return applicableDepartment;
    }

    public void setApplicableDepartment(Department applicableDepartment) {
        this.applicableDepartment = applicableDepartment;
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
        this.maxCarryForwardDays = allowCarryForward ? maxCarryForwardDays : 0;
    }

    public int getMinServiceDays() {
        return minServiceDays;
    }

    public void setMinServiceDays(int minServiceDays) {
        this.minServiceDays = minServiceDays;
    }

    public int getEffectiveYear() {
        return effectiveYear;
    }

    public void setEffectiveYear(int effectiveYear) {
        this.effectiveYear = effectiveYear;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

