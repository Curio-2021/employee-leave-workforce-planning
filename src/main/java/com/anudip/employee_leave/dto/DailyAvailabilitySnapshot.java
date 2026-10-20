package com.anudip.employee_leave.dto;

import com.anudip.employee_leave.entity.ConflictSeverity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DailyAvailabilitySnapshot {

    private LocalDate date;
    private boolean workingDay;
    private boolean holiday;
    private String holidayName;
    private boolean weekend;

    private int totalActiveStaff;
    private int requiredStaff;
    private double approvedAbsences;
    private double pendingAbsences;
    private double availableStaff;
    private double projectedAvailableStaff;
    private double deficit;
    private ConflictSeverity severity = ConflictSeverity.NONE;
    private String explanation;

    private List<EmployeeAbsenceDetail> approvedAbsenceDetails = new ArrayList<>();
    private List<EmployeeAbsenceDetail> pendingAbsenceDetails = new ArrayList<>();

    public DailyAvailabilitySnapshot() {
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public boolean isWorkingDay() {
        return workingDay;
    }

    public void setWorkingDay(boolean workingDay) {
        this.workingDay = workingDay;
    }

    public boolean isHoliday() {
        return holiday;
    }

    public void setHoliday(boolean holiday) {
        this.holiday = holiday;
    }

    public String getHolidayName() {
        return holidayName;
    }

    public void setHolidayName(String holidayName) {
        this.holidayName = holidayName;
    }

    public boolean isWeekend() {
        return weekend;
    }

    public void setWeekend(boolean weekend) {
        this.weekend = weekend;
    }

    public int getTotalActiveStaff() {
        return totalActiveStaff;
    }

    public void setTotalActiveStaff(int totalActiveStaff) {
        this.totalActiveStaff = totalActiveStaff;
    }

    public int getRequiredStaff() {
        return requiredStaff;
    }

    public void setRequiredStaff(int requiredStaff) {
        this.requiredStaff = requiredStaff;
    }

    public double getApprovedAbsences() {
        return approvedAbsences;
    }

    public void setApprovedAbsences(double approvedAbsences) {
        this.approvedAbsences = approvedAbsences;
    }

    public double getPendingAbsences() {
        return pendingAbsences;
    }

    public void setPendingAbsences(double pendingAbsences) {
        this.pendingAbsences = pendingAbsences;
    }

    public double getAvailableStaff() {
        return availableStaff;
    }

    public void setAvailableStaff(double availableStaff) {
        this.availableStaff = availableStaff;
    }

    public double getProjectedAvailableStaff() {
        return projectedAvailableStaff;
    }

    public void setProjectedAvailableStaff(double projectedAvailableStaff) {
        this.projectedAvailableStaff = projectedAvailableStaff;
    }

    public double getDeficit() {
        return deficit;
    }

    public void setDeficit(double deficit) {
        this.deficit = deficit;
    }

    public ConflictSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(ConflictSeverity severity) {
        this.severity = severity;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public List<EmployeeAbsenceDetail> getApprovedAbsenceDetails() {
        return approvedAbsenceDetails;
    }

    public void setApprovedAbsenceDetails(List<EmployeeAbsenceDetail> approvedAbsenceDetails) {
        this.approvedAbsenceDetails = approvedAbsenceDetails;
    }

    public List<EmployeeAbsenceDetail> getPendingAbsenceDetails() {
        return pendingAbsenceDetails;
    }

    public void setPendingAbsenceDetails(List<EmployeeAbsenceDetail> pendingAbsenceDetails) {
        this.pendingAbsenceDetails = pendingAbsenceDetails;
    }
}

