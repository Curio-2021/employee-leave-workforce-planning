package com.anudip.employee_leave.exception;

public class StaffingShortageException extends RuntimeException {

    public StaffingShortageException(String message) {
        super(message);
    }

    public StaffingShortageException(String message, Throwable cause) {
        super(message, cause);
    }
}

