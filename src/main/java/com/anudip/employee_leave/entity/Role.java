package com.anudip.employee_leave.entity;

public enum Role {
    ROLE_EMPLOYEE("Employee"),
    ROLE_MANAGER("Manager"),
    ROLE_HR_ADMIN("HR Administrator");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
