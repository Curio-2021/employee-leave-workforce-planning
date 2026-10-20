package com.anudip.employee_leave.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "department_staffing_rules")
public class DepartmentStaffingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Department is required")
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false, unique = true)
    private Department department;

    @Min(value = 0, message = "Minimum staff count cannot be negative")
    @Max(value = 1000, message = "Minimum staff count exceeds reasonable limit")
    @Column(name = "minimum_staff_count", nullable = false)
    private int minimumStaffCount = 1;

    @Min(value = 0, message = "Minimum staff percentage cannot be negative")
    @Max(value = 100, message = "Minimum staff percentage cannot exceed 100%")
    @Column(name = "minimum_staff_percentage")
    private Integer minimumStaffPercentage;

    @Column(name = "strict_enforcement", nullable = false)
    private boolean strictEnforcement = false;

    @Column(name = "include_weekends", nullable = false)
    private boolean includeWeekends = false;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    @Column(length = 255)
    private String description;

    @Version
    private Long version;

    public DepartmentStaffingRule() {
    }

    public DepartmentStaffingRule(Department department, int minimumStaffCount, Integer minimumStaffPercentage,
                                  boolean strictEnforcement, boolean includeWeekends, boolean active, String description) {
        this.department = department;
        this.minimumStaffCount = minimumStaffCount;
        this.minimumStaffPercentage = minimumStaffPercentage;
        this.strictEnforcement = strictEnforcement;
        this.includeWeekends = includeWeekends;
        this.active = active;
        this.description = description;
    }

    /**
     * Calculates the required minimum staffing headcount for a given active department size.
     * Formula: max(minimumStaffCount, ceil(activeStaffCount * (minimumStaffPercentage / 100)))
     */
    public int calculateRequiredStaff(int activeStaffCount) {
        int required = this.minimumStaffCount;
        if (this.minimumStaffPercentage != null && this.minimumStaffPercentage > 0 && activeStaffCount > 0) {
            int percentageRequired = (int) Math.ceil((activeStaffCount * this.minimumStaffPercentage) / 100.0);
            required = Math.max(required, percentageRequired);
        }
        return Math.max(0, required);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}

