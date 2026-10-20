package com.anudip.employee_leave.entity;

public enum EmploymentStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive");

    private final String displayName;

    EmploymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

