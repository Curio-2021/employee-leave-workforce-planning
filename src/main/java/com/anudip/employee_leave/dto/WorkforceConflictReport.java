package com.anudip.employee_leave.dto;

import com.anudip.employee_leave.entity.ConflictSeverity;

import java.util.ArrayList;
import java.util.List;

public class WorkforceConflictReport {

    private boolean hasConflict;
    private ConflictSeverity severity = ConflictSeverity.NONE;
    private boolean blockingEnabled;
    private boolean ruleConfigured;
    private int minimumStaffCount;
    private Integer minimumStaffPercentage;
    private int calculatedRequiredStaff;
    private String ruleFormulaExplanation;
    private String summaryMessage;

    private List<DailyAvailabilitySnapshot> affectedDates = new ArrayList<>();

    public WorkforceConflictReport() {
    }

    public boolean isHasConflict() {
        return hasConflict;
    }

    public void setHasConflict(boolean hasConflict) {
        this.hasConflict = hasConflict;
    }

    public ConflictSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(ConflictSeverity severity) {
        this.severity = severity;
    }

    public boolean isBlockingEnabled() {
        return blockingEnabled;
    }

    public void setBlockingEnabled(boolean blockingEnabled) {
        this.blockingEnabled = blockingEnabled;
    }

    public boolean isRuleConfigured() {
        return ruleConfigured;
    }

    public void setRuleConfigured(boolean ruleConfigured) {
        this.ruleConfigured = ruleConfigured;
    }

    public int getMinimumStaffCount() {
        return minimumStaffCount;
    }

    public void setMinimumStaffCount(int minimumStaffCount) {
        this.minimumStaffCount = minimumStaffCount;
    }

    public Integer getMinimumStaffPercentage() {
        return minimumStaffPercentage;
    }

    public void setMinimumStaffPercentage(Integer minimumStaffPercentage) {
        this.minimumStaffPercentage = minimumStaffPercentage;
    }

    public int getCalculatedRequiredStaff() {
        return calculatedRequiredStaff;
    }

    public void setCalculatedRequiredStaff(int calculatedRequiredStaff) {
        this.calculatedRequiredStaff = calculatedRequiredStaff;
    }

    public String getRuleFormulaExplanation() {
        return ruleFormulaExplanation;
    }

    public void setRuleFormulaExplanation(String ruleFormulaExplanation) {
        this.ruleFormulaExplanation = ruleFormulaExplanation;
    }

    public String getSummaryMessage() {
        return summaryMessage;
    }

    public void setSummaryMessage(String summaryMessage) {
        this.summaryMessage = summaryMessage;
    }

    public List<DailyAvailabilitySnapshot> getAffectedDates() {
        return affectedDates;
    }

    public void setAffectedDates(List<DailyAvailabilitySnapshot> affectedDates) {
        this.affectedDates = affectedDates;
    }

    // Template and Service alias accessors
    public ConflictSeverity getOverallSeverity() {
        return severity;
    }

    public boolean isHasConflicts() {
        return hasConflict;
    }

    public boolean isStrictRuleEnforced() {
        return blockingEnabled;
    }

    public String getExplanation() {
        return summaryMessage != null ? summaryMessage : ruleFormulaExplanation;
    }

    public int getRequiredStaff() {
        return calculatedRequiredStaff > 0 ? calculatedRequiredStaff : minimumStaffCount;
    }

    public List<DailyAvailabilitySnapshot> getDailySnapshots() {
        return affectedDates;
    }

    public double getMinimumProjectedAvailableStaff() {
        return affectedDates.stream()
                .filter(DailyAvailabilitySnapshot::isWorkingDay)
                .mapToDouble(DailyAvailabilitySnapshot::getAvailableStaff)
                .min()
                .orElse(0.0);
    }

    private String departmentName;

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }
}

