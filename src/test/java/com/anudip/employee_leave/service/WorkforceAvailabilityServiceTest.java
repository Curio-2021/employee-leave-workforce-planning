package com.anudip.employee_leave.service;

import com.anudip.employee_leave.dto.DailyAvailabilitySnapshot;
import com.anudip.employee_leave.entity.ConflictSeverity;
import com.anudip.employee_leave.entity.Department;
import com.anudip.employee_leave.entity.DepartmentStaffingRule;
import com.anudip.employee_leave.entity.Employee;
import com.anudip.employee_leave.entity.EmploymentStatus;
import com.anudip.employee_leave.entity.EmploymentType;
import com.anudip.employee_leave.entity.Holiday;
import com.anudip.employee_leave.entity.HolidayType;
import com.anudip.employee_leave.entity.LeaveRequest;
import com.anudip.employee_leave.entity.LeaveRequestStatus;
import com.anudip.employee_leave.entity.LeaveType;
import com.anudip.employee_leave.repository.DepartmentRepository;
import com.anudip.employee_leave.repository.DepartmentStaffingRuleRepository;
import com.anudip.employee_leave.repository.EmployeeRepository;
import com.anudip.employee_leave.repository.HolidayRepository;
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
class WorkforceAvailabilityServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private DepartmentStaffingRuleRepository staffingRuleRepository;

    @Mock
    private WorkingCalendarService workingCalendarService;

    @Mock
    private HolidayRepository holidayRepository;

    @InjectMocks
    private WorkforceAvailabilityService availabilityService;

    private Department dept;
    private DepartmentStaffingRule rule;
    private List<Employee> activeEmployees;
    private LeaveType casualLeave;

    @BeforeEach
    void setUp() {
        dept = new Department("Operations", "Ops Dept");
        dept.setId(10L);

        // Required staff: 2 minimum, or 50%
        rule = new DepartmentStaffingRule(dept, 2, 50, true, false, true, "Ops rule");

        casualLeave = new LeaveType("Casual Leave", "CL", "Casual", true, true, 5, false);
        casualLeave.setId(1L);

        activeEmployees = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            Employee e = new Employee("EMP0" + i, "Staff " + i, "emp" + i + "@example.com", dept,
                    EmploymentType.FULL_TIME, LocalDate.of(2025, 1, 1), EmploymentStatus.ACTIVE, null);
            e.setId((long) i);
            activeEmployees.add(e);
        }
    }

    @Test
    void testGetDepartmentDailyAvailability_OptimalStaffing() {
        LocalDate date = LocalDate.of(2026, 10, 21);

        when(departmentRepository.findById(10L)).thenReturn(Optional.of(dept));
        when(workingCalendarService.isHoliday(date)).thenReturn(false);
        when(workingCalendarService.isStandardWeekend(date)).thenReturn(false);
        when(workingCalendarService.isWorkingDayForDepartment(10L, date)).thenReturn(true);
        when(staffingRuleRepository.findByDepartmentId(10L)).thenReturn(Optional.of(rule));
        when(employeeRepository.findFiltered(10L, EmploymentStatus.ACTIVE)).thenReturn(activeEmployees);
        when(leaveRequestRepository.findDepartmentLeavesInDateRange(eq(10L), eq(date), eq(date), any()))
                .thenReturn(Collections.emptyList());

        DailyAvailabilitySnapshot snapshot = availabilityService.getDepartmentDailyAvailability(10L, date);

        assertNotNull(snapshot);
        assertTrue(snapshot.isWorkingDay());
        assertEquals(4, snapshot.getTotalActiveStaff());
        assertEquals(2, snapshot.getRequiredStaff()); // max(2, ceil(4 * 0.50)) = 2
        assertEquals(4.0, snapshot.getAvailableStaff());
        assertEquals(0.0, snapshot.getDeficit());
        assertEquals(ConflictSeverity.NONE, snapshot.getSeverity());
    }

    @Test
    void testGetDepartmentDailyAvailability_HalfDayDeduction() {
        LocalDate date = LocalDate.of(2026, 10, 21);

        LeaveRequest halfDayLeave = new LeaveRequest("LR-2026-H1", activeEmployees.get(0), casualLeave,
                date, date, BigDecimal.valueOf(0.50), true, "Doctor appt", null, "AUTO");
        halfDayLeave.setId(101L);
        halfDayLeave.setStatus(LeaveRequestStatus.APPROVED);

        when(departmentRepository.findById(10L)).thenReturn(Optional.of(dept));
        when(workingCalendarService.isHoliday(date)).thenReturn(false);
        when(workingCalendarService.isStandardWeekend(date)).thenReturn(false);
        when(workingCalendarService.isWorkingDayForDepartment(10L, date)).thenReturn(true);
        when(staffingRuleRepository.findByDepartmentId(10L)).thenReturn(Optional.of(rule));
        when(employeeRepository.findFiltered(10L, EmploymentStatus.ACTIVE)).thenReturn(activeEmployees);
        when(leaveRequestRepository.findDepartmentLeavesInDateRange(eq(10L), eq(date), eq(date), any()))
                .thenReturn(List.of(halfDayLeave));

        DailyAvailabilitySnapshot snapshot = availabilityService.getDepartmentDailyAvailability(10L, date);

        assertEquals(4, snapshot.getTotalActiveStaff());
        assertEquals(0.5, snapshot.getApprovedAbsences());
        assertEquals(3.5, snapshot.getAvailableStaff());
        assertEquals(ConflictSeverity.NONE, snapshot.getSeverity());
    }

    @Test
    void testGetDepartmentDailyAvailability_CriticalShortage() {
        LocalDate date = LocalDate.of(2026, 10, 21);

        List<LeaveRequest> approvedLeaves = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            LeaveRequest lr = new LeaveRequest("LR-" + i, activeEmployees.get(i), casualLeave,
                    date, date, BigDecimal.valueOf(1.00), false, "Leave " + i, null, "AUTO");
            lr.setId((long) (200 + i));
            lr.setStatus(LeaveRequestStatus.APPROVED);
            approvedLeaves.add(lr);
        }

        when(departmentRepository.findById(10L)).thenReturn(Optional.of(dept));
        when(workingCalendarService.isHoliday(date)).thenReturn(false);
        when(workingCalendarService.isStandardWeekend(date)).thenReturn(false);
        when(workingCalendarService.isWorkingDayForDepartment(10L, date)).thenReturn(true);
        when(staffingRuleRepository.findByDepartmentId(10L)).thenReturn(Optional.of(rule));
        when(employeeRepository.findFiltered(10L, EmploymentStatus.ACTIVE)).thenReturn(activeEmployees);
        when(leaveRequestRepository.findDepartmentLeavesInDateRange(eq(10L), eq(date), eq(date), any()))
                .thenReturn(approvedLeaves);

        DailyAvailabilitySnapshot snapshot = availabilityService.getDepartmentDailyAvailability(10L, date);

        assertEquals(4, snapshot.getTotalActiveStaff());
        assertEquals(3.0, snapshot.getApprovedAbsences());
        assertEquals(1.0, snapshot.getAvailableStaff());
        assertEquals(2, snapshot.getRequiredStaff());
        assertEquals(1.0, snapshot.getDeficit());
        assertEquals(ConflictSeverity.CRITICAL_STAFFING_SHORTAGE, snapshot.getSeverity());
    }

    @Test
    void testGetDepartmentDailyAvailability_PendingRisk() {
        LocalDate date = LocalDate.of(2026, 10, 21);

        LeaveRequest approved1 = new LeaveRequest("LR-A1", activeEmployees.get(0), casualLeave,
                date, date, BigDecimal.valueOf(1.00), false, "Off", null, "AUTO");
        approved1.setId(301L);
        approved1.setStatus(LeaveRequestStatus.APPROVED);

        LeaveRequest approved2 = new LeaveRequest("LR-A2", activeEmployees.get(1), casualLeave,
                date, date, BigDecimal.valueOf(1.00), false, "Off", null, "AUTO");
        approved2.setId(302L);
        approved2.setStatus(LeaveRequestStatus.APPROVED);

        LeaveRequest pending = new LeaveRequest("LR-P1", activeEmployees.get(2), casualLeave,
                date, date, BigDecimal.valueOf(1.00), false, "Pending", null, "AUTO");
        pending.setId(303L);
        pending.setStatus(LeaveRequestStatus.PENDING);

        when(departmentRepository.findById(10L)).thenReturn(Optional.of(dept));
        when(workingCalendarService.isHoliday(date)).thenReturn(false);
        when(workingCalendarService.isStandardWeekend(date)).thenReturn(false);
        when(workingCalendarService.isWorkingDayForDepartment(10L, date)).thenReturn(true);
        when(staffingRuleRepository.findByDepartmentId(10L)).thenReturn(Optional.of(rule));
        when(employeeRepository.findFiltered(10L, EmploymentStatus.ACTIVE)).thenReturn(activeEmployees);
        when(leaveRequestRepository.findDepartmentLeavesInDateRange(eq(10L), eq(date), eq(date), any()))
                .thenReturn(List.of(approved1, approved2, pending));

        DailyAvailabilitySnapshot snapshot = availabilityService.getDepartmentDailyAvailability(10L, date);

        assertEquals(2.0, snapshot.getAvailableStaff());
        assertEquals(1.0, snapshot.getProjectedAvailableStaff());
        assertEquals(2, snapshot.getRequiredStaff());
        assertEquals(0.0, snapshot.getDeficit());
        assertEquals(ConflictSeverity.WARNING_PENDING_RISK, snapshot.getSeverity());
    }

    @Test
    void testGetDepartmentDailyAvailability_WeekendNonWorkingDay() {
        LocalDate date = LocalDate.of(2026, 10, 24);

        when(departmentRepository.findById(10L)).thenReturn(Optional.of(dept));
        when(workingCalendarService.isHoliday(date)).thenReturn(false);
        when(workingCalendarService.isStandardWeekend(date)).thenReturn(true);
        when(workingCalendarService.isWorkingDayForDepartment(10L, date)).thenReturn(false);
        when(staffingRuleRepository.findByDepartmentId(10L)).thenReturn(Optional.of(rule));
        when(employeeRepository.findFiltered(10L, EmploymentStatus.ACTIVE)).thenReturn(activeEmployees);
        when(leaveRequestRepository.findDepartmentLeavesInDateRange(eq(10L), eq(date), eq(date), any()))
                .thenReturn(Collections.emptyList());

        DailyAvailabilitySnapshot snapshot = availabilityService.getDepartmentDailyAvailability(10L, date);

        assertFalse(snapshot.isWorkingDay());
        assertEquals(0.0, snapshot.getDeficit());
        assertEquals(ConflictSeverity.NONE, snapshot.getSeverity());
    }
}
