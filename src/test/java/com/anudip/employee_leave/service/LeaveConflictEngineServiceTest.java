package com.anudip.employee_leave.service;

import com.anudip.employee_leave.dto.WorkforceConflictReport;
import com.anudip.employee_leave.entity.ConflictSeverity;
import com.anudip.employee_leave.entity.Department;
import com.anudip.employee_leave.entity.DepartmentStaffingRule;
import com.anudip.employee_leave.entity.Employee;
import com.anudip.employee_leave.entity.EmploymentStatus;
import com.anudip.employee_leave.entity.EmploymentType;
import com.anudip.employee_leave.entity.LeaveRequest;
import com.anudip.employee_leave.entity.LeaveRequestStatus;
import com.anudip.employee_leave.entity.LeaveType;
import com.anudip.employee_leave.repository.DepartmentStaffingRuleRepository;
import com.anudip.employee_leave.repository.EmployeeRepository;
import com.anudip.employee_leave.repository.LeaveRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveConflictEngineServiceTest {

    @Mock
    private DepartmentStaffingRuleRepository staffingRuleRepository;

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private WorkingCalendarService workingCalendarService;

    @InjectMocks
    private LeaveConflictEngineService conflictEngineService;

    private Department dept;
    private DepartmentStaffingRule rule;
    private List<Employee> activeStaff;
    private LeaveType casualLeave;

    @BeforeEach
    void setUp() {
        dept = new Department("IT Support", "Support Desk");
        dept.setId(5L);

        // Required staff: 2, strict blocking = true
        rule = new DepartmentStaffingRule(dept, 2, null, true, false, true, "IT rule");

        casualLeave = new LeaveType("Casual", "CL", "Casual", true, true, 5, false);
        casualLeave.setId(1L);

        activeStaff = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Employee e = new Employee("EMP" + i, "Tech " + i, "tech" + i + "@example.com", dept,
                    EmploymentType.FULL_TIME, LocalDate.of(2025, 1, 1), EmploymentStatus.ACTIVE, null);
            e.setId((long) i);
            activeStaff.add(e);
        }
    }

    @Test
    void testEvaluateConflicts_ProposedLeaveCausesShortage() {
        LocalDate date = LocalDate.of(2026, 10, 21);

        // Tech 1 already has approved leave (1 off -> 2 available)
        LeaveRequest existingApproved = new LeaveRequest("LR-EXISTING", activeStaff.get(0), casualLeave,
                date, date, BigDecimal.valueOf(1.00), false, "Existing leave", null, "AUTO");
        existingApproved.setId(10L);
        existingApproved.setStatus(LeaveRequestStatus.APPROVED);

        // Tech 2 proposes leave (would drop simulated available to 1.0, required is 2)
        LeaveRequest proposed = new LeaveRequest("LR-PROPOSED", activeStaff.get(1), casualLeave,
                date, date, BigDecimal.valueOf(1.00), false, "Proposed leave", null, "AUTO");
        proposed.setId(11L);
        proposed.setStatus(LeaveRequestStatus.PENDING);

        when(staffingRuleRepository.findByDepartmentId(5L)).thenReturn(Optional.of(rule));
        when(workingCalendarService.isWorkingDayForDepartment(5L, date)).thenReturn(true);
        when(workingCalendarService.isHoliday(date)).thenReturn(false);
        when(workingCalendarService.isStandardWeekend(date)).thenReturn(false);
        when(employeeRepository.findFiltered(5L, EmploymentStatus.ACTIVE)).thenReturn(activeStaff);

        when(leaveRequestRepository.findDepartmentLeavesInDateRange(eq(5L), eq(date), eq(date), any()))
                .thenReturn(List.of(existingApproved, proposed));

        WorkforceConflictReport report = conflictEngineService.evaluateLeaveRequestConflicts(proposed);

        assertNotNull(report);
        assertTrue(report.isHasConflict());
        assertEquals(ConflictSeverity.CRITICAL_STAFFING_SHORTAGE, report.getSeverity());
        assertEquals(2, report.getCalculatedRequiredStaff());
        assertTrue(report.isBlockingEnabled());
    }

    @Test
    void testEvaluateConflicts_AntiDoubleCountingGuard() {
        LocalDate date = LocalDate.of(2026, 10, 21);

        LeaveRequest evaluatedRequest = new LeaveRequest("LR-SAME", activeStaff.get(0), casualLeave,
                date, date, BigDecimal.valueOf(1.00), false, "Same leave", null, "AUTO");
        evaluatedRequest.setId(99L);
        evaluatedRequest.setStatus(LeaveRequestStatus.APPROVED);

        when(staffingRuleRepository.findByDepartmentId(5L)).thenReturn(Optional.of(rule));
        when(workingCalendarService.isWorkingDayForDepartment(5L, date)).thenReturn(true);
        when(workingCalendarService.isHoliday(date)).thenReturn(false);
        when(workingCalendarService.isStandardWeekend(date)).thenReturn(false);
        when(employeeRepository.findFiltered(5L, EmploymentStatus.ACTIVE)).thenReturn(activeStaff);

        // Repo returns evaluatedRequest in the list
        when(leaveRequestRepository.findDepartmentLeavesInDateRange(eq(5L), eq(date), eq(date), any()))
                .thenReturn(List.of(evaluatedRequest));

        WorkforceConflictReport report = conflictEngineService.evaluateLeaveRequestConflicts(evaluatedRequest);

        assertNotNull(report);
        // Total active: 3. Evaluated request weight: 1.0. Other absences: 0.0. Simulated available: 2.0. Required: 2.
        // No conflict!
        assertFalse(report.isHasConflict());
        assertEquals(ConflictSeverity.NONE, report.getSeverity());
    }

    @Test
    void testEvaluateConflicts_NonWorkingDay_NoConflict() {
        LocalDate date = LocalDate.of(2026, 10, 25);

        LeaveRequest weekendRequest = new LeaveRequest("LR-WEEKEND", activeStaff.get(0), casualLeave,
                date, date, BigDecimal.valueOf(1.00), false, "Weekend leave", null, "AUTO");
        weekendRequest.setId(77L);
        weekendRequest.setStatus(LeaveRequestStatus.PENDING);

        when(staffingRuleRepository.findByDepartmentId(5L)).thenReturn(Optional.of(rule));
        when(workingCalendarService.isWorkingDayForDepartment(5L, date)).thenReturn(false);
        when(workingCalendarService.isHoliday(date)).thenReturn(false);
        when(workingCalendarService.isStandardWeekend(date)).thenReturn(true);

        WorkforceConflictReport report = conflictEngineService.evaluateLeaveRequestConflicts(weekendRequest);

        assertNotNull(report);
        assertFalse(report.isHasConflict());
        assertEquals(ConflictSeverity.NONE, report.getSeverity());
    }
}

