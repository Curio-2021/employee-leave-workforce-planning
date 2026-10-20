package com.anudip.employee_leave.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class LeaveInitializationDTO {

    @NotNull(message = "Employee selection is required")
    private Long employeeId;

    @NotNull(message = "Leave year is required")
    @Min(value = 2000, message = "Year must be 2000 or later")
    @Max(value = 2100, message = "Year must be 2100 or earlier")
    private Integer leaveYear = 2026;

    private Long leaveTypeId; // Optional: null means initialize for all matching active leave types

    private boolean includeCarryForward = true;

    private String actor = "HR_ADMIN";

    public LeaveInitializationDTO() {
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public Integer getLeaveYear() {
        return leaveYear;
    }

    public void setLeaveYear(Integer leaveYear) {
        this.leaveYear = leaveYear;
    }

    public Long getLeaveTypeId() {
        return leaveTypeId;
    }

    public void setLeaveTypeId(Long leaveTypeId) {
        this.leaveTypeId = leaveTypeId;
    }

    public boolean isIncludeCarryForward() {
        return includeCarryForward;
    }

    public void setIncludeCarryForward(boolean includeCarryForward) {
        this.includeCarryForward = includeCarryForward;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }
}

