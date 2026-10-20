package com.anudip.employee_leave.entity;

public enum HolidayType {
    MANDATORY("Mandatory / National"),
    RESTRICTED_OPTIONAL("Restricted / Optional"),
    COMPANY_OFF("Company Off");

    private final String displayName;

    HolidayType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

