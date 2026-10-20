package com.anudip.employee_leave.exception;

public class InvalidLeaveRequestException extends RuntimeException {

    public InvalidLeaveRequestException(String message) {
        super(message);
    }

    public InvalidLeaveRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}

