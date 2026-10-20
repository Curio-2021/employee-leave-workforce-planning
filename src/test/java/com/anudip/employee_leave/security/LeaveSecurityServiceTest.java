package com.anudip.employee_leave.security;

import com.anudip.employee_leave.entity.Department;
import com.anudip.employee_leave.entity.Employee;
import com.anudip.employee_leave.entity.EmploymentStatus;
import com.anudip.employee_leave.entity.EmploymentType;
import com.anudip.employee_leave.entity.LeaveRequest;
import com.anudip.employee_leave.entity.LeaveType;
import com.anudip.employee_leave.entity.Role;
import com.anudip.employee_leave.entity.UserAccount;
import com.anudip.employee_leave.repository.EmployeeRepository;
import com.anudip.employee_leave.repository.LeaveRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveSecurityServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private LeaveSecurityService leaveSecurityService;

    private Department department;
    private Employee managerEmp;
    private Employee reportEmp;
    private Employee otherEmp;

    private CustomUserDetails hrAdminUser;
    private CustomUserDetails managerUser;
    private CustomUserDetails reportUser;
    private CustomUserDetails otherUser;

    private LeaveType leaveType;
    private LeaveRequest reportRequest;
    private LeaveRequest managerRequest;

    @BeforeEach
    void setUp() {
        department = new Department("Engineering", "ENG");
        department.setId(10L);

        managerEmp = new Employee("EMP001", "Manager Mike", "mike@example.com", department,
                EmploymentType.FULL_TIME, LocalDate.of(2020, 1, 1), EmploymentStatus.ACTIVE, null);
        managerEmp.setId(1L);

        reportEmp = new Employee("EMP002", "Developer Dave", "dave@example.com", department,
                EmploymentType.FULL_TIME, LocalDate.of(2022, 1, 1), EmploymentStatus.ACTIVE, managerEmp);
        reportEmp.setId(2L);

        otherEmp = new Employee("EMP003", "Other Oliver", "oliver@example.com", department,
                EmploymentType.FULL_TIME, LocalDate.of(2023, 1, 1), EmploymentStatus.ACTIVE, null);
        otherEmp.setId(3L);

        UserAccount hrAccount = new UserAccount("admin", "hash", Role.ROLE_HR_ADMIN, null);
        hrAdminUser = new CustomUserDetails(hrAccount);

        UserAccount mgrAccount = new UserAccount("mike", "hash", Role.ROLE_MANAGER, managerEmp);
        managerUser = new CustomUserDetails(mgrAccount);

        UserAccount repAccount = new UserAccount("dave", "hash", Role.ROLE_EMPLOYEE, reportEmp);
        reportUser = new CustomUserDetails(repAccount);

        UserAccount othAccount = new UserAccount("oliver", "hash", Role.ROLE_EMPLOYEE, otherEmp);
        otherUser = new CustomUserDetails(othAccount);

        leaveType = new LeaveType("Casual Leave", "CL", "Casual Leave", true, true, 12, true);
        leaveType.setId(5L);

        reportRequest = new LeaveRequest("LR-001", reportEmp, leaveType,
                LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 4),
                BigDecimal.valueOf(3.0), false, "Personal", managerEmp, "MANAGER_APPROVAL");
        reportRequest.setId(100L);

        managerRequest = new LeaveRequest("LR-002", managerEmp, leaveType,
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 12),
                BigDecimal.valueOf(3.0), false, "Vacation", null, "HR_APPROVAL");
        managerRequest.setId(101L);
    }

    @Test
    @DisplayName("HR Admin has universal access to view, approve, cancel requests and view employee data")
    void hrAdmin_UniversalAccess() {
        assertThat(leaveSecurityService.canAccessRequest(100L, hrAdminUser)).isTrue();
        assertThat(leaveSecurityService.canApproveRequest(100L, hrAdminUser)).isTrue();
        assertThat(leaveSecurityService.canCancelRequest(100L, hrAdminUser)).isTrue();
        assertThat(leaveSecurityService.canAccessEmployeeData(2L, hrAdminUser)).isTrue();
    }

    @Test
    @DisplayName("Employee can view and cancel own leave request, but cannot view other's request or approve")
    void employee_OwnRequestBoundaries() {
        when(leaveRequestRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(reportRequest));

        // Own request
        assertThat(leaveSecurityService.canAccessRequest(100L, reportUser)).isTrue();
        assertThat(leaveSecurityService.canCancelRequest(100L, reportUser)).isTrue();
        assertThat(leaveSecurityService.canApproveRequest(100L, reportUser)).isFalse();

        // Other employee accessing report's request
        assertThat(leaveSecurityService.canAccessRequest(100L, otherUser)).isFalse();
        assertThat(leaveSecurityService.canCancelRequest(100L, otherUser)).isFalse();
    }

    @Test
    @DisplayName("Manager can view and approve direct report's leave request")
    void manager_ApproveDirectReport() {
        when(leaveRequestRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(reportRequest));

        assertThat(leaveSecurityService.canAccessRequest(100L, managerUser)).isTrue();
        assertThat(leaveSecurityService.canApproveRequest(100L, managerUser)).isTrue();
    }

    @Test
    @DisplayName("Manager CANNOT approve their own leave request (Separation of Duties)")
    void manager_CannotApproveOwnRequest() {
        when(leaveRequestRepository.findByIdWithDetails(101L)).thenReturn(Optional.of(managerRequest));

        assertThat(leaveSecurityService.canAccessRequest(101L, managerUser)).isTrue();
        assertThat(leaveSecurityService.canApproveRequest(101L, managerUser)).isFalse();
    }

    @Test
    @DisplayName("Manager cannot approve request for employee who is not their direct report")
    void manager_CannotApproveNonReport() {
        LeaveRequest otherRequest = new LeaveRequest("LR-003", otherEmp, leaveType,
                LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 4),
                BigDecimal.valueOf(3.0), false, "Test", null, "MANAGER_APPROVAL");
        otherRequest.setId(102L);

        when(leaveRequestRepository.findByIdWithDetails(102L)).thenReturn(Optional.of(otherRequest));

        assertThat(leaveSecurityService.canAccessRequest(102L, managerUser)).isFalse();
        assertThat(leaveSecurityService.canApproveRequest(102L, managerUser)).isFalse();
    }

    @Test
    @DisplayName("Manager can access employee data for direct reports but not other employees")
    void manager_AccessEmployeeData() {
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(reportEmp));
        when(employeeRepository.findById(3L)).thenReturn(Optional.of(otherEmp));

        assertThat(leaveSecurityService.canAccessEmployeeData(2L, managerUser)).isTrue();
        assertThat(leaveSecurityService.canAccessEmployeeData(3L, managerUser)).isFalse();
    }
}
