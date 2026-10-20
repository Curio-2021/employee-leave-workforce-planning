package com.anudip.employee_leave.service;

import com.anudip.employee_leave.dto.LeaveRequestDTO;
import com.anudip.employee_leave.dto.WorkforceConflictReport;
import com.anudip.employee_leave.entity.ConflictSeverity;
import com.anudip.employee_leave.entity.Department;
import com.anudip.employee_leave.entity.Employee;
import com.anudip.employee_leave.entity.EmploymentStatus;
import com.anudip.employee_leave.entity.EmploymentType;
import com.anudip.employee_leave.entity.LeaveBalance;
import com.anudip.employee_leave.entity.LeavePolicy;
import com.anudip.employee_leave.entity.LeaveRequest;
import com.anudip.employee_leave.entity.LeaveRequestStatus;
import com.anudip.employee_leave.entity.LeaveType;
import com.anudip.employee_leave.exception.InsufficientLeaveBalanceException;
import com.anudip.employee_leave.exception.InvalidLeaveRequestException;
import com.anudip.employee_leave.exception.ResourceNotFoundException;
import com.anudip.employee_leave.exception.StaffingShortageException;
import com.anudip.employee_leave.repository.DepartmentRepository;
import com.anudip.employee_leave.repository.EmployeeRepository;
import com.anudip.employee_leave.repository.LeaveBalanceRepository;
import com.anudip.employee_leave.repository.LeaveRequestRepository;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.anudip.employee_leave.entity.DepartmentStaffingRule;
import com.anudip.employee_leave.repository.DepartmentStaffingRuleRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveRequestServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private LeaveBalanceRepository leaveBalanceRepository;

    @Mock
    private LeaveBalanceService leaveBalanceService;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private LeaveTypeRepository leaveTypeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private HolidayService holidayService;

    @Mock
    private LeaveConflictEngineService leaveConflictEngineService;

    @Mock
    private DepartmentStaffingRuleRepository staffingRuleRepository;

    private WorkingCalendarService workingCalendarService;

    private LeaveRequestService leaveRequestService;

    private Department dept;
    private Department hrDept;
    private Employee employee;
    private Employee manager;
    private Employee hrStaff;
    private LeaveType casualLeave;
    private LeavePolicy standardPolicy;
    private LeaveBalance leaveBalance;

    @BeforeEach
    void setUp() {
        workingCalendarService = new WorkingCalendarService(holidayService, staffingRuleRepository);
        leaveRequestService = new LeaveRequestService(
                leaveRequestRepository, leaveBalanceRepository, leaveBalanceService,
                employeeRepository, leaveTypeRepository, departmentRepository,
                holidayService, leaveConflictEngineService, workingCalendarService);

        dept = new Department("Engineering", "Eng Dept");
        dept.setId(1L);

        hrDept = new Department("Human Resources", "HR Dept");
        hrDept.setId(2L);

        manager = new Employee("MGR01", "Alice Manager", "alice@example.com", dept,
                EmploymentType.FULL_TIME, LocalDate.of(2020, 1, 1), EmploymentStatus.ACTIVE, null);
        manager.setId(2L);

        hrStaff = new Employee("HR01", "Bob HR", "bob.hr@example.com", hrDept,
                EmploymentType.FULL_TIME, LocalDate.of(2021, 1, 1), EmploymentStatus.ACTIVE, null);
        hrStaff.setId(3L);

        employee = new Employee("EMP01", "John Developer", "john@example.com", dept,
                EmploymentType.FULL_TIME, LocalDate.of(2024, 1, 1), EmploymentStatus.ACTIVE, manager);
        employee.setId(1L);

        casualLeave = new LeaveType("Casual Leave", "CL", "Casual Leave", true, true, 5, false);
        casualLeave.setId(1L);

        standardPolicy = new LeavePolicy("Standard CL Policy", casualLeave, null, null, 12, false, 0, 0, 2026, true);
        standardPolicy.setId(1L);

        leaveBalance = new LeaveBalance(employee, casualLeave, standardPolicy, 2026);
        leaveBalance.setId(1L);
        leaveBalance.setGrantedEntitlement(BigDecimal.valueOf(12.00));
        leaveBalance.recalculateBalance();
    }

    // --- Duration Calculation Tests ---

    @Test
    void testCalculateDuration_FullDaysWithoutHolidays() {
        LocalDate start = LocalDate.of(2026, 4, 8); // Wednesday
        LocalDate end = LocalDate.of(2026, 4, 10);  // Friday
        when(holidayService.isHoliday(any())).thenReturn(false);

        BigDecimal duration = leaveRequestService.calculateDuration(start, end, false);
        assertEquals(BigDecimal.valueOf(3.00).setScale(2, RoundingMode.HALF_UP), duration);
    }

    @Test
    void testCalculateDuration_ExcludesActiveHolidays() {
        LocalDate start = LocalDate.of(2026, 4, 8); // Wednesday
        LocalDate holiday = LocalDate.of(2026, 4, 9); // Thursday
        LocalDate end = LocalDate.of(2026, 4, 10); // Friday

        when(holidayService.isHoliday(holiday)).thenReturn(true);
        when(holidayService.isHoliday(start)).thenReturn(false);
        when(holidayService.isHoliday(end)).thenReturn(false);

        BigDecimal duration = leaveRequestService.calculateDuration(start, end, false);
        assertEquals(BigDecimal.valueOf(2.00).setScale(2, RoundingMode.HALF_UP), duration);
    }

    @Test
    void testCalculateDuration_FridayToMonday_StandardMonFriDepartment_ReturnsTwoDays() {
        LocalDate friday = LocalDate.of(2026, 4, 10);
        LocalDate monday = LocalDate.of(2026, 4, 13);
        when(holidayService.isHoliday(any())).thenReturn(false);

        // Standard Mon-Fri department (no weekend rule)
        BigDecimal duration = leaveRequestService.calculateDuration(dept.getId(), friday, monday, false);
        assertEquals(BigDecimal.valueOf(2.00).setScale(2, RoundingMode.HALF_UP), duration);
    }

    @Test
    void testCalculateDuration_FridayToMonday_WeekendOperatingDepartment_ReturnsFourDays() {
        LocalDate friday = LocalDate.of(2026, 4, 10);
        LocalDate monday = LocalDate.of(2026, 4, 13);
        when(holidayService.isHoliday(any())).thenReturn(false);

        DepartmentStaffingRule weekendRule = new DepartmentStaffingRule(
                dept, 1, null, false, true, true, "24x7 Operations");
        when(staffingRuleRepository.findByDepartmentId(dept.getId())).thenReturn(Optional.of(weekendRule));

        BigDecimal duration = leaveRequestService.calculateDuration(dept.getId(), friday, monday, false);
        assertEquals(BigDecimal.valueOf(4.00).setScale(2, RoundingMode.HALF_UP), duration);
    }

    @Test
    void testCalculateDuration_HalfDay_WorkingDay() {
        LocalDate singleDate = LocalDate.of(2026, 4, 10); // Friday
        when(holidayService.isHoliday(singleDate)).thenReturn(false);

        BigDecimal duration = leaveRequestService.calculateDuration(singleDate, singleDate, true);
        assertEquals(new BigDecimal("0.50"), duration);
    }

    @Test
    void testCalculateDuration_HalfDay_Weekend_StandardMonFri_ReturnsZero() {
        LocalDate saturday = LocalDate.of(2026, 4, 11); // Saturday
        when(holidayService.isHoliday(saturday)).thenReturn(false);

        BigDecimal duration = leaveRequestService.calculateDuration(dept.getId(), saturday, saturday, true);
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), duration);
    }

    @Test
    void testCalculateDuration_HalfDay_Weekend_WeekendOperating_ReturnsHalfDay() {
        LocalDate saturday = LocalDate.of(2026, 4, 11); // Saturday
        when(holidayService.isHoliday(saturday)).thenReturn(false);

        DepartmentStaffingRule weekendRule = new DepartmentStaffingRule(
                dept, 1, null, false, true, true, "24x7 Operations");
        when(staffingRuleRepository.findByDepartmentId(dept.getId())).thenReturn(Optional.of(weekendRule));

        BigDecimal duration = leaveRequestService.calculateDuration(dept.getId(), saturday, saturday, true);
        assertEquals(new BigDecimal("0.50"), duration);
    }

    @Test
    void testCalculateDuration_HalfDayOnHolidayReturnsZero() {
        LocalDate singleDate = LocalDate.of(2026, 4, 10);
        when(holidayService.isHoliday(singleDate)).thenReturn(true);

        BigDecimal duration = leaveRequestService.calculateDuration(singleDate, singleDate, true);
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), duration);
    }

    @Test
    void testCalculateDuration_StartDateAfterEndDateThrows() {
        LocalDate start = LocalDate.of(2026, 4, 15);
        LocalDate end = LocalDate.of(2026, 4, 10);

        assertThrows(IllegalArgumentException.class, () ->
                leaveRequestService.calculateDuration(start, end, false));
    }

    // --- Submission Validation Tests ---

    @Test
    void testSubmitLeaveRequest_Success() {
        LeaveRequestDTO dto = new LeaveRequestDTO(1L, 1L,
                LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 5), false, "Personal work");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(casualLeave));
        when(leaveBalanceService.resolveApplicablePolicy(employee, casualLeave, 2026)).thenReturn(standardPolicy);
        when(holidayService.isHoliday(any())).thenReturn(false);
        when(leaveRequestRepository.findOverlappingRequests(eq(1L), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndLeaveYear(1L, 1L, 2026))
                .thenReturn(Optional.of(leaveBalance));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(i -> {
            LeaveRequest saved = i.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        LeaveRequest result = leaveRequestService.submitLeaveRequest(dto);

        assertNotNull(result);
        assertEquals(LeaveRequestStatus.PENDING, result.getStatus());
        assertEquals(BigDecimal.valueOf(2.00).setScale(2, RoundingMode.HALF_UP), result.getDuration());
        assertEquals(manager, result.getAssignedApprover());
        assertEquals("MANAGER_APPROVAL", result.getApprovalRoute());
        assertTrue(result.getRequestNumber().startsWith("LR-2026-"));

        // Verify balance was NOT deducted at submission time
        verify(leaveBalanceService, never()).deductLeaveForApprovedRequest(anyLong(), anyLong(), anyInt(), any(), anyString(), anyString());
    }

    @Test
    void testSubmitLeaveRequest_InactiveEmployeeThrows() {
        employee.setEmploymentStatus(EmploymentStatus.INACTIVE);
        LeaveRequestDTO dto = new LeaveRequestDTO(1L, 1L,
                LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 5), false, "Personal work");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        assertThrows(InvalidLeaveRequestException.class, () ->
                leaveRequestService.submitLeaveRequest(dto));
    }

    @Test
    void testSubmitLeaveRequest_InactiveLeaveTypeThrows() {
        casualLeave.setActive(false);
        LeaveRequestDTO dto = new LeaveRequestDTO(1L, 1L,
                LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 5), false, "Personal work");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(casualLeave));

        assertThrows(InvalidLeaveRequestException.class, () ->
                leaveRequestService.submitLeaveRequest(dto));
    }

    @Test
    void testSubmitLeaveRequest_CrossYearDatesThrows() {
        LeaveRequestDTO dto = new LeaveRequestDTO(1L, 1L,
                LocalDate.of(2026, 12, 30), LocalDate.of(2027, 1, 2), false, "New year break");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(casualLeave));

        assertThrows(InvalidLeaveRequestException.class, () ->
                leaveRequestService.submitLeaveRequest(dto));
    }

    @Test
    void testSubmitLeaveRequest_ExceedsMaxConsecutiveDaysThrows() {
        casualLeave.setMaxConsecutiveDays(3); // limit is 3 working days
        LeaveRequestDTO dto = new LeaveRequestDTO(1L, 1L,
                LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 8), false, "Extended trip"); // 5 working days (Mon-Fri)

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(casualLeave));
        when(leaveBalanceService.resolveApplicablePolicy(employee, casualLeave, 2026)).thenReturn(standardPolicy);
        when(holidayService.isHoliday(any())).thenReturn(false);

        InvalidLeaveRequestException ex = assertThrows(InvalidLeaveRequestException.class, () ->
                leaveRequestService.submitLeaveRequest(dto));
        assertTrue(ex.getMessage().contains("exceeds maximum allowed 3 consecutive days"));
    }

    @Test
    void testSubmitLeaveRequest_MinServiceDaysViolationThrows() {
        standardPolicy.setMinServiceDays(90);
        // Employee joined on 2026-05-01, requesting leave on 2026-05-15 (only 14 days of service)
        employee.setJoiningDate(LocalDate.of(2026, 5, 1));
        LeaveRequestDTO dto = new LeaveRequestDTO(1L, 1L,
                LocalDate.of(2026, 5, 15), LocalDate.of(2026, 5, 16), false, "Need leave");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(casualLeave));
        when(leaveBalanceService.resolveApplicablePolicy(employee, casualLeave, 2026)).thenReturn(standardPolicy);
        when(holidayService.isHoliday(any())).thenReturn(false);

        assertThrows(InvalidLeaveRequestException.class, () ->
                leaveRequestService.submitLeaveRequest(dto));
    }

    @Test
    void testSubmitLeaveRequest_OverlappingRequestThrows() {
        LeaveRequestDTO dto = new LeaveRequestDTO(1L, 1L,
                LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 6), false, "Clashing request");

        LeaveRequest existing = new LeaveRequest("LR-2026-0001", employee, casualLeave,
                LocalDate.of(2026, 5, 5), LocalDate.of(2026, 5, 8), BigDecimal.valueOf(4),
                false, "Existing", manager, "MANAGER_APPROVAL");
        existing.setStatus(LeaveRequestStatus.PENDING);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(casualLeave));
        when(leaveBalanceService.resolveApplicablePolicy(employee, casualLeave, 2026)).thenReturn(standardPolicy);
        when(holidayService.isHoliday(any())).thenReturn(false);
        when(leaveRequestRepository.findOverlappingRequests(eq(1L), any(), any(), any()))
                .thenReturn(List.of(existing));

        assertThrows(InvalidLeaveRequestException.class, () ->
                leaveRequestService.submitLeaveRequest(dto));
    }

    @Test
    void testSubmitLeaveRequest_InsufficientBalanceThrows() {
        leaveBalance.setGrantedEntitlement(BigDecimal.valueOf(1.00)); // only 1 day available
        leaveBalance.recalculateBalance();

        LeaveRequestDTO dto = new LeaveRequestDTO(1L, 1L,
                LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 6), false, "Requesting 3 days");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(casualLeave));
        when(leaveBalanceService.resolveApplicablePolicy(employee, casualLeave, 2026)).thenReturn(standardPolicy);
        when(holidayService.isHoliday(any())).thenReturn(false);
        when(leaveRequestRepository.findOverlappingRequests(eq(1L), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndLeaveYear(1L, 1L, 2026))
                .thenReturn(Optional.of(leaveBalance));

        assertThrows(InsufficientLeaveBalanceException.class, () ->
                leaveRequestService.submitLeaveRequest(dto));
    }

    @Test
    void testSubmitLeaveRequest_ApproverResolution_RoutesToHrWhenNoManager() {
        employee.setReportingManager(null); // No manager
        LeaveRequestDTO dto = new LeaveRequestDTO(1L, 1L,
                LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 4), false, "Single day");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(casualLeave));
        when(leaveBalanceService.resolveApplicablePolicy(employee, casualLeave, 2026)).thenReturn(standardPolicy);
        when(holidayService.isHoliday(any())).thenReturn(false);
        when(leaveRequestRepository.findOverlappingRequests(eq(1L), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndLeaveYear(1L, 1L, 2026))
                .thenReturn(Optional.of(leaveBalance));
        when(employeeRepository.findActiveHrEmployees()).thenReturn(List.of(hrStaff));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(i -> i.getArgument(0));

        LeaveRequest result = leaveRequestService.submitLeaveRequest(dto);

        assertEquals(hrStaff, result.getAssignedApprover());
        assertEquals("HR_APPROVAL", result.getApprovalRoute());
    }

    // --- Approval and Rejection Tests ---

    @Test
    void testApproveLeaveRequest_Success_AtomicDeduction() {
        LeaveRequest request = new LeaveRequest("LR-2026-999", employee, casualLeave,
                LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 5), BigDecimal.valueOf(2.00),
                false, "Doctor appointment", manager, "MANAGER_APPROVAL");
        request.setId(99L);

        when(leaveRequestRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(request));
        when(departmentRepository.findByIdWithLock(1L)).thenReturn(Optional.of(dept));
        when(leaveConflictEngineService.evaluateLeaveRequestConflicts(request)).thenReturn(new WorkforceConflictReport());
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndLeaveYear(1L, 1L, 2026))
                .thenReturn(Optional.of(leaveBalance));
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(i -> i.getArgument(0));

        LeaveRequest approved = leaveRequestService.approveLeaveRequest(99L, 2L, "Approved, cover arranged");

        assertEquals(LeaveRequestStatus.APPROVED, approved.getStatus());
        assertEquals(manager, approved.getDecidedBy());
        assertEquals("Approved, cover arranged", approved.getApproverComments());
        assertNotNull(approved.getDecisionTimestamp());

        // Verify balance deduction was called
        verify(leaveBalanceService).deductLeaveForApprovedRequest(
                eq(1L), eq(1L), eq(2026), eq(BigDecimal.valueOf(2.00)), eq("LR-2026-999"), anyString(), anyString());
    }

    @Test
    void testApproveLeaveRequest_AlreadyApprovedThrows() {
        LeaveRequest request = new LeaveRequest("LR-2026-999", employee, casualLeave,
                LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 5), BigDecimal.valueOf(2.00),
                false, "Doctor appointment", manager, "MANAGER_APPROVAL");
        request.setId(99L);
        request.setStatus(LeaveRequestStatus.APPROVED);

        when(leaveRequestRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(request));

        assertThrows(InvalidLeaveRequestException.class, () ->
                leaveRequestService.approveLeaveRequest(99L, 2L, "Approved"));
    }

    @Test
    void testApproveLeaveRequest_StrictEnforcement_ThrowsStaffingShortageException() {
        LeaveRequest request = new LeaveRequest("LR-2026-999", employee, casualLeave,
                LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 5), BigDecimal.valueOf(2.00),
                false, "Trip", manager, "MANAGER_APPROVAL");
        request.setId(99L);

        when(leaveRequestRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(request));
        when(departmentRepository.findByIdWithLock(1L)).thenReturn(Optional.of(dept));

        WorkforceConflictReport report = new WorkforceConflictReport();
        report.setHasConflict(true);
        report.setBlockingEnabled(true);
        report.setSeverity(ConflictSeverity.CRITICAL_STAFFING_SHORTAGE);
        report.setSummaryMessage("Strict limit breached");
        when(leaveConflictEngineService.evaluateLeaveRequestConflicts(request)).thenReturn(report);

        assertThrows(StaffingShortageException.class, () ->
                leaveRequestService.approveLeaveRequest(99L, 2L, "Approved", false, null));
    }

    @Test
    void testApproveLeaveRequest_AdvisoryShortage_WithoutOverride_ThrowsStaffingShortageException() {
        LeaveRequest request = new LeaveRequest("LR-2026-999", employee, casualLeave,
                LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 5), BigDecimal.valueOf(2.00),
                false, "Trip", manager, "MANAGER_APPROVAL");
        request.setId(99L);

        when(leaveRequestRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(request));
        when(departmentRepository.findByIdWithLock(1L)).thenReturn(Optional.of(dept));

        WorkforceConflictReport report = new WorkforceConflictReport();
        report.setHasConflict(true);
        report.setBlockingEnabled(false);
        report.setSeverity(ConflictSeverity.CRITICAL_STAFFING_SHORTAGE);
        report.setSummaryMessage("Advisory shortage");
        when(leaveConflictEngineService.evaluateLeaveRequestConflicts(request)).thenReturn(report);

        assertThrows(StaffingShortageException.class, () ->
                leaveRequestService.approveLeaveRequest(99L, 2L, "Approved", false, null));
    }

    @Test
    void testApproveLeaveRequest_AdvisoryShortage_WithOverride_Succeeds() {
        LeaveRequest request = new LeaveRequest("LR-2026-999", employee, casualLeave,
                LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 5), BigDecimal.valueOf(2.00),
                false, "Trip", manager, "MANAGER_APPROVAL");
        request.setId(99L);

        when(leaveRequestRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(request));
        when(departmentRepository.findByIdWithLock(1L)).thenReturn(Optional.of(dept));

        WorkforceConflictReport report = new WorkforceConflictReport();
        report.setHasConflict(true);
        report.setBlockingEnabled(false);
        report.setSeverity(ConflictSeverity.CRITICAL_STAFFING_SHORTAGE);
        when(leaveConflictEngineService.evaluateLeaveRequestConflicts(request)).thenReturn(report);

        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndLeaveYear(1L, 1L, 2026))
                .thenReturn(Optional.of(leaveBalance));
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(i -> i.getArgument(0));

        LeaveRequest approved = leaveRequestService.approveLeaveRequest(
                99L, 2L, "Approved with justification", true, "Cross-trained contractor covering");

        assertEquals(LeaveRequestStatus.APPROVED, approved.getStatus());
        assertEquals("Cross-trained contractor covering", approved.getStaffingOverrideJustification());
        assertNull(approved.getRejectionReason(), "Rejection reason must remain null on approved request");
        verify(leaveBalanceService).deductLeaveForApprovedRequest(
                eq(1L), eq(1L), eq(2026), eq(BigDecimal.valueOf(2.00)), eq("LR-2026-999"), anyString(), anyString());
    }

    @Test
    void testApproveLeaveRequest_AdvisoryShortage_WithOverrideMissingJustification_ThrowsStaffingShortageException() {
        LeaveRequest request = new LeaveRequest("LR-2026-999", employee, casualLeave,
                LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 5), BigDecimal.valueOf(2.00),
                false, "Trip", manager, "MANAGER_APPROVAL");
        request.setId(99L);

        when(leaveRequestRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(request));
        when(departmentRepository.findByIdWithLock(1L)).thenReturn(Optional.of(dept));

        WorkforceConflictReport report = new WorkforceConflictReport();
        report.setHasConflict(true);
        report.setBlockingEnabled(false);
        report.setSeverity(ConflictSeverity.CRITICAL_STAFFING_SHORTAGE);
        when(leaveConflictEngineService.evaluateLeaveRequestConflicts(request)).thenReturn(report);

        StaffingShortageException ex = assertThrows(StaffingShortageException.class, () ->
                leaveRequestService.approveLeaveRequest(99L, 2L, "Approved", true, "   "));
        assertTrue(ex.getMessage().contains("override justification is required"));
    }

    @Test
    void testRejectLeaveRequest_Success_NoDeduction() {
        LeaveRequest request = new LeaveRequest("LR-2026-999", employee, casualLeave,
                LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 5), BigDecimal.valueOf(2.00),
                false, "Doctor appointment", manager, "MANAGER_APPROVAL");
        request.setId(99L);

        when(leaveRequestRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(request));
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(i -> i.getArgument(0));

        LeaveRequest rejected = leaveRequestService.rejectLeaveRequest(99L, 2L, "Project release deadline", "Please reapply next week");

        assertEquals(LeaveRequestStatus.REJECTED, rejected.getStatus());
        assertEquals("Project release deadline", rejected.getRejectionReason());
        assertNull(rejected.getStaffingOverrideJustification(), "Override justification must be null on rejected request");
        assertEquals(manager, rejected.getDecidedBy());
        assertNotNull(rejected.getDecisionTimestamp());

        // Verify no deduction was invoked
        verify(leaveBalanceService, never()).deductLeaveForApprovedRequest(anyLong(), anyLong(), anyInt(), any(), anyString(), anyString());
    }

    @Test
    void testRejectLeaveRequest_BlankRejectionReasonThrows() {
        LeaveRequest request = new LeaveRequest("LR-2026-999", employee, casualLeave,
                LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 5), BigDecimal.valueOf(2.00),
                false, "Doctor appointment", manager, "MANAGER_APPROVAL");
        request.setId(99L);

        when(leaveRequestRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(request));

        InvalidLeaveRequestException ex = assertThrows(InvalidLeaveRequestException.class, () ->
                leaveRequestService.rejectLeaveRequest(99L, 2L, "   ", null));
        assertTrue(ex.getMessage().contains("Rejection reason is mandatory"));
    }

    @Test
    void testCancelLeaveRequest_Pending_NoBalanceReversal() {
        LeaveRequest request = new LeaveRequest("LR-2026-999", employee, casualLeave,
                LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 5), BigDecimal.valueOf(2.00),
                false, "Doctor appointment", manager, "MANAGER_APPROVAL");
        request.setId(99L);

        when(leaveRequestRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(request));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(i -> i.getArgument(0));

        LeaveRequest cancelled = leaveRequestService.cancelLeaveRequest(99L, "Meeting rescheduled", "John Developer");

        assertEquals(LeaveRequestStatus.CANCELLED, cancelled.getStatus());
        assertEquals("Meeting rescheduled", cancelled.getCancellationReason());
        assertNotNull(cancelled.getCancelledTimestamp());

        verify(leaveBalanceService, never()).reverseLeaveForCancelledRequest(anyLong(), anyLong(), anyInt(), any(), anyString(), anyString(), anyString());
    }

    @Test
    void testCancelLeaveRequest_Approved_TriggersReversal() {
        LeaveRequest request = new LeaveRequest("LR-2026-999", employee, casualLeave,
                LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 5), BigDecimal.valueOf(2.00),
                false, "Doctor appointment", manager, "MANAGER_APPROVAL");
        request.setId(99L);
        request.setStatus(LeaveRequestStatus.APPROVED);

        when(leaveRequestRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(request));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(i -> i.getArgument(0));

        LeaveRequest cancelled = leaveRequestService.cancelLeaveRequest(99L, "Client cancelled trip", "John Developer");

        assertEquals(LeaveRequestStatus.CANCELLED, cancelled.getStatus());
        verify(leaveBalanceService).reverseLeaveForCancelledRequest(
                eq(1L), eq(1L), eq(2026), eq(BigDecimal.valueOf(2.00)), eq("LR-2026-999"), anyString(), anyString());
    }

    @Test
    void testSubmitLeaveRequest_StartDateBeforeJoiningDateThrows() {
        employee.setJoiningDate(LocalDate.of(2026, 6, 1));
        LeaveRequestDTO dto = new LeaveRequestDTO(1L, 1L,
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 2), false, "Prior date");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(casualLeave));

        assertThrows(InvalidLeaveRequestException.class, () ->
                leaveRequestService.submitLeaveRequest(dto));
    }

    @Test
    void testSubmitLeaveRequest_HalfDayAcrossMultipleDatesThrows() {
        LeaveRequestDTO dto = new LeaveRequestDTO(1L, 1L,
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 2), true, "Half day across 2 days");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(casualLeave));

        assertThrows(InvalidLeaveRequestException.class, () ->
                leaveRequestService.submitLeaveRequest(dto));
    }

    @Test
    void testSubmitLeaveRequest_HRStaffExcludesSelfWhenNoManager() {
        // HR staff member submitting a leave request with no reporting manager
        hrStaff.setReportingManager(null);
        LeaveRequestDTO dto = new LeaveRequestDTO(3L, 1L,
                LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 4), false, "HR taking leave");

        Employee secondHr = new Employee("HR02", "Carol HR Lead", "carol@example.com", hrDept,
                EmploymentType.FULL_TIME, LocalDate.of(2020, 1, 1), EmploymentStatus.ACTIVE, null);
        secondHr.setId(4L);

        when(employeeRepository.findById(3L)).thenReturn(Optional.of(hrStaff));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(casualLeave));
        when(leaveBalanceService.resolveApplicablePolicy(hrStaff, casualLeave, 2026)).thenReturn(standardPolicy);
        when(holidayService.isHoliday(any())).thenReturn(false);
        when(leaveRequestRepository.findOverlappingRequests(eq(3L), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndLeaveYear(3L, 1L, 2026))
                .thenReturn(Optional.of(leaveBalance));
        // Both hrStaff and secondHr returned from query
        when(employeeRepository.findActiveHrEmployees()).thenReturn(List.of(hrStaff, secondHr));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(i -> i.getArgument(0));

        LeaveRequest result = leaveRequestService.submitLeaveRequest(dto);

        // Assigned approver must be secondHr, NOT hrStaff themselves
        assertEquals(secondHr, result.getAssignedApprover());
        assertEquals("HR_APPROVAL", result.getApprovalRoute());
    }

    @Test
    void testApproveLeaveRequest_SelfApprovalForbiddenThrows() {
        LeaveRequest request = new LeaveRequest("LR-2026-999", employee, casualLeave,
                LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 5), BigDecimal.valueOf(2.00),
                false, "Doctor appointment", employee, "MANAGER_APPROVAL");
        request.setId(99L);

        when(leaveRequestRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(request));
        when(departmentRepository.findByIdWithLock(1L)).thenReturn(Optional.of(dept));
        when(leaveConflictEngineService.evaluateLeaveRequestConflicts(request)).thenReturn(new WorkforceConflictReport());
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndLeaveYear(1L, 1L, 2026))
                .thenReturn(Optional.of(leaveBalance));
        // Employee attempts to approve their own request
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        assertThrows(InvalidLeaveRequestException.class, () ->
                leaveRequestService.approveLeaveRequest(99L, 1L, "Self approving"));
    }

    @Test
    void testRejectLeaveRequest_SelfRejectionForbiddenThrows() {
        LeaveRequest request = new LeaveRequest("LR-2026-999", employee, casualLeave,
                LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 5), BigDecimal.valueOf(2.00),
                false, "Doctor appointment", employee, "MANAGER_APPROVAL");
        request.setId(99L);

        when(leaveRequestRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(request));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        assertThrows(InvalidLeaveRequestException.class, () ->
                leaveRequestService.rejectLeaveRequest(99L, 1L, "Rejecting self", "Self rejecting"));
    }
}

