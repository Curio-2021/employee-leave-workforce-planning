package com.anudip.employee_leave;

import com.anudip.employee_leave.dto.HolidayDTO;
import com.anudip.employee_leave.dto.LeaveInitializationDTO;
import com.anudip.employee_leave.dto.LeaveRequestDTO;
import com.anudip.employee_leave.entity.Department;
import com.anudip.employee_leave.entity.Employee;
import com.anudip.employee_leave.entity.EmploymentStatus;
import com.anudip.employee_leave.entity.EmploymentType;
import com.anudip.employee_leave.entity.HolidayType;
import com.anudip.employee_leave.entity.LeaveBalance;
import com.anudip.employee_leave.entity.LeavePolicy;
import com.anudip.employee_leave.entity.LeaveRequest;
import com.anudip.employee_leave.entity.LeaveRequestStatus;
import com.anudip.employee_leave.entity.LeaveTransaction;
import com.anudip.employee_leave.entity.LeaveTransactionType;
import com.anudip.employee_leave.entity.LeaveType;
import com.anudip.employee_leave.exception.InsufficientLeaveBalanceException;
import com.anudip.employee_leave.exception.InvalidLeaveRequestException;
import com.anudip.employee_leave.repository.DepartmentRepository;
import com.anudip.employee_leave.repository.EmployeeRepository;
import com.anudip.employee_leave.repository.HolidayRepository;
import com.anudip.employee_leave.repository.LeaveBalanceRepository;
import com.anudip.employee_leave.repository.LeavePolicyRepository;
import com.anudip.employee_leave.repository.LeaveRequestRepository;
import com.anudip.employee_leave.repository.LeaveTransactionRepository;
import com.anudip.employee_leave.repository.LeaveTypeRepository;
import com.anudip.employee_leave.service.HolidayService;
import com.anudip.employee_leave.service.LeaveBalanceService;
import com.anudip.employee_leave.service.LeaveRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
public class LeaveRequestWorkflowIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Autowired
    private LeavePolicyRepository leavePolicyRepository;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private LeaveTransactionRepository leaveTransactionRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private HolidayRepository holidayRepository;

    @Autowired
    private HolidayService holidayService;

    @Autowired
    private LeaveBalanceService leaveBalanceService;

    @Autowired
    private LeaveRequestService leaveRequestService;

    private Department dept;
    private Employee manager;
    private Employee employee;
    private LeaveType casualLeave;
    private LeavePolicy policy;
    private LeaveBalance balance;

    @BeforeEach
    void setUp() {
        String testSuffix = UUID.randomUUID().toString().substring(0, 6);

        dept = new Department("Test Dept " + testSuffix, "Integration Test Department");
        dept = departmentRepository.save(dept);

        manager = new Employee("MGR-" + testSuffix, "Manager " + testSuffix,
                "mgr." + testSuffix + "@test.com", dept,
                EmploymentType.FULL_TIME, LocalDate.of(2022, 1, 1),
                EmploymentStatus.ACTIVE, null);
        manager = employeeRepository.save(manager);

        employee = new Employee("EMP-" + testSuffix, "Employee " + testSuffix,
                "emp." + testSuffix + "@test.com", dept,
                EmploymentType.FULL_TIME, LocalDate.of(2023, 1, 1),
                EmploymentStatus.ACTIVE, manager);
        employee = employeeRepository.save(employee);

        casualLeave = new LeaveType("CL " + testSuffix, "CL" + testSuffix.substring(0, 3).toUpperCase(),
                "Casual Leave Test", true, true, 5, false);
        casualLeave = leaveTypeRepository.save(casualLeave);

        policy = new LeavePolicy("Policy " + testSuffix, casualLeave, null, null,
                15, false, 0, 0, 2026, true);
        policy = leavePolicyRepository.save(policy);

        // Initialize balance with 15 days entitlement for 2026
        LeaveInitializationDTO initDTO = new LeaveInitializationDTO();
        initDTO.setEmployeeId(employee.getId());
        initDTO.setLeaveTypeId(casualLeave.getId());
        initDTO.setLeaveYear(2026);
        balance = leaveBalanceService.initializeEntitlements(initDTO).get(0);
    }

    @Test
    void test01_DatabaseConnectivityAndContextLoad() throws Exception {
        assertNotNull(dataSource, "DataSource should not be null");
        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection, "Active database connection must be established");
            assertFalse(connection.isClosed(), "Database connection must be open");
            assertEquals("MySQL", connection.getMetaData().getDatabaseProductName(), "Must connect to MySQL");
        }
    }

    @Test
    void test02_SubmitValidRequest_StatusPending_BalanceUnchanged() {
        LeaveRequestDTO dto = new LeaveRequestDTO(employee.getId(), casualLeave.getId(),
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 3), false, "Valid test leave request");

        LeaveRequest request = leaveRequestService.submitLeaveRequest(dto);

        assertNotNull(request.getId());
        assertEquals(LeaveRequestStatus.PENDING, request.getStatus());
        assertEquals(BigDecimal.valueOf(3.00).setScale(2, RoundingMode.HALF_UP), request.getDuration());
        assertEquals(manager.getId(), request.getAssignedApprover().getId());
        assertEquals("MANAGER_APPROVAL", request.getApprovalRoute());

        // CRITICAL CHECK: Balance must NOT be deducted at submission
        LeaveBalance currentBalance = leaveBalanceRepository.findById(balance.getId()).orElseThrow();
        assertEquals(BigDecimal.valueOf(0.00).setScale(2, RoundingMode.HALF_UP), currentBalance.getUsedLeave());
        assertEquals(BigDecimal.valueOf(15.00).setScale(2, RoundingMode.HALF_UP), currentBalance.getAvailableBalance());
    }

    @Test
    void test03_Validation_InvalidDateRange() {
        LeaveRequestDTO dto = new LeaveRequestDTO(employee.getId(), casualLeave.getId(),
                LocalDate.of(2026, 6, 5), LocalDate.of(2026, 6, 2), false, "Invalid date order");

        InvalidLeaveRequestException ex = assertThrows(InvalidLeaveRequestException.class, () ->
                leaveRequestService.submitLeaveRequest(dto));
        assertTrue(ex.getMessage().contains("cannot be after end date"));
    }

    @Test
    void test04_Validation_InsufficientBalance() {
        // Remove max consecutive days limit so balance limit triggers
        casualLeave.setMaxConsecutiveDays(null);
        casualLeave = leaveTypeRepository.save(casualLeave);

        // Request 22 working days when only 15 are available
        LeaveRequestDTO dto = new LeaveRequestDTO(employee.getId(), casualLeave.getId(),
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), false, "Exceeding available days");

        InsufficientLeaveBalanceException ex = assertThrows(InsufficientLeaveBalanceException.class, () ->
                leaveRequestService.submitLeaveRequest(dto));
        assertTrue(ex.getMessage().contains("Insufficient leave balance"));
    }

    @Test
    void test05_Validation_OverlappingRequest() {
        // Submit first request for June 1 to June 3
        LeaveRequestDTO dto1 = new LeaveRequestDTO(employee.getId(), casualLeave.getId(),
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 3), false, "First request");
        leaveRequestService.submitLeaveRequest(dto1);

        // Submit clashing request for June 2 to June 4
        LeaveRequestDTO dto2 = new LeaveRequestDTO(employee.getId(), casualLeave.getId(),
                LocalDate.of(2026, 6, 2), LocalDate.of(2026, 6, 4), false, "Clashing request");

        InvalidLeaveRequestException ex = assertThrows(InvalidLeaveRequestException.class, () ->
                leaveRequestService.submitLeaveRequest(dto2));
        assertTrue(ex.getMessage().contains("Overlapping leave request detected"));
    }

    @Test
    void test06_Validation_ExceedsMaxConsecutiveDays() {
        // Casual leave max consecutive limit is 5 days, request 6 working days (June 1 Mon to June 8 Mon)
        LeaveRequestDTO dto = new LeaveRequestDTO(employee.getId(), casualLeave.getId(),
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 8), false, "6 working days consecutive");

        InvalidLeaveRequestException ex = assertThrows(InvalidLeaveRequestException.class, () ->
                leaveRequestService.submitLeaveRequest(dto));
        assertTrue(ex.getMessage().contains("exceeds maximum allowed 5 consecutive days"));
    }

    @Test
    void test07_ApproveValidRequest_DeductionAndLedgerConsistency() {
        LeaveRequestDTO dto = new LeaveRequestDTO(employee.getId(), casualLeave.getId(),
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2), false, "2 days leave");
        LeaveRequest request = leaveRequestService.submitLeaveRequest(dto);

        // Approve request by manager
        LeaveRequest approved = leaveRequestService.approveLeaveRequest(request.getId(), manager.getId(), "Approved by manager");

        assertEquals(LeaveRequestStatus.APPROVED, approved.getStatus());
        assertEquals(manager.getId(), approved.getDecidedBy().getId());
        assertNotNull(approved.getDecisionTimestamp());

        // Verify balance deduction
        LeaveBalance updatedBalance = leaveBalanceRepository.findById(balance.getId()).orElseThrow();
        assertEquals(BigDecimal.valueOf(2.00).setScale(2, RoundingMode.HALF_UP), updatedBalance.getUsedLeave());
        assertEquals(BigDecimal.valueOf(13.00).setScale(2, RoundingMode.HALF_UP), updatedBalance.getAvailableBalance());

        // Verify ledger entry
        String expectedKey = "LEAVE-DEDUCTION-" + request.getRequestNumber();
        assertTrue(leaveTransactionRepository.existsByIdempotencyKey(expectedKey));
        LeaveTransaction tx = leaveTransactionRepository.findByIdempotencyKey(expectedKey).orElseThrow();
        assertEquals(LeaveTransactionType.APPROVED_LEAVE_DEDUCTION, tx.getTransactionType());
        assertEquals(BigDecimal.valueOf(2.00).setScale(2, RoundingMode.HALF_UP), tx.getAmount());
        assertEquals(BigDecimal.valueOf(15.00).setScale(2, RoundingMode.HALF_UP), tx.getPreviousBalance());
        assertEquals(BigDecimal.valueOf(13.00).setScale(2, RoundingMode.HALF_UP), tx.getResultingBalance());
    }

    @Test
    void test08_DuplicateApprovalPrevention() {
        LeaveRequestDTO dto = new LeaveRequestDTO(employee.getId(), casualLeave.getId(),
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2), false, "2 days leave");
        LeaveRequest request = leaveRequestService.submitLeaveRequest(dto);

        // First approval succeeds
        leaveRequestService.approveLeaveRequest(request.getId(), manager.getId(), "First approval");

        // Attempt second approval on already APPROVED request
        InvalidLeaveRequestException ex = assertThrows(InvalidLeaveRequestException.class, () ->
                leaveRequestService.approveLeaveRequest(request.getId(), manager.getId(), "Second approval"));
        assertTrue(ex.getMessage().contains("Only PENDING requests can be approved"));

        // Balance must remain exactly 13 days (NOT deducted twice)
        LeaveBalance currentBalance = leaveBalanceRepository.findById(balance.getId()).orElseThrow();
        assertEquals(BigDecimal.valueOf(2.00).setScale(2, RoundingMode.HALF_UP), currentBalance.getUsedLeave());
        assertEquals(BigDecimal.valueOf(13.00).setScale(2, RoundingMode.HALF_UP), currentBalance.getAvailableBalance());
    }

    @Test
    void test09_RejectPendingRequest_BalanceUnchanged() {
        LeaveRequestDTO dto = new LeaveRequestDTO(employee.getId(), casualLeave.getId(),
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2), false, "2 days leave");
        LeaveRequest request = leaveRequestService.submitLeaveRequest(dto);

        // Reject request
        LeaveRequest rejected = leaveRequestService.rejectLeaveRequest(
                request.getId(), manager.getId(), "Project deadline priority", "Please reschedule");

        assertEquals(LeaveRequestStatus.REJECTED, rejected.getStatus());
        assertEquals("Project deadline priority", rejected.getRejectionReason());
        assertNotNull(rejected.getDecisionTimestamp());

        // Balance must remain completely unchanged
        LeaveBalance currentBalance = leaveBalanceRepository.findById(balance.getId()).orElseThrow();
        assertEquals(BigDecimal.valueOf(0.00).setScale(2, RoundingMode.HALF_UP), currentBalance.getUsedLeave());
        assertEquals(BigDecimal.valueOf(15.00).setScale(2, RoundingMode.HALF_UP), currentBalance.getAvailableBalance());
    }

    @Test
    void test10_CancelPendingRequest_NoBalanceReversal() {
        LeaveRequestDTO dto = new LeaveRequestDTO(employee.getId(), casualLeave.getId(),
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2), false, "Pending request to cancel");
        LeaveRequest request = leaveRequestService.submitLeaveRequest(dto);

        LeaveRequest cancelled = leaveRequestService.cancelLeaveRequest(request.getId(), "Changed mind", "User");
        assertEquals(LeaveRequestStatus.CANCELLED, cancelled.getStatus());

        // Balance remains unchanged
        LeaveBalance currentBalance = leaveBalanceRepository.findById(balance.getId()).orElseThrow();
        assertEquals(BigDecimal.valueOf(0.00).setScale(2, RoundingMode.HALF_UP), currentBalance.getUsedLeave());
        assertEquals(BigDecimal.valueOf(15.00).setScale(2, RoundingMode.HALF_UP), currentBalance.getAvailableBalance());
    }

    @Test
    void test11_CancelApprovedRequest_ReversesDeductionExactlyOnce() {
        LeaveRequestDTO dto = new LeaveRequestDTO(employee.getId(), casualLeave.getId(),
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2), false, "Approved then cancelled");
        LeaveRequest request = leaveRequestService.submitLeaveRequest(dto);

        // Approve (deducts 2 days -> 13 available)
        leaveRequestService.approveLeaveRequest(request.getId(), manager.getId(), "Approved");
        LeaveBalance afterApprove = leaveBalanceRepository.findById(balance.getId()).orElseThrow();
        assertEquals(BigDecimal.valueOf(13.00).setScale(2, RoundingMode.HALF_UP), afterApprove.getAvailableBalance());

        // Cancel approved request (must restore 2 days back to 15 available)
        LeaveRequest cancelled = leaveRequestService.cancelLeaveRequest(request.getId(), "Conference cancelled", "User");
        assertEquals(LeaveRequestStatus.CANCELLED, cancelled.getStatus());

        LeaveBalance afterCancel = leaveBalanceRepository.findById(balance.getId()).orElseThrow();
        assertEquals(BigDecimal.valueOf(0.00).setScale(2, RoundingMode.HALF_UP), afterCancel.getUsedLeave());
        assertEquals(BigDecimal.valueOf(15.00).setScale(2, RoundingMode.HALF_UP), afterCancel.getAvailableBalance());

        // Check reversal transaction ledger entry
        String reversalKey = "LEAVE-REVERSAL-" + request.getRequestNumber();
        assertTrue(leaveTransactionRepository.existsByIdempotencyKey(reversalKey));
        LeaveTransaction revTx = leaveTransactionRepository.findByIdempotencyKey(reversalKey).orElseThrow();
        assertEquals(LeaveTransactionType.REVERSAL_CORRECTION, revTx.getTransactionType());
        assertEquals(BigDecimal.valueOf(2.00).setScale(2, RoundingMode.HALF_UP), revTx.getAmount());
        assertEquals(BigDecimal.valueOf(13.00).setScale(2, RoundingMode.HALF_UP), revTx.getPreviousBalance());
        assertEquals(BigDecimal.valueOf(15.00).setScale(2, RoundingMode.HALF_UP), revTx.getResultingBalance());

        // Attempting to cancel again on CANCELLED request throws exception
        assertThrows(InvalidLeaveRequestException.class, () ->
                leaveRequestService.cancelLeaveRequest(request.getId(), "Cancel again", "User"));
    }

    @Test
    void test12_HolidayExclusionAndHalfDayDuration() {
        // Register a holiday on 2026-07-02
        HolidayDTO holDTO = new HolidayDTO();
        holDTO.setName("Test Holiday " + UUID.randomUUID().toString().substring(0, 5));
        holDTO.setHolidayDate(LocalDate.of(2026, 7, 2));
        holDTO.setHolidayType(HolidayType.MANDATORY);
        holDTO.setActive(true);
        holidayService.createHoliday(holDTO);

        // Request July 1 to July 3 (3 calendar days, 1 holiday -> 2.0 working days)
        BigDecimal duration = leaveRequestService.calculateDuration(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3), false);
        assertEquals(BigDecimal.valueOf(2.00).setScale(2, RoundingMode.HALF_UP), duration);

        // Half day on non-holiday July 1
        BigDecimal halfDayDuration = leaveRequestService.calculateDuration(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1), true);
        assertEquals(new BigDecimal("0.50"), halfDayDuration);

        // Half day on holiday July 2 -> 0.00 days
        BigDecimal halfDayOnHoliday = leaveRequestService.calculateDuration(
                LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 2), true);
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), halfDayOnHoliday);
    }

    @Test
    void test13_SelfApprovalForbidden() {
        LeaveRequestDTO dto = new LeaveRequestDTO(employee.getId(), casualLeave.getId(),
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2), false, "Self approval test");
        LeaveRequest request = leaveRequestService.submitLeaveRequest(dto);

        // Employee attempts to approve their own request
        InvalidLeaveRequestException ex = assertThrows(InvalidLeaveRequestException.class, () ->
                leaveRequestService.approveLeaveRequest(request.getId(), employee.getId(), "Self approving"));
        assertTrue(ex.getMessage().contains("Self-approval is not permitted"));
    }
}
