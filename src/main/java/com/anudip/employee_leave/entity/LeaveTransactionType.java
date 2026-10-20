package com.anudip.employee_leave.entity;

public enum LeaveTransactionType {
    ENTITLEMENT_GRANT("Entitlement Grant"),
    CARRY_FORWARD("Carry Forward"),
    MANUAL_ADJUSTMENT_CREDIT("Manual Adjustment (Credit)"),
    MANUAL_ADJUSTMENT_DEBIT("Manual Adjustment (Debit)"),
    APPROVED_LEAVE_DEDUCTION("Approved Leave Deduction"),
    REVERSAL_CORRECTION("Reversal / Correction");

    private final String displayName;

    LeaveTransactionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

