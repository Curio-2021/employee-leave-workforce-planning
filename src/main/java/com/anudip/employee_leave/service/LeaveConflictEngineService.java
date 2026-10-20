package com.anudip.employee_leave.service;

import com.anudip.employee_leave.dto.DailyAvailabilitySnapshot;
import com.anudip.employee_leave.dto.EmployeeAbsenceDetail;
import com.anudip.employee_leave.dto.WorkforceConflictReport;
import com.anudip.employee_leave.entity.ConflictSeverity;
import com.anudip.employee_leave.entity.Department;
import com.anudip.employee_leave.entity.DepartmentStaffingRule;
import com.anudip.employee_leave.entity.Employee;
import com.anudip.employee_leave.entity.EmploymentStatus;
import com.anudip.employee_leave.entity.LeaveRequest;
import com.anudip.employee_leave.entity.LeaveRequestStatus;
import com.anudip.employee_leave.repository.DepartmentStaffingRuleRepository;
import com.anudip.employee_leave.repository.EmployeeRepository;
import com.anudip.employee_leave.repository.LeaveRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LeaveConflictEngineService {

    private final DepartmentStaffingRuleRepository staffingRuleRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final WorkingCalendarService workingCalendarService;

    public LeaveConflictEngineService(DepartmentStaffingRuleRepository staffingRuleRepository,
                                      LeaveRequestRepository leaveRequestRepository,
                                      EmployeeRepository employeeRepository,
                                      WorkingCalendarService workingCalendarService) {
        this.staffingRuleRepository = staffingRuleRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
        this.workingCalendarService = workingCalendarService;
    }

    /**
     * Non-mutating evaluation of staffing impact if a proposed leave request is approved.
     * Evaluates daily staffing, handles partial days, and prevents double counting of the applicant.
     */
    @Transactional(readOnly = true)
    public WorkforceConflictReport evaluateLeaveRequestConflicts(LeaveRequest request) {
        WorkforceConflictReport report = new WorkforceConflictReport();

        Department department = null;
        if (request.getEmployee() != null && request.getEmployee().getId() != null) {
            Employee emp = employeeRepository.findById(request.getEmployee().getId()).orElse(request.getEmployee());
            department = emp.getDepartment();
        } else if (request.getEmployee() != null) {
            department = request.getEmployee().getDepartment();
        }

        if (department == null) {
            report.setHasConflict(false);
            report.setSeverity(ConflictSeverity.NONE);
            report.setSummaryMessage("Employee has no assigned department; skipping team availability conflict checks.");
            return report;
        }

        Long departmentId = department.getId();
        report.setDepartmentName(department.getName());
        Optional<DepartmentStaffingRule> ruleOpt = staffingRuleRepository.findByDepartmentId(departmentId)
                .filter(DepartmentStaffingRule::isActive);

        boolean ruleConfigured = ruleOpt.isPresent();
        report.setRuleConfigured(ruleConfigured);

        DepartmentStaffingRule rule = ruleOpt.orElse(null);
        int minCount = rule != null ? rule.getMinimumStaffCount() : 1;
        Integer minPct = rule != null ? rule.getMinimumStaffPercentage() : null;
        boolean strict = rule != null && rule.isStrictEnforcement();

        report.setMinimumStaffCount(minCount);
        report.setMinimumStaffPercentage(minPct);
        report.setBlockingEnabled(strict);

        if (ruleConfigured) {
            String pctDesc = minPct != null ? " and " + minPct + "% minimum capacity" : "";
            report.setRuleFormulaExplanation("Configured Department Rule: Requires at least " + minCount + " staff" + pctDesc +
                    (strict ? " (Strict Blocking Enabled)" : " (Advisory Warning Mode)"));
        } else {
            report.setRuleFormulaExplanation("Default Policy: Requiring at least 1 active staff member (Advisory Mode).");
        }

        List<DailyAvailabilitySnapshot> affectedDates = new ArrayList<>();
        ConflictSeverity highestSeverity = ConflictSeverity.NONE;
        boolean hasShortage = false;

        LocalDate current = request.getStartDate();
        while (!current.isAfter(request.getEndDate())) {
            boolean isWorkingDay = workingCalendarService.isWorkingDayForDepartment(departmentId, current);
            boolean isHoliday = workingCalendarService.isHoliday(current);

            DailyAvailabilitySnapshot snapshot = new DailyAvailabilitySnapshot();
            snapshot.setDate(current);
            snapshot.setWorkingDay(isWorkingDay);
            snapshot.setHoliday(isHoliday);
            snapshot.setWeekend(workingCalendarService.isStandardWeekend(current));

            if (!isWorkingDay) {
                snapshot.setSeverity(ConflictSeverity.NONE);
                snapshot.setRequiredStaff(0);
                snapshot.setExplanation(isHoliday ? "Official Holiday (No staffing required)" : "Non-Working Weekend Day");
                affectedDates.add(snapshot);
                current = current.plusDays(1);
                continue;
            }

            // Total eligible active department employees on this date
            LocalDate evalDate = current;
            List<Employee> activeStaff = employeeRepository.findFiltered(departmentId, EmploymentStatus.ACTIVE).stream()
                    .filter(e -> e.getJoiningDate() == null || !e.getJoiningDate().isAfter(evalDate))
                    .collect(Collectors.toList());
            int totalActive = activeStaff.size();
            snapshot.setTotalActiveStaff(totalActive);

            int requiredStaff = rule != null ? rule.calculateRequiredStaff(totalActive) : (totalActive > 0 ? 1 : 0);
            snapshot.setRequiredStaff(requiredStaff);
            report.setCalculatedRequiredStaff(requiredStaff);

            // Fetch other leaves in department covering this date
            List<LeaveRequest> otherLeaves = leaveRequestRepository.findDepartmentLeavesInDateRange(
                    departmentId, current, current, List.of(LeaveRequestStatus.APPROVED, LeaveRequestStatus.PENDING));

            double approvedAbsences = 0.0;
            double pendingAbsences = 0.0;
            List<EmployeeAbsenceDetail> approvedDetails = new ArrayList<>();
            List<EmployeeAbsenceDetail> pendingDetails = new ArrayList<>();

            for (LeaveRequest existing : otherLeaves) {
                // ANTI-DOUBLE-COUNTING GUARD:
                // Exclude the current request being evaluated and any other requests by the same applicant
                if (existing.getId() != null && existing.getId().equals(request.getId())) {
                    continue;
                }
                if (existing.getRequestNumber().equals(request.getRequestNumber())) {
                    continue;
                }
                if (existing.getEmployee().getId().equals(request.getEmployee().getId())) {
                    continue;
                }

                double weight = existing.isHalfDay() ? 0.50 : 1.00;
                EmployeeAbsenceDetail detail = new EmployeeAbsenceDetail(
                        existing.getId(),
                        existing.getEmployee().getId(),
                        existing.getEmployee().getFullName(),
                        existing.getEmployee().getEmployeeId(),
                        existing.getLeaveType().getName(),
                        existing.getRequestNumber(),
                        existing.getStatus().name(),
                        existing.isHalfDay(),
                        weight
                );

                if (existing.getStatus() == LeaveRequestStatus.APPROVED) {
                    approvedAbsences += weight;
                    approvedDetails.add(detail);
                } else if (existing.getStatus() == LeaveRequestStatus.PENDING) {
                    pendingAbsences += weight;
                    pendingDetails.add(detail);
                }
            }

            // Add the proposed request's absence weight to simulate approval
            double proposedWeight = request.isHalfDay() ? 0.50 : 1.00;
            double simulatedApprovedAbsences = approvedAbsences + proposedWeight;

            // Available staff after approving this request
            double simulatedAvailableStaff = Math.max(0.0, totalActive - simulatedApprovedAbsences);
            double simulatedProjectedStaff = Math.max(0.0, simulatedAvailableStaff - pendingAbsences);

            snapshot.setApprovedAbsences(simulatedApprovedAbsences);
            snapshot.setPendingAbsences(pendingAbsences);
            snapshot.setAvailableStaff(simulatedAvailableStaff);
            snapshot.setProjectedAvailableStaff(simulatedProjectedStaff);
            snapshot.setApprovedAbsenceDetails(approvedDetails);
            snapshot.setPendingAbsenceDetails(pendingDetails);

            double deficit = Math.max(0.0, requiredStaff - simulatedAvailableStaff);
            snapshot.setDeficit(deficit);

            if (deficit > 0) {
                snapshot.setSeverity(ConflictSeverity.CRITICAL_STAFFING_SHORTAGE);
                snapshot.setExplanation(String.format("Critical Staffing Shortage on %s: Approving this leave would drop available staff to %.1f, falling below minimum requirement of %d (Deficit: %.1f).",
                        current, simulatedAvailableStaff, requiredStaff, deficit));
                highestSeverity = ConflictSeverity.CRITICAL_STAFFING_SHORTAGE;
                hasShortage = true;
            } else if (simulatedProjectedStaff < requiredStaff) {
                snapshot.setSeverity(ConflictSeverity.WARNING_PENDING_RISK);
                snapshot.setExplanation(String.format("Pending Staffing Risk on %s: Available staff (%.1f) meets requirement (%d), but pending peer requests could cause a shortage (Projected: %.1f).",
                        current, simulatedAvailableStaff, requiredStaff, simulatedProjectedStaff));
                if (highestSeverity != ConflictSeverity.CRITICAL_STAFFING_SHORTAGE) {
                    highestSeverity = ConflictSeverity.WARNING_PENDING_RISK;
                }
            } else {
                snapshot.setSeverity(ConflictSeverity.NONE);
                snapshot.setExplanation(String.format("Adequate Coverage on %s: %.1f staff available after leave (Minimum required: %d).",
                        current, simulatedAvailableStaff, requiredStaff));
            }

            affectedDates.add(snapshot);
            current = current.plusDays(1);
        }

        report.setAffectedDates(affectedDates);
        report.setHasConflict(highestSeverity != ConflictSeverity.NONE);
        report.setSeverity(highestSeverity);

        if (highestSeverity == ConflictSeverity.CRITICAL_STAFFING_SHORTAGE) {
            if (strict) {
                report.setSummaryMessage("CRITICAL CONFLICT: Approving this request violates department staffing rules. Approval is strictly blocked.");
            } else {
                report.setSummaryMessage("CRITICAL CONFLICT WARNING: Approving this request causes a staffing deficit. Approval requires managerial override justification.");
            }
        } else if (highestSeverity == ConflictSeverity.WARNING_PENDING_RISK) {
            report.setSummaryMessage("STAFFING WARNING: Approving this request is safe based on approved leaves, but overlapping pending peer requests pose a potential shortage risk.");
        } else {
            report.setSummaryMessage("STAFFING OPTIMAL: Approval will not breach department minimum coverage requirements.");
        }

        return report;
    }
}

