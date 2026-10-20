package com.anudip.employee_leave.entity;

public enum ConflictSeverity {
    NONE("Optimal Staffing", "success"),
    WARNING_PENDING_RISK("Pending Risk", "warning"),
    CRITICAL_STAFFING_SHORTAGE("Critical Shortage", "danger");

    private final String displayName;
    private final String badgeClass;

    ConflictSeverity(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public boolean isShortage() {
        return this == CRITICAL_STAFFING_SHORTAGE;
    }

    public boolean isWarning() {
        return this == WARNING_PENDING_RISK;
    }
}

