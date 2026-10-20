package com.anudip.employee_leave.service;

import com.anudip.employee_leave.dto.LeaveAdjustmentDTO;
import com.anudip.employee_leave.dto.LeaveInitializationDTO;
import com.anudip.employee_leave.entity.Department;
import com.anudip.employee_leave.entity.Employee;
import com.anudip.employee_leave.entity.EmploymentStatus;
import com.anudip.employee_leave.entity.EmploymentType;
import com.anudip.employee_leave.entity.LeaveBalance;
import com.anudip.employee_leave.entity.LeavePolicy;
import com.anudip.employee_leave.entity.LeaveTransaction;
import com.anudip.employee_leave.entity.LeaveTransactionType;
import com.anudip.employee_leave.entity.LeaveType;
import com.anudip.employee_leave.exception.InsufficientLeaveBalanceException;
import com.anudip.employee_leave.exception.PolicyConflictException;
import com.anudip.employee_leave.exception.ResourceNotFoundException;
import com.anudip.employee_leave.repository.EmployeeRepository;
import com.anudip.employee_leave.repository.LeaveBalanceRepository;
import com.anudip.employee_leave.repository.LeavePolicyRepository;
import com.anudip.employee_leave.repository.LeaveTransactionRepository;
import com.anudip.employee_leave.repository.LeaveTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveBalanceServiceTest {

    @Mock
    private LeaveBalanceRepository leaveBalanceRepository;

    @Mock
    private LeaveTransactionRepository leaveTransactionRepository;

    @Mock
    private LeavePolicyRepository leavePolicyRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private LeaveTypeRepository leaveTypeRepository;

    @InjectMocks
    private LeaveBalanceService leaveBalanceService;

    private Department department;
    private Employee employee;
    private LeaveType casualLeave;
    private LeavePolicy standardPolicy;
    private LeaveBalance existingBalance;

    @BeforeEach
    void setUp() {
        department = new Department("Engineering", "Software");
        department.setId(1L);

        employee = new Employee("EMP-001", "Jane Doe", "jane@example.com", department,
                EmploymentType.FULL_TIME, LocalDate.of(2024, 1, 1), EmploymentStatus.ACTIVE, null);
        employee.setId(1L);

        casualLeave = new LeaveType("Casual Leave", "CL", "Personal days", true, true, 3, false);
        casualLeave.setId(1L);

        standardPolicy = new LeavePolicy("Standard Casual 2026", casualLeave, null, null,
                12, true, 5, 0, 2026, true);
        standardPolicy.setId(1L);

        existingBalance = new LeaveBalance(employee, casualLeave, standardPolicy, 2026);
        existingBalance.setId(1L);
        existingBalance.setGrantedEntitlement(BigDecimal.valueOf(12.00));
        existingBalance.recalculateBalance();
    }

    @Test
    void testEntitlementInitializationSuccess() {
        LeaveInitializationDTO dto = new LeaveInitializationDTO();
        dto.setEmployeeId(1L);
        dto.setLeaveYear(2026);
        dto.setLeaveTypeId(1L);
        dto.setIncludeCarryForward(false);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(casualLeave));
        when(leavePolicyRepository.findByEffectiveYear(2026)).thenReturn(List.of(standardPolicy));
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndLeaveYear(1L, 1L, 2026))
                .thenReturn(Optional.of(existingBalance));
        when(leaveTransactionRepository.existsByIdempotencyKey(any())).thenReturn(false);
        when(leaveBalanceRepository.save(any(LeaveBalance.class))).thenReturn(existingBalance);

        List<LeaveBalance> balances = leaveBalanceService.initializeEntitlements(dto);

        assertNotNull(balances);
        assertEquals(1, balances.size());
        assertEquals(BigDecimal.valueOf(12.00).setScale(2, RoundingMode.HALF_UP), balances.get(0).getAvailableBalance());
        verify(leaveTransactionRepository).save(any(LeaveTransaction.class));
    }

    @Test
    void testMissingPolicyThrowsResourceNotFoundException() {
        when(leavePolicyRepository.findByEffectiveYear(2026)).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class,
                () -> leaveBalanceService.resolveApplicablePolicy(employee, casualLeave, 2026));
    }

    @Test
    void testConflictingPoliciesThrowsPolicyConflictException() {
        LeavePolicy policy1 = new LeavePolicy("Policy A", casualLeave, null, null, 10, false, 0, 0, 2026, true);
        policy1.setId(10L);
        LeavePolicy policy2 = new LeavePolicy("Policy B", casualLeave, null, null, 12, false, 0, 0, 2026, true);
        policy2.setId(11L);

        when(leavePolicyRepository.findByEffectiveYear(2026)).thenReturn(List.of(policy1, policy2));

        assertThrows(PolicyConflictException.class,
                () -> leaveBalanceService.resolveApplicablePolicy(employee, casualLeave, 2026));
    }

    @Test
    void testDuplicateEntitlementPreventionIdempotent() {
        LeaveInitializationDTO dto = new LeaveInitializationDTO();
        dto.setEmployeeId(1L);
        dto.setLeaveYear(2026);
        dto.setLeaveTypeId(1L);
        dto.setIncludeCarryForward(false);

        String grantKey = "GRANT-EMP_1-LT_1-YR_2026";
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(casualLeave));
        when(leavePolicyRepository.findByEffectiveYear(2026)).thenReturn(List.of(standardPolicy));
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndLeaveYear(1L, 1L, 2026))
                .thenReturn(Optional.of(existingBalance));
        when(leaveTransactionRepository.existsByIdempotencyKey(grantKey)).thenReturn(true);

        leaveBalanceService.initializeEntitlements(dto);

        // Transaction must NOT be saved again
        verify(leaveTransactionRepository, never()).save(any(LeaveTransaction.class));
    }

    @Test
    void testPositiveAdjustmentCreditSuccess() {
        LeaveAdjustmentDTO dto = new LeaveAdjustmentDTO();
        dto.setEmployeeId(1L);
        dto.setLeaveTypeId(1L);
        dto.setLeaveYear(2026);
        dto.setAdjustmentType("CREDIT");
        dto.setAmount(BigDecimal.valueOf(2.00));
        dto.setReason("Compensatory credit for Saturday shift");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(casualLeave));
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndLeaveYear(1L, 1L, 2026))
                .thenReturn(Optional.of(existingBalance));
        when(leaveBalanceRepository.save(any(LeaveBalance.class))).thenAnswer(i -> i.getArgument(0));

        LeaveBalance result = leaveBalanceService.applyAdjustment(dto);

        assertEquals(BigDecimal.valueOf(14.00).setScale(2, RoundingMode.HALF_UP), result.getAvailableBalance());
        verify(leaveTransactionRepository).save(any(LeaveTransaction.class));
    }

    @Test
    void testNegativeAdjustmentDebitSuccess() {
        LeaveAdjustmentDTO dto = new LeaveAdjustmentDTO();
        dto.setEmployeeId(1L);
        dto.setLeaveTypeId(1L);
        dto.setLeaveYear(2026);
        dto.setAdjustmentType("DEBIT");
        dto.setAmount(BigDecimal.valueOf(3.00));
        dto.setReason("Correction of over-allocated balance");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(casualLeave));
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndLeaveYear(1L, 1L, 2026))
                .thenReturn(Optional.of(existingBalance));
        when(leaveBalanceRepository.save(any(LeaveBalance.class))).thenAnswer(i -> i.getArgument(0));

        LeaveBalance result = leaveBalanceService.applyAdjustment(dto);

        assertEquals(BigDecimal.valueOf(9.00).setScale(2, RoundingMode.HALF_UP), result.getAvailableBalance());
        verify(leaveTransactionRepository).save(any(LeaveTransaction.class));
    }

    @Test
    void testInvalidZeroOrNegativeAdjustmentThrowsException() {
        LeaveAdjustmentDTO dto = new LeaveAdjustmentDTO();
        dto.setAmount(BigDecimal.ZERO);

        assertThrows(IllegalArgumentException.class, () -> leaveBalanceService.applyAdjustment(dto));
    }

    @Test
    void testDebitExceedingAvailableBalanceThrowsInsufficientException() {
        LeaveAdjustmentDTO dto = new LeaveAdjustmentDTO();
        dto.setEmployeeId(1L);
        dto.setLeaveTypeId(1L);
        dto.setLeaveYear(2026);
        dto.setAdjustmentType("DEBIT");
        dto.setAmount(BigDecimal.valueOf(20.00)); // Greater than 12.00
        dto.setReason("Excessive deduction test");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(casualLeave));
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndLeaveYear(1L, 1L, 2026))
                .thenReturn(Optional.of(existingBalance));

        assertThrows(InsufficientLeaveBalanceException.class, () -> leaveBalanceService.applyAdjustment(dto));
        verify(leaveTransactionRepository, never()).save(any(LeaveTransaction.class));
    }

    @Test
    void testDeductLeaveForApprovedRequestSuccess() {
        when(leaveTransactionRepository.existsByIdempotencyKey("LEAVE-DEDUCTION-REQ-101")).thenReturn(false);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(casualLeave));
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndLeaveYear(1L, 1L, 2026))
                .thenReturn(Optional.of(existingBalance));
        when(leaveBalanceRepository.save(any(LeaveBalance.class))).thenAnswer(i -> i.getArgument(0));

        LeaveBalance result = leaveBalanceService.deductLeaveForApprovedRequest(
                1L, 1L, 2026, BigDecimal.valueOf(2.50), "REQ-101", "Approved vacation");

        assertEquals(BigDecimal.valueOf(9.50).setScale(2, RoundingMode.HALF_UP), result.getAvailableBalance());
        verify(leaveTransactionRepository).save(any(LeaveTransaction.class));
    }

    @Test
    void testDuplicateDeductionPreventionIdempotent() {
        when(leaveTransactionRepository.existsByIdempotencyKey("LEAVE-DEDUCTION-REQ-101")).thenReturn(true);
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndLeaveYear(1L, 1L, 2026))
                .thenReturn(Optional.of(existingBalance));

        LeaveBalance result = leaveBalanceService.deductLeaveForApprovedRequest(
                1L, 1L, 2026, BigDecimal.valueOf(2.50), "REQ-101", "Duplicate call");

        // Balance remains unchanged, no additional deduction or transaction saved
        assertEquals(BigDecimal.valueOf(12.00).setScale(2, RoundingMode.HALF_UP), result.getAvailableBalance());
        verify(leaveTransactionRepository, never()).save(any(LeaveTransaction.class));
    }

    @Test
    void testDeductLeaveInsufficientBalanceThrowsException() {
        when(leaveTransactionRepository.existsByIdempotencyKey("LEAVE-DEDUCTION-REQ-102")).thenReturn(false);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(casualLeave));
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndLeaveYear(1L, 1L, 2026))
                .thenReturn(Optional.of(existingBalance));

        assertThrows(InsufficientLeaveBalanceException.class, () ->
                leaveBalanceService.deductLeaveForApprovedRequest(1L, 1L, 2026, BigDecimal.valueOf(15.00), "REQ-102", "Exceeds balance"));
    }

    @Test
    void testCarryForwardHonorsMaxLimits() {
        LeaveInitializationDTO dto = new LeaveInitializationDTO();
        dto.setEmployeeId(1L);
        dto.setLeaveYear(2026);
        dto.setLeaveTypeId(1L);
        dto.setIncludeCarryForward(true);

        LeaveBalance prevYearBalance = new LeaveBalance(employee, casualLeave, standardPolicy, 2025);
        prevYearBalance.setGrantedEntitlement(BigDecimal.valueOf(10.00)); // 10 days remaining
        prevYearBalance.recalculateBalance();

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(casualLeave));
        when(leavePolicyRepository.findByEffectiveYear(2026)).thenReturn(List.of(standardPolicy));
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndLeaveYear(1L, 1L, 2026))
                .thenReturn(Optional.of(existingBalance));
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndLeaveYear(1L, 1L, 2025))
                .thenReturn(Optional.of(prevYearBalance));
        when(leaveTransactionRepository.existsByIdempotencyKey(any())).thenReturn(false);
        when(leaveBalanceRepository.save(any(LeaveBalance.class))).thenAnswer(i -> i.getArgument(0));

        leaveBalanceService.initializeEntitlements(dto);

        // Standard policy max carry forward is 5 days, so 10 available should be capped at 5
        assertEquals(BigDecimal.valueOf(5.00).setScale(2, RoundingMode.HALF_UP), existingBalance.getCarriedForward());
        assertEquals(BigDecimal.valueOf(17.00).setScale(2, RoundingMode.HALF_UP), existingBalance.getAvailableBalance());
        verify(leaveTransactionRepository, times(2)).save(any(LeaveTransaction.class)); // 1 grant + 1 carry forward
    }

    @Test
    void testBalanceCalculationFormula() {
        LeaveBalance bal = new LeaveBalance();
        bal.setCarriedForward(BigDecimal.valueOf(3.50));
        bal.setGrantedEntitlement(BigDecimal.valueOf(12.00));
        bal.setAdjustmentCredits(BigDecimal.valueOf(1.50));
        bal.setAdjustmentDebits(BigDecimal.valueOf(2.00));
        bal.setUsedLeave(BigDecimal.valueOf(4.00));

        // Available = (3.50 + 12.00 + 1.50) - (2.00 + 4.00) = 17.00 - 6.00 = 11.00
        assertEquals(BigDecimal.valueOf(11.00).setScale(2, RoundingMode.HALF_UP), bal.getAvailableBalance());
    }

    @Test
    void testDeductLeaveForApprovedRequest_Success() {
        LeaveBalance bal = new LeaveBalance(employee, casualLeave, standardPolicy, 2026);
        bal.setGrantedEntitlement(BigDecimal.valueOf(12.00));
        bal.recalculateBalance();

        when(leaveTransactionRepository.existsByIdempotencyKey("LEAVE-DEDUCTION-LR-2026-001")).thenReturn(false);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(casualLeave));
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndLeaveYear(1L, 1L, 2026)).thenReturn(Optional.of(bal));
        when(leaveBalanceRepository.save(any(LeaveBalance.class))).thenAnswer(i -> i.getArgument(0));

        LeaveBalance updated = leaveBalanceService.deductLeaveForApprovedRequest(
                1L, 1L, 2026, BigDecimal.valueOf(3.00), "LR-2026-001", "Vacation", "Manager Bob");

        assertEquals(BigDecimal.valueOf(3.00).setScale(2, RoundingMode.HALF_UP), updated.getUsedLeave());
        assertEquals(BigDecimal.valueOf(9.00).setScale(2, RoundingMode.HALF_UP), updated.getAvailableBalance());
        verify(leaveTransactionRepository).save(any(LeaveTransaction.class));
    }

    @Test
    void testDeductLeaveForApprovedRequest_IdempotentGuard() {
        LeaveBalance bal = new LeaveBalance(employee, casualLeave, standardPolicy, 2026);
        bal.setGrantedEntitlement(BigDecimal.valueOf(12.00));
        bal.setUsedLeave(BigDecimal.valueOf(3.00));
        bal.recalculateBalance();

        when(leaveTransactionRepository.existsByIdempotencyKey("LEAVE-DEDUCTION-LR-2026-001")).thenReturn(true);
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndLeaveYear(1L, 1L, 2026)).thenReturn(Optional.of(bal));

        LeaveBalance result = leaveBalanceService.deductLeaveForApprovedRequest(
                1L, 1L, 2026, BigDecimal.valueOf(3.00), "LR-2026-001", "Vacation");

        assertEquals(BigDecimal.valueOf(9.00).setScale(2, RoundingMode.HALF_UP), result.getAvailableBalance());
        verify(leaveBalanceRepository, never()).save(any());
        verify(leaveTransactionRepository, never()).save(any());
    }

    @Test
    void testDeductLeaveForApprovedRequest_InsufficientBalanceThrows() {
        LeaveBalance bal = new LeaveBalance(employee, casualLeave, standardPolicy, 2026);
        bal.setGrantedEntitlement(BigDecimal.valueOf(2.00));
        bal.recalculateBalance();

        when(leaveTransactionRepository.existsByIdempotencyKey("LEAVE-DEDUCTION-LR-2026-002")).thenReturn(false);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(casualLeave));
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndLeaveYear(1L, 1L, 2026)).thenReturn(Optional.of(bal));

        assertThrows(InsufficientLeaveBalanceException.class, () ->
                leaveBalanceService.deductLeaveForApprovedRequest(
                        1L, 1L, 2026, BigDecimal.valueOf(5.00), "LR-2026-002", "Extended leave"));
    }

    @Test
    void testReverseLeaveForCancelledRequest_Success() {
        LeaveBalance bal = new LeaveBalance(employee, casualLeave, standardPolicy, 2026);
        bal.setGrantedEntitlement(BigDecimal.valueOf(12.00));
        bal.setUsedLeave(BigDecimal.valueOf(3.00));
        bal.recalculateBalance();

        when(leaveTransactionRepository.existsByIdempotencyKey("LEAVE-REVERSAL-LR-2026-001")).thenReturn(false);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(casualLeave));
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndLeaveYear(1L, 1L, 2026)).thenReturn(Optional.of(bal));
        when(leaveBalanceRepository.save(any(LeaveBalance.class))).thenAnswer(i -> i.getArgument(0));

        LeaveBalance updated = leaveBalanceService.reverseLeaveForCancelledRequest(
                1L, 1L, 2026, BigDecimal.valueOf(3.00), "LR-2026-001", "Trip cancelled", "System");

        assertEquals(BigDecimal.valueOf(0.00).setScale(2, RoundingMode.HALF_UP), updated.getUsedLeave());
        assertEquals(BigDecimal.valueOf(12.00).setScale(2, RoundingMode.HALF_UP), updated.getAvailableBalance());
        verify(leaveTransactionRepository).save(any(LeaveTransaction.class));
    }
}

