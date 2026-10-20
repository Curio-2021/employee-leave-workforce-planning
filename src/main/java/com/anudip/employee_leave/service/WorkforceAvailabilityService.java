package com.anudip.employee_leave.service;

import com.anudip.employee_leave.dto.DailyAvailabilitySnapshot;
import com.anudip.employee_leave.dto.EmployeeAbsenceDetail;
import com.anudip.employee_leave.entity.ConflictSeverity;
import com.anudip.employee_leave.entity.Department;
import com.anudip.employee_leave.entity.DepartmentStaffingRule;
import com.anudip.employee_leave.entity.Employee;
import com.anudip.employee_leave.entity.EmploymentStatus;
import com.anudip.employee_leave.entity.Holiday;
import com.anudip.employee_leave.entity.LeaveRequest;
import com.anudip.employee_leave.entity.LeaveRequestStatus;
import com.anudip.employee_leave.exception.ResourceNotFoundException;
import com.anudip.employee_leave.repository.DepartmentRepository;
import com.anudip.employee_leave.repository.DepartmentStaffingRuleRepository;
import com.anudip.employee_leave.repository.EmployeeRepository;
import com.anudip.employee_leave.repository.HolidayRepository;
import com.anudip.employee_leave.repository.LeaveRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class WorkforceAvailabilityService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final DepartmentStaffingRuleRepository staffingRuleRepository;
    private final WorkingCalendarService workingCalendarService;
    private final HolidayRepository holidayRepository;

    public WorkforceAvailabilityService(DepartmentRepository departmentRepository,
                                        EmployeeRepository employeeRepository,
                                        LeaveRequestRepository leaveRequestRepository,
                                        DepartmentStaffingRuleRepository staffingRuleRepository,
                                        WorkingCalendarService workingCalendarService,
                                        HolidayRepository holidayRepository) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.staffingRuleRepository = staffingRuleRepository;
        this.workingCalendarService = workingCalendarService;
        this.holidayRepository = holidayRepository;
    }

    /**
     * Calculates the daily availability snapshot for a specific department and date.
     */
    @Transactional(readOnly = true)
    public DailyAvailabilitySnapshot getDepartmentDailyAvailability(Long departmentId, LocalDate date) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + departmentId));

        DailyAvailabilitySnapshot snapshot = new DailyAvailabilitySnapshot();
        snapshot.setDate(date);

        boolean isHoliday = workingCalendarService.isHoliday(date);
        snapshot.setHoliday(isHoliday);
        if (isHoliday) {
            List<Holiday> holidays = holidayRepository.findAll();
            for (Holiday h : holidays) {
                if (h.isActive() && h.getHolidayDate().equals(date)) {
                    snapshot.setHolidayName(h.getName());
                    break;
                }
            }
        }

        boolean isWeekend = workingCalendarService.isStandardWeekend(date);
        snapshot.setWeekend(isWeekend);

        boolean isWorkingDay = workingCalendarService.isWorkingDayForDepartment(departmentId, date);
        snapshot.setWorkingDay(isWorkingDay);

        // Fetch active employees belonging to department who joined on or before this date
        List<Employee> departmentEmployees = employeeRepository.findFiltered(departmentId, EmploymentStatus.ACTIVE);
        List<Employee> eligibleStaff = departmentEmployees.stream()
                .filter(e -> e.getJoiningDate() == null || !e.getJoiningDate().isAfter(date))
                .collect(Collectors.toList());

        int totalActiveStaff = eligibleStaff.size();
        snapshot.setTotalActiveStaff(totalActiveStaff);

        // Resolve staffing rule
        Optional<DepartmentStaffingRule> ruleOpt = staffingRuleRepository.findByDepartmentId(departmentId)
                .filter(DepartmentStaffingRule::isActive);

        int requiredStaff;
        if (!isWorkingDay) {
            requiredStaff = 0; // No staff required on holidays or non-working days
        } else if (ruleOpt.isPresent()) {
            requiredStaff = ruleOpt.get().calculateRequiredStaff(totalActiveStaff);
        } else {
            // Default fallback: require at least 1 staff member if department has employees
            requiredStaff = totalActiveStaff > 0 ? 1 : 0;
        }
        snapshot.setRequiredStaff(requiredStaff);

        // Query leaves overlapping this date
        List<LeaveRequest> overlappingLeaves = leaveRequestRepository.findDepartmentLeavesInDateRange(
                departmentId, date, date, List.of(LeaveRequestStatus.APPROVED, LeaveRequestStatus.PENDING));

        double approvedAbsences = 0.0;
        double pendingAbsences = 0.0;
        List<EmployeeAbsenceDetail> approvedDetails = new ArrayList<>();
        List<EmployeeAbsenceDetail> pendingDetails = new ArrayList<>();

        for (LeaveRequest req : overlappingLeaves) {
            double weight = req.isHalfDay() ? 0.50 : 1.00;
            EmployeeAbsenceDetail detail = new EmployeeAbsenceDetail(
                    req.getId(),
                    req.getEmployee().getId(),
                    req.getEmployee().getFullName(),
                    req.getEmployee().getEmployeeId(),
                    req.getLeaveType().getName(),
                    req.getRequestNumber(),
                    req.getStatus().name(),
                    req.isHalfDay(),
                    weight
            );

            if (req.getStatus() == LeaveRequestStatus.APPROVED) {
                approvedAbsences += weight;
                approvedDetails.add(detail);
            } else if (req.getStatus() == LeaveRequestStatus.PENDING) {
                pendingAbsences += weight;
                pendingDetails.add(detail);
            }
        }

        snapshot.setApprovedAbsences(approvedAbsences);
        snapshot.setPendingAbsences(pendingAbsences);
        snapshot.setApprovedAbsenceDetails(approvedDetails);
        snapshot.setPendingAbsenceDetails(pendingDetails);

        double availableStaff = Math.max(0.0, totalActiveStaff - approvedAbsences);
        double projectedAvailableStaff = Math.max(0.0, availableStaff - pendingAbsences);
        snapshot.setAvailableStaff(availableStaff);
        snapshot.setProjectedAvailableStaff(projectedAvailableStaff);

        if (!isWorkingDay) {
            snapshot.setSeverity(ConflictSeverity.NONE);
            snapshot.setDeficit(0.0);
            snapshot.setExplanation(isHoliday ? "Company Holiday: " + snapshot.getHolidayName() : "Weekend / Scheduled Non-Working Day");
        } else {
            double deficit = Math.max(0.0, requiredStaff - availableStaff);
            snapshot.setDeficit(deficit);

            if (deficit > 0) {
                snapshot.setSeverity(ConflictSeverity.CRITICAL_STAFFING_SHORTAGE);
                snapshot.setExplanation(String.format("Critical Staffing Shortage: Available staff (%.1f) is below required minimum (%d). Deficit: %.1f.",
                        availableStaff, requiredStaff, deficit));
            } else if (projectedAvailableStaff < requiredStaff) {
                snapshot.setSeverity(ConflictSeverity.WARNING_PENDING_RISK);
                snapshot.setExplanation(String.format("Pending Leave Risk: Available staff (%.1f) meets requirement (%d), but pending requests would drop coverage to %.1f.",
                        availableStaff, requiredStaff, projectedAvailableStaff));
            } else {
                snapshot.setSeverity(ConflictSeverity.NONE);
                snapshot.setExplanation(String.format("Optimal Staffing: %.1f available staff exceeds required threshold (%d).",
                        availableStaff, requiredStaff));
            }
        }

        return snapshot;
    }

    /**
     * Generates a timeline of daily availability snapshots for a date range.
     */
    @Transactional(readOnly = true)
    public List<DailyAvailabilitySnapshot> getDepartmentAvailabilityTimeline(Long departmentId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Invalid date range for availability timeline.");
        }

        List<DailyAvailabilitySnapshot> timeline = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            timeline.add(getDepartmentDailyAvailability(departmentId, current));
            current = current.plusDays(1);
        }
        return timeline;
    }
}

