package com.anudip.employee_leave.service;

import com.anudip.employee_leave.dto.DailyAvailabilitySnapshot;
import com.anudip.employee_leave.dto.DashboardSummaryDTO;
import com.anudip.employee_leave.entity.Department;
import com.anudip.employee_leave.entity.Employee;
import com.anudip.employee_leave.entity.EmploymentStatus;
import com.anudip.employee_leave.entity.LeaveRequest;
import com.anudip.employee_leave.entity.LeaveRequestStatus;
import com.anudip.employee_leave.entity.LeaveType;
import com.anudip.employee_leave.repository.DepartmentRepository;
import com.anudip.employee_leave.repository.EmployeeRepository;
import com.anudip.employee_leave.repository.LeaveRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkforceDashboardServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private WorkforceAvailabilityService workforceAvailabilityService;

    @Mock
    private PolicyExceptionService policyExceptionService;

    @Mock
    private AuditService auditService;

    private WorkforceDashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new WorkforceDashboardService(
                employeeRepository,
                departmentRepository,
                leaveRequestRepository,
                workforceAvailabilityService,
                policyExceptionService,
                auditService
        );
    }

    @Test
    void testGetDashboardSummary_ComputesMetricsAndOverlappingLeaves() {
        LocalDate refDate = LocalDate.of(2026, 10, 20);

        when(employeeRepository.count()).thenReturn(15L);
        when(employeeRepository.countByEmploymentStatus(EmploymentStatus.ACTIVE)).thenReturn(12L);
        when(departmentRepository.count()).thenReturn(2L);
        when(leaveRequestRepository.countByStatus(LeaveRequestStatus.PENDING)).thenReturn(3L);
        when(leaveRequestRepository.countByStatus(LeaveRequestStatus.APPROVED)).thenReturn(10L);
        when(leaveRequestRepository.countByStatus(LeaveRequestStatus.REJECTED)).thenReturn(2L);

        Department dept1 = new Department();
        dept1.setId(1L);
        dept1.setName("Engineering");

        Department dept2 = new Department();
        dept2.setId(2L);
        dept2.setName("Support");

        when(departmentRepository.findAllByOrderByNameAsc()).thenReturn(List.of(dept1, dept2));

        // Dept 1 snapshot: optimal
        DailyAvailabilitySnapshot snap1 = new DailyAvailabilitySnapshot();
        snap1.setTotalActiveStaff(8);
        snap1.setRequiredStaff(4);
        snap1.setApprovedAbsences(1.0);
        snap1.setAvailableStaff(7.0);
        snap1.setDeficit(0.0);
        when(workforceAvailabilityService.getDepartmentDailyAvailability(1L, refDate)).thenReturn(snap1);

        // Dept 2 snapshot: shortage
        DailyAvailabilitySnapshot snap2 = new DailyAvailabilitySnapshot();
        snap2.setTotalActiveStaff(4);
        snap2.setRequiredStaff(3);
        snap2.setApprovedAbsences(2.0);
        snap2.setAvailableStaff(2.0);
        snap2.setDeficit(1.0);
        when(workforceAvailabilityService.getDepartmentDailyAvailability(2L, refDate)).thenReturn(snap2);

        // Upcoming approved leaves overlapping [2026-10-20, 2026-11-03]
        Employee emp = new Employee();
        emp.setId(10L);
        emp.setFullName("David Miller");
        emp.setEmployeeId("EMP010");
        emp.setDepartment(dept1);

        LeaveType lt = new LeaveType();
        lt.setName("Earned Leave");

        LeaveRequest upcomingReq = new LeaveRequest();
        upcomingReq.setId(100L);
        upcomingReq.setRequestNumber("LR-2026-0099");
        upcomingReq.setEmployee(emp);
        upcomingReq.setLeaveType(lt);
        upcomingReq.setStartDate(LocalDate.of(2026, 10, 22));
        upcomingReq.setEndDate(LocalDate.of(2026, 10, 24));
        upcomingReq.setDuration(new java.math.BigDecimal("3.00"));
        upcomingReq.setStatus(LeaveRequestStatus.APPROVED);

        when(leaveRequestRepository.findLeavesInDateRangeWithDetails(eq(refDate), eq(refDate.plusDays(14)), eq(LeaveRequestStatus.APPROVED)))
                .thenReturn(List.of(upcomingReq));

        when(policyExceptionService.getExceptionCounts()).thenReturn(Map.of("TOTAL", 4L));

        DashboardSummaryDTO summary = dashboardService.getDashboardSummary(refDate);

        assertThat(summary.getTotalEmployees()).isEqualTo(15L);
        assertThat(summary.getActiveEmployees()).isEqualTo(12L);
        assertThat(summary.getTotalDepartments()).isEqualTo(2L);
        assertThat(summary.getPendingRequestsCount()).isEqualTo(3L);
        assertThat(summary.getEmployeesOnLeaveToday()).isEqualTo(3.0);
        assertThat(summary.getDepartmentsWithShortageCount()).isEqualTo(1);

        assertThat(summary.getDepartmentStats()).hasSize(2);
        assertThat(summary.getDepartmentStats().get(0).getDepartmentName()).isEqualTo("Engineering");
        assertThat(summary.getDepartmentStats().get(0).isShortageToday()).isFalse();
        assertThat(summary.getDepartmentStats().get(1).getDepartmentName()).isEqualTo("Support");
        assertThat(summary.getDepartmentStats().get(1).isShortageToday()).isTrue();

        assertThat(summary.getUpcomingLeaves()).hasSize(1);
        assertThat(summary.getUpcomingLeaves().get(0).getRequestNumber()).isEqualTo("LR-2026-0099");
        assertThat(summary.getUpcomingLeaves().get(0).getEmployeeName()).isEqualTo("David Miller");
        assertThat(summary.getExceptionCounts()).containsEntry("TOTAL", 4L);
    }
}
