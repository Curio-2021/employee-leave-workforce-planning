package com.anudip.employee_leave.service;

import com.anudip.employee_leave.dto.LeaveRequestDTO;
import com.anudip.employee_leave.entity.Department;
import com.anudip.employee_leave.entity.Employee;
import com.anudip.employee_leave.entity.EmploymentStatus;
import com.anudip.employee_leave.entity.LeaveBalance;
import com.anudip.employee_leave.entity.LeavePolicy;
import com.anudip.employee_leave.entity.LeaveRequest;
import com.anudip.employee_leave.entity.LeaveRequestStatus;
import com.anudip.employee_leave.entity.LeaveType;
import com.anudip.employee_leave.exception.InsufficientLeaveBalanceException;
import com.anudip.employee_leave.exception.InvalidLeaveRequestException;
import com.anudip.employee_leave.exception.ResourceNotFoundException;
import com.anudip.employee_leave.repository.DepartmentRepository;
import com.anudip.employee_leave.repository.EmployeeRepository;
import com.anudip.employee_leave.repository.LeaveBalanceRepository;
import com.anudip.employee_leave.repository.LeaveRequestRepository;
import com.anudip.employee_leave.repository.LeaveTypeRepository;
import com.anudip.employee_leave.dto.WorkforceConflictReport;
import com.anudip.employee_leave.entity.ConflictSeverity;
import com.anudip.employee_leave.exception.StaffingShortageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveBalanceService leaveBalanceService;
    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final DepartmentRepository departmentRepository;
    private final HolidayService holidayService;
    private final LeaveConflictEngineService leaveConflictEngineService;
    private final WorkingCalendarService workingCalendarService;
    private final AuditService auditService;

    public LeaveRequestService(LeaveRequestRepository leaveRequestRepository,
                               LeaveBalanceRepository leaveBalanceRepository,
                               LeaveBalanceService leaveBalanceService,
                               EmployeeRepository employeeRepository,
                               LeaveTypeRepository leaveTypeRepository,
                               DepartmentRepository departmentRepository,
                               HolidayService holidayService,
                               LeaveConflictEngineService leaveConflictEngineService,
                               WorkingCalendarService workingCalendarService) {
        this(leaveRequestRepository, leaveBalanceRepository, leaveBalanceService,
                employeeRepository, leaveTypeRepository, departmentRepository,
                holidayService, leaveConflictEngineService, workingCalendarService, null);
    }

    @Autowired
    public LeaveRequestService(LeaveRequestRepository leaveRequestRepository,
                               LeaveBalanceRepository leaveBalanceRepository,
                               LeaveBalanceService leaveBalanceService,
                               EmployeeRepository employeeRepository,
                               LeaveTypeRepository leaveTypeRepository,
                               DepartmentRepository departmentRepository,
                               HolidayService holidayService,
                               LeaveConflictEngineService leaveConflictEngineService,
                               WorkingCalendarService workingCalendarService,
                               AuditService auditService) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.leaveBalanceService = leaveBalanceService;
        this.employeeRepository = employeeRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.departmentRepository = departmentRepository;
        this.holidayService = holidayService;
        this.leaveConflictEngineService = leaveConflictEngineService;
        this.workingCalendarService = workingCalendarService;
        this.auditService = auditService;
    }

    /**
     * Calculates the net leave duration between two dates based on department working calendar.
     */
    public BigDecimal calculateDuration(Long departmentId, LocalDate startDate, LocalDate endDate, boolean halfDay) {
        return workingCalendarService.calculateDuration(departmentId, startDate, endDate, halfDay);
    }

    public BigDecimal calculateDuration(Department department, LocalDate startDate, LocalDate endDate, boolean halfDay) {
        return workingCalendarService.calculateDuration(department, startDate, endDate, halfDay);
    }

    public BigDecimal calculateDuration(Employee employee, LocalDate startDate, LocalDate endDate, boolean halfDay) {
        return workingCalendarService.calculateDuration(employee, startDate, endDate, halfDay);
    }

    public BigDecimal calculateDuration(LocalDate startDate, LocalDate endDate, boolean halfDay) {
        return workingCalendarService.calculateDuration((Long) null, startDate, endDate, halfDay);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequest> getFilteredRequests(Long employeeId, Long departmentId, Long leaveTypeId, LeaveRequestStatus status) {
        return leaveRequestRepository.findFiltered(employeeId, departmentId, leaveTypeId, status);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequest> getPendingRequests() {
        return leaveRequestRepository.findByStatusOrderBySubmissionTimestampAsc(LeaveRequestStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public LeaveRequest getRequestById(Long id) {
        return leaveRequestRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public long countPendingRequests() {
        return leaveRequestRepository.countByStatus(LeaveRequestStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<Employee> getAllActiveEmployees() {
        return employeeRepository.findFiltered(null, EmploymentStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<LeaveType> getAllActiveLeaveTypes() {
        return leaveTypeRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    /**
     * Submits a new leave request after rigorous validation against employee status,
     * leave type, policy constraints, holiday calendar, and available leave balance.
     * Note: Balance is NOT deducted at submission time.
     */
    @Transactional
    public LeaveRequest submitLeaveRequest(LeaveRequestDTO dto) {
        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + dto.getEmployeeId()));

        if (employee.getEmploymentStatus() != EmploymentStatus.ACTIVE) {
            throw new InvalidLeaveRequestException("Cannot submit leave request for inactive employee: " + employee.getFullName());
        }

        LeaveType leaveType = leaveTypeRepository.findById(dto.getLeaveTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Leave type not found with ID: " + dto.getLeaveTypeId()));

        if (!leaveType.isActive()) {
            throw new InvalidLeaveRequestException("Cannot submit request for inactive leave type: " + leaveType.getName());
        }

        if (dto.getStartDate() == null || dto.getEndDate() == null) {
            throw new InvalidLeaveRequestException("Start date and end date are required.");
        }

        if (dto.getStartDate().isAfter(dto.getEndDate())) {
            throw new InvalidLeaveRequestException("Start date (" + dto.getStartDate() + ") cannot be after end date (" + dto.getEndDate() + ").");
        }

        if (employee.getJoiningDate() != null && dto.getStartDate().isBefore(employee.getJoiningDate())) {
            throw new InvalidLeaveRequestException("Leave start date (" + dto.getStartDate() +
                    ") cannot be earlier than employee joining date (" + employee.getJoiningDate() + ").");
        }

        if (dto.isHalfDay() && !dto.getStartDate().equals(dto.getEndDate())) {
            throw new InvalidLeaveRequestException("Half-day leave can only be requested when Start Date and End Date are identical.");
        }

        if (dto.getStartDate().getYear() != dto.getEndDate().getYear()) {
            throw new InvalidLeaveRequestException("Leave requests cannot span across multiple calendar years. " +
                    "Please submit separate requests for each year to align with annual entitlements.");
        }

        int leaveYear = dto.getStartDate().getYear();

        // 1. Validate applicable active policy exists
        LeavePolicy policy = leaveBalanceService.resolveApplicablePolicy(employee, leaveType, leaveYear);

        // 2. Server-side duration calculation based on employee's department working calendar
        BigDecimal duration = calculateDuration(employee, dto.getStartDate(), dto.getEndDate(), dto.isHalfDay());
        if (duration.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidLeaveRequestException(
                    "Requested date range contains no working days for the employee's department (dates are non-working days or official holidays).");
        }

        // 3. Validate maximum consecutive days restriction
        if (leaveType.getMaxConsecutiveDays() != null && leaveType.getMaxConsecutiveDays() > 0) {
            if (duration.compareTo(BigDecimal.valueOf(leaveType.getMaxConsecutiveDays())) > 0) {
                throw new InvalidLeaveRequestException("Requested duration of " + duration + " days exceeds maximum allowed " +
                        leaveType.getMaxConsecutiveDays() + " consecutive days for " + leaveType.getName() + ".");
            }
        }

        // 4. Validate minimum service days requirement
        if (policy.getMinServiceDays() > 0 && employee.getJoiningDate() != null) {
            long serviceDays = ChronoUnit.DAYS.between(employee.getJoiningDate(), dto.getStartDate());
            if (serviceDays < policy.getMinServiceDays()) {
                throw new InvalidLeaveRequestException("Employee has " + serviceDays + " days of service (joined " +
                        employee.getJoiningDate() + "). The policy requires a minimum of " + policy.getMinServiceDays() + " service days.");
            }
        }

        // 5. Overlapping request conflict detection
        List<LeaveRequest> overlapping = leaveRequestRepository.findOverlappingRequests(
                employee.getId(), dto.getStartDate(), dto.getEndDate(),
                List.of(LeaveRequestStatus.PENDING, LeaveRequestStatus.APPROVED));

        if (!overlapping.isEmpty()) {
            LeaveRequest clash = overlapping.get(0);
            throw new InvalidLeaveRequestException("Overlapping leave request detected: Request " + clash.getRequestNumber() +
                    " (" + clash.getStartDate() + " to " + clash.getEndDate() + ") is currently " + clash.getStatus().getDisplayName() + ".");
        }

        // 6. Balance pre-validation: check sufficient balance exists
        LeaveBalance balance = leaveBalanceRepository
                .findByEmployeeIdAndLeaveTypeIdAndLeaveYear(employee.getId(), leaveType.getId(), leaveYear)
                .orElseThrow(() -> new InsufficientLeaveBalanceException(
                        "No leave balance record exists for " + employee.getFullName() + " for " + leaveType.getName() +
                                " in year " + leaveYear + ". Please initialize the entitlement before requesting leave."));

        if (balance.getAvailableBalance().compareTo(duration) < 0) {
            throw new InsufficientLeaveBalanceException("Insufficient leave balance. Available: " +
                    balance.getAvailableBalance() + " days, Requested: " + duration + " days.");
        }

        // 7. Approver resolution
        Employee assignedApprover = null;
        String approvalRoute;

        if (employee.getReportingManager() != null && employee.getReportingManager().getEmploymentStatus() == EmploymentStatus.ACTIVE) {
            assignedApprover = employee.getReportingManager();
            approvalRoute = "MANAGER_APPROVAL";
        } else {
            // Attempt to resolve active HR employee if manager is missing (excluding the employee themselves)
            List<Employee> hrStaff = employeeRepository.findActiveHrEmployees();
            for (Employee hr : hrStaff) {
                if (!hr.getId().equals(employee.getId())) {
                    assignedApprover = hr;
                    break;
                }
            }

            if (assignedApprover != null) {
                approvalRoute = "HR_APPROVAL";
            } else {
                throw new InvalidLeaveRequestException("Cannot route leave request: Employee " + employee.getFullName() +
                        " does not have an active reporting manager assigned, and no distinct active HR approver could be resolved. " +
                        "Please assign a reporting manager before submitting leave requests.");
            }
        }

        // 8. Generate unique request identifier
        String requestNumber = "LR-" + leaveYear + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        LeaveRequest request = new LeaveRequest(
                requestNumber, employee, leaveType,
                dto.getStartDate(), dto.getEndDate(), duration,
                dto.isHalfDay(), dto.getReason().trim(),
                assignedApprover, approvalRoute
        );

        LeaveRequest saved = leaveRequestRepository.save(request);
        if (auditService != null) {
            String actor = employee.getFullName() + " (" + employee.getEmployeeId() + ")";
            Long deptId = employee.getDepartment() != null ? employee.getDepartment().getId() : null;
            auditService.recordEvent(actor, "LEAVE_REQUEST_SUBMITTED", "LeaveRequest", saved.getRequestNumber(), deptId,
                    "Submitted leave request for " + saved.getDuration() + " working day(s) from " +
                    saved.getStartDate() + " to " + saved.getEndDate() + " (" + leaveType.getName() + ")");
        }
        return saved;
    }

    /**
     * Approves a pending leave request, revalidates balance sufficiency, and atomically
     * deducts the leave quantity from the employee's balance and appends to the audit ledger.
     */
    @Transactional
    public LeaveRequest approveLeaveRequest(Long requestId, Long actingApproverId, String comments) {
        return approveLeaveRequest(requestId, actingApproverId, comments, false, null);
    }

    /**
     * Approves a pending leave request with concurrency lock on the department and
     * real-time staffing conflict re-evaluation.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public LeaveRequest approveLeaveRequest(Long requestId, Long actingApproverId, String comments,
                                           boolean overrideConfirmed, String overrideJustification) {
        LeaveRequest request = getRequestById(requestId);

        if (request.getStatus() != LeaveRequestStatus.PENDING) {
            throw new InvalidLeaveRequestException("Cannot approve request with status '" + request.getStatus() +
                    "'. Only PENDING requests can be approved.");
        }

        // Concurrency Guard: Lock the department row to serialize concurrent approvals for this department
        String finalOverrideJustification = null;
        if (request.getEmployee().getDepartment() != null) {
            departmentRepository.findByIdWithLock(request.getEmployee().getDepartment().getId());

            // Re-evaluate staffing conflict in real-time under the serialized lock
            WorkforceConflictReport conflictReport = leaveConflictEngineService.evaluateLeaveRequestConflicts(request);
            if (conflictReport.getSeverity() == ConflictSeverity.CRITICAL_STAFFING_SHORTAGE) {
                if (conflictReport.isBlockingEnabled()) {
                    throw new StaffingShortageException(conflictReport.getSummaryMessage());
                } else {
                    if (!overrideConfirmed) {
                        throw new StaffingShortageException("Approval requires explicit override confirmation: " +
                                conflictReport.getSummaryMessage());
                    }
                    if (overrideJustification == null || overrideJustification.trim().isEmpty()) {
                        throw new StaffingShortageException("Managerial override justification is required when approving with an advisory staffing conflict.");
                    }
                    finalOverrideJustification = overrideJustification.trim();
                }
            }
        }

        int leaveYear = request.getStartDate().getYear();

        // Revalidate leave balance at approval time
        LeaveBalance balance = leaveBalanceRepository
                .findByEmployeeIdAndLeaveTypeIdAndLeaveYear(request.getEmployee().getId(), request.getLeaveType().getId(), leaveYear)
                .orElseThrow(() -> new InsufficientLeaveBalanceException("No leave balance found for year " + leaveYear));

        if (balance.getAvailableBalance().compareTo(request.getDuration()) < 0) {
            throw new InsufficientLeaveBalanceException("Cannot approve request. Available leave balance has changed to " +
                    balance.getAvailableBalance() + " days, which is insufficient for the requested " + request.getDuration() + " days.");
        }

        // Resolve acting approver
        Employee approver = null;
        if (actingApproverId != null) {
            approver = employeeRepository.findById(actingApproverId).orElse(null);
        }
        if (approver == null) {
            approver = request.getAssignedApprover();
        }

        if (approver != null && approver.getId().equals(request.getEmployee().getId())) {
            throw new InvalidLeaveRequestException("Self-approval is not permitted. Employee cannot approve their own leave request.");
        }

        String actorName = approver != null ? approver.getFullName() + " (" + approver.getEmployeeId() + ")" : "WORKFLOW_APPROVER";

        String auditReason = "Leave approval for " + request.getRequestNumber() + ": " + request.getReason();
        if (finalOverrideJustification != null) {
            auditReason += " | Staffing Override Justification: " + finalOverrideJustification;
        }

        // Atomically deduct leave balance and insert transaction ledger record
        leaveBalanceService.deductLeaveForApprovedRequest(
                request.getEmployee().getId(),
                request.getLeaveType().getId(),
                leaveYear,
                request.getDuration(),
                request.getRequestNumber(),
                auditReason,
                actorName
        );

        // Update request status to APPROVED with dedicated override justification
        request.approve(approver, comments, finalOverrideJustification);
        LeaveRequest saved = leaveRequestRepository.save(request);

        if (auditService != null) {
            Long deptId = request.getEmployee().getDepartment() != null ? request.getEmployee().getDepartment().getId() : null;
            if (finalOverrideJustification != null) {
                auditService.recordEvent(actorName, "LEAVE_REQUEST_OVERRIDDEN", "LeaveRequest", request.getRequestNumber(), deptId,
                        "Approved with advisory staffing override. Justification: " + finalOverrideJustification);
            } else {
                auditService.recordEvent(actorName, "LEAVE_REQUEST_APPROVED", "LeaveRequest", request.getRequestNumber(), deptId,
                        "Approved " + request.getDuration() + " day(s) leave request: " + request.getRequestNumber());
            }
        }
        return saved;
    }

    /**
     * Rejects a pending leave request with a mandatory rejection reason.
     * No leave balances are deducted.
     */
    @Transactional
    public LeaveRequest rejectLeaveRequest(Long requestId, Long actingApproverId, String rejectionReason, String comments) {
        LeaveRequest request = getRequestById(requestId);

        if (request.getStatus() != LeaveRequestStatus.PENDING) {
            throw new InvalidLeaveRequestException("Cannot reject request with status '" + request.getStatus() +
                    "'. Only PENDING requests can be rejected.");
        }

        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            throw new InvalidLeaveRequestException("Rejection reason is mandatory when rejecting a leave request.");
        }

        Employee approver = null;
        if (actingApproverId != null) {
            approver = employeeRepository.findById(actingApproverId).orElse(null);
        }
        if (approver == null) {
            approver = request.getAssignedApprover();
        }

        if (approver != null && approver.getId().equals(request.getEmployee().getId())) {
            throw new InvalidLeaveRequestException("Employee cannot reject their own leave request. Please use cancellation instead.");
        }

        request.reject(approver, rejectionReason, comments);
        LeaveRequest saved = leaveRequestRepository.save(request);

        if (auditService != null) {
            String actorName = approver != null ? approver.getFullName() + " (" + approver.getEmployeeId() + ")" : "WORKFLOW_APPROVER";
            Long deptId = request.getEmployee().getDepartment() != null ? request.getEmployee().getDepartment().getId() : null;
            auditService.recordEvent(actorName, "LEAVE_REQUEST_REJECTED", "LeaveRequest", request.getRequestNumber(), deptId,
                    "Rejected leave request " + request.getRequestNumber() + ". Reason: " + rejectionReason);
        }
        return saved;
    }

    /**
     * Cancels a pending or approved leave request.
     * If already approved, the previously deducted leave balance is atomically reversed back.
     */
    @Transactional
    public LeaveRequest cancelLeaveRequest(Long requestId, String cancellationReason, String actor) {
        LeaveRequest request = getRequestById(requestId);

        if (request.getStatus() != LeaveRequestStatus.PENDING && request.getStatus() != LeaveRequestStatus.APPROVED) {
            throw new InvalidLeaveRequestException("Cannot cancel request with status '" + request.getStatus() +
                    "'. Only PENDING or APPROVED requests can be cancelled.");
        }

        // If previously approved, reverse the deduction from the balance and ledger
        if (request.getStatus() == LeaveRequestStatus.APPROVED) {
            int leaveYear = request.getStartDate().getYear();
            leaveBalanceService.reverseLeaveForCancelledRequest(
                    request.getEmployee().getId(),
                    request.getLeaveType().getId(),
                    leaveYear,
                    request.getDuration(),
                    request.getRequestNumber(),
                    "Reversal for cancelled request " + request.getRequestNumber() + (cancellationReason != null ? ": " + cancellationReason : ""),
                    actor != null ? actor : "WORKFLOW_ENGINE"
            );
        }

        request.cancel(cancellationReason);
        LeaveRequest saved = leaveRequestRepository.save(request);

        if (auditService != null) {
            Long deptId = request.getEmployee().getDepartment() != null ? request.getEmployee().getDepartment().getId() : null;
            auditService.recordEvent(actor != null ? actor : "USER", "LEAVE_REQUEST_CANCELLED", "LeaveRequest", request.getRequestNumber(), deptId,
                    "Cancelled leave request " + request.getRequestNumber() + (cancellationReason != null ? ": " + cancellationReason : ""));
        }
        return saved;
    }
}

