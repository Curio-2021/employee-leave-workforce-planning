package com.anudip.employee_leave.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class LeaveAdjustmentDTO {

    private Long leaveBalanceId;

    @NotNull(message = "Employee is required")
    private Long employeeId;

    @NotNull(message = "Leave type is required")
    private Long leaveTypeId;

    @NotNull(message = "Leave year is required")
    private Integer leaveYear = 2026;

    @NotBlank(message = "Adjustment type is required (CREDIT or DEBIT)")
    private String adjustmentType = "CREDIT";

    @NotNull(message = "Adjustment amount is required")
    @DecimalMin(value = "0.25", message = "Adjustment amount must be at least 0.25 days")
    @DecimalMax(value = "100.00", message = "Adjustment amount cannot exceed 100 days")
    private BigDecimal amount;

    @NotBlank(message = "Mandatory reason must be provided for audit tracking")
    @Size(min = 5, max = 255, message = "Reason must be between 5 and 255 characters")
    private String reason;

    @Size(max = 100, message = "Reference number cannot exceed 100 characters")
    private String referenceNumber;

    private String actor = "HR_ADMIN";

    public LeaveAdjustmentDTO() {
    }

    public Long getLeaveBalanceId() {
        return leaveBalanceId;
    }

    public void setLeaveBalanceId(Long leaveBalanceId) {
        this.leaveBalanceId = leaveBalanceId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public Long getLeaveTypeId() {
        return leaveTypeId;
    }

    public void setLeaveTypeId(Long leaveTypeId) {
        this.leaveTypeId = leaveTypeId;
    }

    public Integer getLeaveYear() {
        return leaveYear;
    }

    public void setLeaveYear(Integer leaveYear) {
        this.leaveYear = leaveYear;
    }

    public String getAdjustmentType() {
        return adjustmentType;
    }

    public void setAdjustmentType(String adjustmentType) {
        this.adjustmentType = adjustmentType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }
}

