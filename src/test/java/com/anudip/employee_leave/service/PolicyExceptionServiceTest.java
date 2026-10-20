package com.anudip.employee_leave.service;

import com.anudip.employee_leave.dto.PolicyExceptionItem;
import com.anudip.employee_leave.dto.WorkforceConflictReport;
import com.anudip.employee_leave.entity.Department;
import com.anudip.employee_leave.entity.Employee;
import com.anudip.employee_leave.entity.LeaveRequest;
import com.anudip.employee_leave.entity.LeaveRequestStatus;
import com.anudip.employee_leave.entity.LeaveTransaction;
import com.anudip.employee_leave.entity.LeaveTransactionType;
import com.anudip.employee_leave.entity.LeaveType;
import com.anudip.employee_leave.repository.LeaveRequestRepository;
import com.anudip.employee_leave.repository.LeaveTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyExceptionServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private LeaveTransactionRepository leaveTransactionRepository;

    @Mock
    private LeaveConflictEngineService leaveConflictEngineService;

    private PolicyExceptionService policyExceptionService;

    private Department dept;
    private Employee emp;
    private LeaveType leaveType;

    @BeforeEach
    void setUp() {
        policyExceptionService = new PolicyExceptionService(
                leaveRequestRepository, leaveTransactionRepository, leaveConflictEngineService);

        dept = new Department();
        dept.setId(10L);
        dept.setName("Operations");

        emp = new Employee();
        emp.setId(100L);
        emp.setEmployeeId("EMP100");
        emp.setFullName("Bob Marley");
        emp.setDepartment(dept);

        leaveType = new LeaveType();
        leaveType.setId(5L);
        leaveType.setName("Casual Leave");
    }

    @Test
    void testGetPolicyExceptions_AggregatesOverridesRisksAndAdjustments() {
        // 1. Authorized Override request
        LeaveRequest overrideReq = new LeaveRequest();
        overrideReq.setId(1L);
        overrideReq.setRequestNumber("LR-2026-0001");
        overrideReq.setEmployee(emp);
        overrideReq.setLeaveType(leaveType);
        overrideReq.setStartDate(LocalDate.of(2026, 11, 2));
        overrideReq.setEndDate(LocalDate.of(2026, 11, 4));
        overrideReq.setDuration(new java.math.BigDecimal("3.00"));
        overrideReq.setStatus(LeaveRequestStatus.APPROVED);
        overrideReq.setStaffingOverrideJustification("Cover arranged with external vendor");
        overrideReq.setSubmissionTimestamp(LocalDateTime.now().minusDays(2));
        overrideReq.setDecisionTimestamp(LocalDateTime.now().minusDays(1));

        when(leaveRequestRepository.findStaffingOverridesWithDetails()).thenReturn(List.of(overrideReq));

        // 2. Pending request with conflict risk
        LeaveRequest pendingReq = new LeaveRequest();
        pendingReq.setId(2L);
        pendingReq.setRequestNumber("LR-2026-0002");
        pendingReq.setEmployee(emp);
        pendingReq.setLeaveType(leaveType);
        pendingReq.setStartDate(LocalDate.of(2026, 11, 10));
        pendingReq.setEndDate(LocalDate.of(2026, 11, 11));
        pendingReq.setDuration(new java.math.BigDecimal("2.00"));
        pendingReq.setStatus(LeaveRequestStatus.PENDING);
        pendingReq.setSubmissionTimestamp(LocalDateTime.now().minusHours(5));

        when(leaveRequestRepository.findByStatusOrderBySubmissionTimestampAsc(LeaveRequestStatus.PENDING))
                .thenReturn(List.of(pendingReq));

        WorkforceConflictReport report = new WorkforceConflictReport();
        report.setHasConflict(true);
        report.setBlockingEnabled(true);
        report.setSummaryMessage("Staffing shortage: below required minimum threshold");
        report.setAffectedDates(List.of(new com.anudip.employee_leave.dto.DailyAvailabilitySnapshot(),
                new com.anudip.employee_leave.dto.DailyAvailabilitySnapshot()));
        when(leaveConflictEngineService.evaluateLeaveRequestConflicts(pendingReq)).thenReturn(report);

        // 3. Manual balance adjustment transaction
        LeaveTransaction tx = new LeaveTransaction(
                null, emp, leaveType, 2026, LeaveTransactionType.MANUAL_ADJUSTMENT_CREDIT,
                new java.math.BigDecimal("5.00"), new java.math.BigDecimal("10.00"), new java.math.BigDecimal("15.00"),
                "Annual service anniversary credit", "REF-001", "ADJ-KEY-001", "HR_ADMIN"
        );
        tx.setId(50L);
        when(leaveTransactionRepository.findByTransactionTypeInWithDetails(any())).thenReturn(List.of(tx));

        List<PolicyExceptionItem> exceptions = policyExceptionService.getPolicyExceptions(null, null, null);

        assertThat(exceptions).hasSize(3);

        Map<String, Long> counts = policyExceptionService.getExceptionCounts();
        assertThat(counts.get("TOTAL")).isEqualTo(3L);
        assertThat(counts.get(PolicyExceptionService.CAT_AUTHORIZED_OVERRIDE)).isEqualTo(1L);
        assertThat(counts.get(PolicyExceptionService.CAT_PENDING_SHORTAGE_RISK)).isEqualTo(1L);
        assertThat(counts.get(PolicyExceptionService.CAT_ADMINISTRATIVE_ADJUSTMENT)).isEqualTo(1L);
    }

    @Test
    void testGetPolicyExceptions_FiltersByCategory() {
        LeaveRequest overrideReq = new LeaveRequest();
        overrideReq.setId(1L);
        overrideReq.setRequestNumber("LR-2026-0001");
        overrideReq.setEmployee(emp);
        overrideReq.setLeaveType(leaveType);
        overrideReq.setStartDate(LocalDate.of(2026, 11, 2));
        overrideReq.setEndDate(LocalDate.of(2026, 11, 4));
        overrideReq.setDuration(new java.math.BigDecimal("3.00"));
        overrideReq.setStatus(LeaveRequestStatus.APPROVED);
        overrideReq.setStaffingOverrideJustification("Cover arranged");
        overrideReq.setSubmissionTimestamp(LocalDateTime.now());

        when(leaveRequestRepository.findStaffingOverridesWithDetails()).thenReturn(List.of(overrideReq));

        List<PolicyExceptionItem> result = policyExceptionService.getPolicyExceptions(
                PolicyExceptionService.CAT_AUTHORIZED_OVERRIDE, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo(PolicyExceptionService.CAT_AUTHORIZED_OVERRIDE);
        assertThat(result.get(0).getJustification()).isEqualTo("Cover arranged");
    }
}
