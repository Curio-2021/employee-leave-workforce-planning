package com.anudip.employee_leave;

import com.anudip.employee_leave.dto.DailyAvailabilitySnapshot;
import com.anudip.employee_leave.dto.DepartmentStaffingRuleDTO;
import com.anudip.employee_leave.dto.HolidayDTO;
import com.anudip.employee_leave.dto.LeaveInitializationDTO;
import com.anudip.employee_leave.dto.LeaveRequestDTO;
import com.anudip.employee_leave.dto.WorkforceConflictReport;
import com.anudip.employee_leave.entity.ConflictSeverity;
import com.anudip.employee_leave.entity.Department;
import com.anudip.employee_leave.entity.DepartmentStaffingRule;
import com.anudip.employee_leave.entity.Employee;
import com.anudip.employee_leave.entity.EmploymentStatus;
import com.anudip.employee_leave.entity.EmploymentType;
import com.anudip.employee_leave.entity.Holiday;
import com.anudip.employee_leave.entity.HolidayType;
import com.anudip.employee_leave.entity.LeaveBalance;
import com.anudip.employee_leave.entity.LeavePolicy;
import com.anudip.employee_leave.entity.LeaveRequest;
import com.anudip.employee_leave.entity.LeaveRequestStatus;
import com.anudip.employee_leave.entity.LeaveTransaction;
import com.anudip.employee_leave.entity.LeaveTransactionType;
import com.anudip.employee_leave.entity.LeaveType;
import com.anudip.employee_leave.exception.StaffingShortageException;
import com.anudip.employee_leave.repository.DepartmentRepository;
import com.anudip.employee_leave.repository.DepartmentStaffingRuleRepository;
import com.anudip.employee_leave.repository.EmployeeRepository;
import com.anudip.employee_leave.repository.HolidayRepository;
import com.anudip.employee_leave.repository.LeaveBalanceRepository;
import com.anudip.employee_leave.repository.LeavePolicyRepository;
import com.anudip.employee_leave.repository.LeaveRequestRepository;
import com.anudip.employee_leave.repository.LeaveTransactionRepository;
import com.anudip.employee_leave.repository.LeaveTypeRepository;
import com.anudip.employee_leave.service.DepartmentStaffingRuleService;
import com.anudip.employee_leave.service.HolidayService;
import com.anudip.employee_leave.service.LeaveBalanceService;
import com.anudip.employee_leave.service.LeaveConflictEngineService;
import com.anudip.employee_leave.service.LeaveRequestService;
import com.anudip.employee_leave.service.WorkforceAvailabilityService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class WorkforceAvailabilityConflictIntegrationTest {

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
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private LeaveTransactionRepository leaveTransactionRepository;

    @Autowired
    private HolidayRepository holidayRepository;

    @Autowired
    private DepartmentStaffingRuleRepository staffingRuleRepository;

    @Autowired
    private DepartmentStaffingRuleService staffingRuleService;

    @Autowired
    private LeaveBalanceService leaveBalanceService;

    @Autowired
    private LeaveRequestService leaveRequestService;

    @Autowired
    private WorkforceAvailabilityService availabilityService;

    @Autowired
    private LeaveConflictEngineService conflictEngineService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;
    private String uniqueSuffix;

    private Department testDept;
    private Employee employee1;
    private Employee employee2;
    private Employee manager;
    private LeaveType testLeaveType;
    private LeavePolicy testPolicy;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);

        transactionTemplate.execute(status -> {
            testDept = new Department("Dept-" + uniqueSuffix, "Test Department");
            testDept = departmentRepository.save(testDept);

            manager = new Employee("MGR-" + uniqueSuffix, "Manager " + uniqueSuffix,
                    "mgr-" + uniqueSuffix + "@example.com", testDept, EmploymentType.FULL_TIME,
                    LocalDate.of(2020, 1, 1), EmploymentStatus.ACTIVE, null);
            manager = employeeRepository.save(manager);

            employee1 = new Employee("EMP1-" + uniqueSuffix, "Worker One " + uniqueSuffix,
                    "emp1-" + uniqueSuffix + "@example.com", testDept, EmploymentType.FULL_TIME,
                    LocalDate.of(2024, 1, 1), EmploymentStatus.ACTIVE, manager);
            employee1 = employeeRepository.save(employee1);

            employee2 = new Employee("EMP2-" + uniqueSuffix, "Worker Two " + uniqueSuffix,
                    "emp2-" + uniqueSuffix + "@example.com", testDept, EmploymentType.FULL_TIME,
                    LocalDate.of(2024, 1, 1), EmploymentStatus.ACTIVE, manager);
            employee2 = employeeRepository.save(employee2);

            testLeaveType = new LeaveType("Leave-" + uniqueSuffix, "L-" + uniqueSuffix.substring(0, 3),
                    "Test Leave", true, true, 10, false);
            testLeaveType = leaveTypeRepository.save(testLeaveType);

            testPolicy = new LeavePolicy("Policy " + uniqueSuffix, testLeaveType, null, null,
                    20, false, 0, 0, 2026, true);
            testPolicy = leavePolicyRepository.save(testPolicy);

            LeaveInitializationDTO init1 = new LeaveInitializationDTO();
            init1.setEmployeeId(employee1.getId());
            init1.setLeaveTypeId(testLeaveType.getId());
            init1.setLeaveYear(2026);
            leaveBalanceService.initializeEntitlements(init1);

            LeaveInitializationDTO init2 = new LeaveInitializationDTO();
            init2.setEmployeeId(employee2.getId());
            init2.setLeaveTypeId(testLeaveType.getId());
            init2.setLeaveYear(2026);
            leaveBalanceService.initializeEntitlements(init2);

            return null;
        });
    }

    @AfterEach
    void tearDown() {
        transactionTemplate.execute(status -> {
            leaveTransactionRepository.deleteAll();
            leaveRequestRepository.deleteAll();
            leaveBalanceRepository.deleteAll();
            staffingRuleRepository.deleteAll();
            employeeRepository.deleteAll();
            departmentRepository.deleteAll();
            leavePolicyRepository.deleteAll();
            leaveTypeRepository.deleteAll();
            return null;
        });
    }

    @Test
    void testAvailabilityCalculation_WorkingDayAndHoliday() {
        LocalDate workingDay = LocalDate.of(2026, 11, 4); // Wednesday
        LocalDate holidayDate = LocalDate.of(2026, 11, 5); // Thursday

        transactionTemplate.execute(status -> {
            Holiday holiday = new Holiday();
            holiday.setName("Test Holiday " + uniqueSuffix);
            holiday.setHolidayDate(holidayDate);
            holiday.setCalendarYear(holidayDate.getYear());
            holiday.setHolidayType(HolidayType.MANDATORY);
            holiday.setActive(true);
            holiday.setDescription("Company Holiday");
            holidayRepository.save(holiday);

            // Rule: Min 2 staff required, Mon-Fri
            DepartmentStaffingRuleDTO ruleDTO = new DepartmentStaffingRuleDTO();
            ruleDTO.setDepartmentId(testDept.getId());
            ruleDTO.setMinimumStaffCount(2);
            ruleDTO.setStrictEnforcement(true);
            ruleDTO.setIncludeWeekends(false);
            ruleDTO.setActive(true);
            staffingRuleService.saveOrUpdateRule(ruleDTO);

            return null;
        });

        // 1. Check working day snapshot
        DailyAvailabilitySnapshot workingSnapshot = availabilityService.getDepartmentDailyAvailability(testDept.getId(), workingDay);
        assertTrue(workingSnapshot.isWorkingDay());
        assertFalse(workingSnapshot.isHoliday());
        assertEquals(3, workingSnapshot.getTotalActiveStaff()); // manager + employee1 + employee2 = 3
        assertEquals(2, workingSnapshot.getRequiredStaff());
        assertEquals(3.0, workingSnapshot.getAvailableStaff());
        assertEquals(ConflictSeverity.NONE, workingSnapshot.getSeverity());

        // 2. Check holiday snapshot
        DailyAvailabilitySnapshot holidaySnapshot = availabilityService.getDepartmentDailyAvailability(testDept.getId(), holidayDate);
        assertFalse(holidaySnapshot.isWorkingDay());
        assertTrue(holidaySnapshot.isHoliday());
        assertEquals(ConflictSeverity.NONE, holidaySnapshot.getSeverity());

        // Clean up test holiday
        transactionTemplate.execute(status -> {
            holidayRepository.findAll().stream()
                    .filter(h -> h.getName().contains(uniqueSuffix))
                    .forEach(holidayRepository::delete);
            return null;
        });
    }

    @Test
    void testStrictStaffingRule_BlocksApprovalWhenMinimumBreached() {
        // Department has 3 active staff (manager, employee1, employee2).
        // Set minimum staff requirement to 3! (So any leave approval breaches it)
        transactionTemplate.execute(status -> {
            DepartmentStaffingRuleDTO ruleDTO = new DepartmentStaffingRuleDTO();
            ruleDTO.setDepartmentId(testDept.getId());
            ruleDTO.setMinimumStaffCount(3);
            ruleDTO.setStrictEnforcement(true);
            ruleDTO.setActive(true);
            staffingRuleService.saveOrUpdateRule(ruleDTO);
            return null;
        });

        // Employee 1 submits leave for Wednesday 2026-11-04
        LocalDate leaveDate = LocalDate.of(2026, 11, 4);
        LeaveRequestDTO reqDTO = new LeaveRequestDTO(employee1.getId(), testLeaveType.getId(),
                leaveDate, leaveDate, false, "Family event");
        LeaveRequest submitted = leaveRequestService.submitLeaveRequest(reqDTO);

        // Pre-approval conflict evaluation should flag critical shortage
        WorkforceConflictReport preview = conflictEngineService.evaluateLeaveRequestConflicts(submitted);
        assertTrue(preview.isHasConflict());
        assertTrue(preview.isBlockingEnabled());
        assertEquals(ConflictSeverity.CRITICAL_STAFFING_SHORTAGE, preview.getSeverity());

        // Approving must throw StaffingShortageException
        assertThrows(StaffingShortageException.class, () ->
                leaveRequestService.approveLeaveRequest(submitted.getId(), manager.getId(), "Trying to approve", false, null));

        // Verify request remains PENDING
        LeaveRequest reloaded = leaveRequestRepository.findById(submitted.getId()).orElseThrow();
        assertEquals(LeaveRequestStatus.PENDING, reloaded.getStatus());
    }

    @Test
    void testAdvisoryStaffingRule_RequiresOverrideConfirmationAndJustification() {
        // Minimum 3 staff required, but strict = false (Advisory)
        transactionTemplate.execute(status -> {
            DepartmentStaffingRuleDTO ruleDTO = new DepartmentStaffingRuleDTO();
            ruleDTO.setDepartmentId(testDept.getId());
            ruleDTO.setMinimumStaffCount(3);
            ruleDTO.setStrictEnforcement(false); // Advisory mode
            ruleDTO.setActive(true);
            staffingRuleService.saveOrUpdateRule(ruleDTO);
            return null;
        });

        LocalDate leaveDate = LocalDate.of(2026, 11, 4);
        LeaveRequestDTO reqDTO = new LeaveRequestDTO(employee1.getId(), testLeaveType.getId(),
                leaveDate, leaveDate, false, "Urgent work");
        LeaveRequest submitted = leaveRequestService.submitLeaveRequest(reqDTO);

        // 1. Without overrideConfirmed -> throws StaffingShortageException
        assertThrows(StaffingShortageException.class, () ->
                leaveRequestService.approveLeaveRequest(submitted.getId(), manager.getId(), "Trying normal approve", false, null));

        // 2. With overrideConfirmed = true but empty justification -> throws StaffingShortageException
        assertThrows(StaffingShortageException.class, () ->
                leaveRequestService.approveLeaveRequest(submitted.getId(), manager.getId(), "Trying override", true, "   "));

        // 3. With overrideConfirmed = true and proper justification -> succeeds!
        LeaveRequest approved = leaveRequestService.approveLeaveRequest(
                submitted.getId(), manager.getId(), "Approved", true, "Cross-department backup engineer assigned.");

        assertEquals(LeaveRequestStatus.APPROVED, approved.getStatus());
        assertEquals(manager.getId(), approved.getDecidedBy().getId());
        assertEquals("Cross-department backup engineer assigned.", approved.getStaffingOverrideJustification());
        assertNull(approved.getRejectionReason(), "Rejection reason must be null on approved request");

        // Verify persisted state after database reload
        LeaveRequest reloadedApproved = leaveRequestRepository.findById(approved.getId()).orElseThrow();
        assertEquals("Cross-department backup engineer assigned.", reloadedApproved.getStaffingOverrideJustification());
        assertNull(reloadedApproved.getRejectionReason(), "Rejection reason must remain null in database for approved override");
    }

    @Test
    void testFridayToMondayLeave_StandardMonFriDepartment_OnlyDeductsTwoDays() {
        // Standard Mon-Fri department (no weekend rule configured)
        LocalDate friday = LocalDate.of(2026, 11, 6);
        LocalDate monday = LocalDate.of(2026, 11, 9);

        LeaveRequestDTO reqDTO = new LeaveRequestDTO(employee1.getId(), testLeaveType.getId(),
                friday, monday, false, "Long weekend leave");
        LeaveRequest submitted = leaveRequestService.submitLeaveRequest(reqDTO);

        // Net working days must be exactly 2.00 (Friday + Monday), skipping Saturday and Sunday
        assertEquals(BigDecimal.valueOf(2.00).setScale(2, RoundingMode.HALF_UP), submitted.getDuration());

        // Approve and verify balance deduction
        leaveRequestService.approveLeaveRequest(submitted.getId(), manager.getId(), "Approved");

        LeaveBalance bal = leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndLeaveYear(
                employee1.getId(), testLeaveType.getId(), 2026).orElseThrow();
        // 20.00 entitlement - 2.00 working days = 18.00 available balance
        assertEquals(0, new BigDecimal("18.00").compareTo(bal.getAvailableBalance()),
                "Balance must be deducted by exactly 2.00 working days, not 4 calendar days");
    }

    @Test
    void testFridayToMondayLeave_WeekendOperatingDepartment_DeductsFourDays() {
        // Configure department to operate 7 days a week (includeWeekends = true)
        transactionTemplate.execute(status -> {
            DepartmentStaffingRuleDTO ruleDTO = new DepartmentStaffingRuleDTO();
            ruleDTO.setDepartmentId(testDept.getId());
            ruleDTO.setMinimumStaffCount(1);
            ruleDTO.setStrictEnforcement(false);
            ruleDTO.setIncludeWeekends(true); // Continuous operations
            ruleDTO.setActive(true);
            staffingRuleService.saveOrUpdateRule(ruleDTO);
            return null;
        });

        LocalDate friday = LocalDate.of(2026, 11, 6);
        LocalDate monday = LocalDate.of(2026, 11, 9);

        LeaveRequestDTO reqDTO = new LeaveRequestDTO(employee1.getId(), testLeaveType.getId(),
                friday, monday, false, "Continuous shift leave");
        LeaveRequest submitted = leaveRequestService.submitLeaveRequest(reqDTO);

        // Net working days must be exactly 4.00 (Friday, Saturday, Sunday, Monday)
        assertEquals(BigDecimal.valueOf(4.00).setScale(2, RoundingMode.HALF_UP), submitted.getDuration());

        // Approve and verify balance deduction
        leaveRequestService.approveLeaveRequest(submitted.getId(), manager.getId(), "Approved 7-day schedule");

        LeaveBalance bal = leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndLeaveYear(
                employee1.getId(), testLeaveType.getId(), 2026).orElseThrow();
        // 20.00 entitlement - 4.00 working days = 16.00 available balance
        assertEquals(0, new BigDecimal("16.00").compareTo(bal.getAvailableBalance()),
                "Balance must be deducted by 4.00 working days for weekend-operating department");
    }

    @Test
    void testRejectLeaveRequest_SetsRejectionReason_PreservesNullOverrideJustification() {
        LocalDate leaveDate = LocalDate.of(2026, 11, 4);
        LeaveRequestDTO reqDTO = new LeaveRequestDTO(employee1.getId(), testLeaveType.getId(),
                leaveDate, leaveDate, false, "Personal business");
        LeaveRequest submitted = leaveRequestService.submitLeaveRequest(reqDTO);

        LeaveRequest rejected = leaveRequestService.rejectLeaveRequest(
                submitted.getId(), manager.getId(), "Operational peak freeze", "Please reschedule for next month");

        assertEquals(LeaveRequestStatus.REJECTED, rejected.getStatus());
        assertEquals("Operational peak freeze", rejected.getRejectionReason());
        assertNull(rejected.getStaffingOverrideJustification(), "Override justification must be null on rejected request");

        LeaveRequest reloadedRejected = leaveRequestRepository.findById(rejected.getId()).orElseThrow();
        assertEquals("Operational peak freeze", reloadedRejected.getRejectionReason());
        assertNull(reloadedRejected.getStaffingOverrideJustification());
    }

    @Test
    void testConcurrentApprovals_SerializedByDepartmentLock_PreventsOverApproval() throws Exception {
        // Department has 3 active staff.
        // Set minimum staff requirement to 2.
        // If employee1 and employee2 both submit leave on the same date:
        // Approving one leaves 2 staff available (meets minimum 2).
        // Approving BOTH would leave only 1 staff available (breaches minimum 2)!
        // With strict = true, concurrent execution MUST serialize and exactly 1 can succeed, while the other fails!
        transactionTemplate.execute(status -> {
            DepartmentStaffingRuleDTO ruleDTO = new DepartmentStaffingRuleDTO();
            ruleDTO.setDepartmentId(testDept.getId());
            ruleDTO.setMinimumStaffCount(2);
            ruleDTO.setStrictEnforcement(true); // Strict block
            ruleDTO.setActive(true);
            staffingRuleService.saveOrUpdateRule(ruleDTO);
            return null;
        });

        LocalDate date = LocalDate.of(2026, 11, 4);

        LeaveRequestDTO req1 = new LeaveRequestDTO(employee1.getId(), testLeaveType.getId(),
                date, date, false, "Worker 1 leave");
        LeaveRequest r1 = leaveRequestService.submitLeaveRequest(req1);

        LeaveRequestDTO req2 = new LeaveRequestDTO(employee2.getId(), testLeaveType.getId(),
                date, date, false, "Worker 2 leave");
        LeaveRequest r2 = leaveRequestService.submitLeaveRequest(req2);

        // Launch concurrent approval in 2 parallel threads
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger shortageBlockedCount = new AtomicInteger(0);

        executor.submit(() -> {
            try {
                startLatch.await();
                leaveRequestService.approveLeaveRequest(r1.getId(), manager.getId(), "Approve T1", false, null);
                successCount.incrementAndGet();
            } catch (StaffingShortageException e) {
                shortageBlockedCount.incrementAndGet();
            } catch (Exception e) {
                // other unexpected
            } finally {
                doneLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await();
                leaveRequestService.approveLeaveRequest(r2.getId(), manager.getId(), "Approve T2", false, null);
                successCount.incrementAndGet();
            } catch (StaffingShortageException e) {
                shortageBlockedCount.incrementAndGet();
            } catch (Exception e) {
                // other unexpected
            } finally {
                doneLatch.countDown();
            }
        });

        // Fire both threads simultaneously
        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Concurrent approvals should complete within timeout");
        // Exactly ONE approval must succeed, and the other must be blocked by StaffingShortageException!
        assertEquals(1, successCount.get(), "Exactly one approval must succeed");
        assertEquals(1, shortageBlockedCount.get(), "The competing concurrent approval must be blocked by StaffingShortageException");

        // Assert database state after concurrency execution
        transactionTemplate.execute(status -> {
            LeaveRequest req1Reloaded = leaveRequestRepository.findById(r1.getId()).orElseThrow();
            LeaveRequest req2Reloaded = leaveRequestRepository.findById(r2.getId()).orElseThrow();

            boolean req1Approved = req1Reloaded.getStatus() == LeaveRequestStatus.APPROVED;
            boolean req2Approved = req2Reloaded.getStatus() == LeaveRequestStatus.APPROVED;

            // Exactly one must be APPROVED and the other PENDING
            assertTrue(req1Approved ^ req2Approved, "Exactly one leave request must be APPROVED, and one must remain PENDING");

            // Verify available staffing is compliant with minimum requirement
            DailyAvailabilitySnapshot snapshot = availabilityService.getDepartmentDailyAvailability(testDept.getId(), date);
            assertEquals(2.0, snapshot.getAvailableStaff(), "Department staffing must remain at or above minimum");
            assertEquals(0.0, snapshot.getDeficit(), "Department must have zero deficit");

            // Verify balance deductions
            LeaveBalance bal1 = leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndLeaveYear(
                    employee1.getId(), testLeaveType.getId(), 2026).orElseThrow();
            LeaveBalance bal2 = leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndLeaveYear(
                    employee2.getId(), testLeaveType.getId(), 2026).orElseThrow();

            if (req1Approved) {
                assertEquals(0, new BigDecimal("19.00").compareTo(bal1.getAvailableBalance()), "Approved worker balance should be 19.00");
                assertEquals(0, new BigDecimal("20.00").compareTo(bal2.getAvailableBalance()), "Blocked worker balance should remain 20.00");
            } else {
                assertEquals(0, new BigDecimal("20.00").compareTo(bal1.getAvailableBalance()), "Blocked worker balance should remain 20.00");
                assertEquals(0, new BigDecimal("19.00").compareTo(bal2.getAvailableBalance()), "Approved worker balance should be 19.00");
            }

            // Verify transaction ledger has exactly 1 entry
            List<LeaveTransaction> txs = leaveTransactionRepository.findAll();
            long debitCount = txs.stream()
                    .filter(t -> t.getTransactionType() == LeaveTransactionType.APPROVED_LEAVE_DEDUCTION)
                    .count();
            assertEquals(1, debitCount, "Exactly one APPROVED_LEAVE_DEDUCTION transaction must be recorded in the audit ledger");

            return null;
        });
    }
}
