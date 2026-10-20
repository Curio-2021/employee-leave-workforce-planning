package com.anudip.employee_leave.dto;

public class EmployeeAbsenceDetail {

    private Long requestId;
    private Long employeeId;
    private String employeeName;
    private String employeeIdentifier;
    private String leaveTypeName;
    private String requestNumber;
    private String status;
    private boolean halfDay;
    private double absenceWeight;

    public EmployeeAbsenceDetail() {
    }

    public EmployeeAbsenceDetail(Long employeeId, String employeeName, String employeeIdentifier,
                                 String leaveTypeName, String requestNumber, String status,
                                 boolean halfDay, double absenceWeight) {
        this(null, employeeId, employeeName, employeeIdentifier, leaveTypeName, requestNumber, status, halfDay, absenceWeight);
    }

    public EmployeeAbsenceDetail(Long requestId, Long employeeId, String employeeName, String employeeIdentifier,
                                 String leaveTypeName, String requestNumber, String status,
                                 boolean halfDay, double absenceWeight) {
        this.requestId = requestId;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.employeeIdentifier = employeeIdentifier;
        this.leaveTypeName = leaveTypeName;
        this.requestNumber = requestNumber;
        this.status = status;
        this.halfDay = halfDay;
        this.absenceWeight = absenceWeight;
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public String getEmployeeCode() {
        return employeeIdentifier;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getEmployeeIdentifier() {
        return employeeIdentifier;
    }

    public void setEmployeeIdentifier(String employeeIdentifier) {
        this.employeeIdentifier = employeeIdentifier;
    }

    public String getLeaveTypeName() {
        return leaveTypeName;
    }

    public void setLeaveTypeName(String leaveTypeName) {
        this.leaveTypeName = leaveTypeName;
    }

    public String getRequestNumber() {
        return requestNumber;
    }

    public void setRequestNumber(String requestNumber) {
        this.requestNumber = requestNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isHalfDay() {
        return halfDay;
    }

    public void setHalfDay(boolean halfDay) {
        this.halfDay = halfDay;
    }

    public double getAbsenceWeight() {
        return absenceWeight;
    }

    public void setAbsenceWeight(double absenceWeight) {
        this.absenceWeight = absenceWeight;
    }
}

