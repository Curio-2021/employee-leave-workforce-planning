package com.anudip.employee_leave.service;

import com.anudip.employee_leave.dto.LeaveAdjustmentDTO;
import com.anudip.employee_leave.dto.LeaveInitializationDTO;
import com.anudip.employee_leave.entity.Employee;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class LeaveBalanceService {

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveTransactionRepository leaveTransactionRepository;
    private final LeavePolicyRepository leavePolicyRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final AuditService auditService;

    public LeaveBalanceService(LeaveBalanceRepository leaveBalanceRepository,
                               LeaveTransactionRepository leaveTransactionRepository,
                               LeavePolicyRepository leavePolicyRepository,
                               EmployeeRepository employeeRepository,
                               LeaveTypeRepository leaveTypeRepository) {
        this(leaveBalanceRepository, leaveTransactionRepository, leavePolicyRepository,
                employeeRepository, leaveTypeRepository, null);
    }

    @Autowired
    public LeaveBalanceService(LeaveBalanceRepository leaveBalanceRepository,
                               LeaveTransactionRepository leaveTransactionRepository,
                               LeavePolicyRepository leavePolicyRepository,
                               EmployeeRepository employeeRepository,
                               LeaveTypeRepository leaveTypeRepository,
                               AuditService auditService) {
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.leaveTransactionRepository = leaveTransactionRepository;
        this.leavePolicyRepository = leavePolicyRepository;
        this.employeeRepository = employeeRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<LeaveBalance> getFilteredBalances(Long employeeId, Long departmentId, Long leaveTypeId, Integer leaveYear) {
        return leaveBalanceRepository.findFiltered(employeeId, departmentId, leaveTypeId, leaveYear);
    }

    @Transactional(readOnly = true)
    public LeaveBalance getBalanceById(Long id) {
        return leaveBalanceRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave balance record not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<LeaveTransaction> getTransactionsForBalance(Long balanceId) {
        return leaveTransactionRepository.findByLeaveBalanceIdOrderByTransactionTimestampDesc(balanceId);
    }

    @Transactional(readOnly = true)
    public List<LeaveTransaction> getFilteredTransactions(Long employeeId, Long leaveTypeId, Integer leaveYear) {
        return leaveTransactionRepository.findFiltered(employeeId, leaveTypeId, leaveYear);
    }

    @Transactional(readOnly = true)
    public LeavePolicy resolveApplicablePolicy(Employee employee, LeaveType leaveType, int year) {
        List<LeavePolicy> policiesForYear = leavePolicyRepository.findByEffectiveYear(year);

        List<LeavePolicy> matchingPolicies = new ArrayList<>();
        for (LeavePolicy policy : policiesForYear) {
            if (!policy.isActive()) {
                continue;
            }
            if (!policy.getLeaveType().getId().equals(leaveType.getId())) {
                continue;
            }
            // Department scope check
            boolean matchesDept = policy.getApplicableDepartment() == null ||
                    (employee.getDepartment() != null && policy.getApplicableDepartment().getId().equals(employee.getDepartment().getId()));
            if (!matchesDept) {
                continue;
            }
            // Employment type check
            boolean matchesType = policy.getApplicableEmploymentType() == null ||
                    policy.getApplicableEmploymentType() == employee.getEmploymentType();
            if (!matchesType) {
                continue;
            }
            matchingPolicies.add(policy);
        }

        if (matchingPolicies.isEmpty()) {
            throw new ResourceNotFoundException("No active leave policy found for '" + leaveType.getName() +
                    "' applicable to " + employee.getFullName() + " (" +
                    (employee.getDepartment() != null ? employee.getDepartment().getName() : "No Dept") +
                    ", " + employee.getEmploymentType().getDisplayName() + ") for year " + year + ".");
        }

        if (matchingPolicies.size() == 1) {
            return matchingPolicies.get(0);
        }

        // More than one policy matches: calculate specificity scores
        // Specificity: specific dept = +2, specific employmentType = +1, global = 0
        int maxScore = -1;
        List<LeavePolicy> bestPolicies = new ArrayList<>();
        for (LeavePolicy p : matchingPolicies) {
            int score = (p.getApplicableDepartment() != null ? 2 : 0) +
                    (p.getApplicableEmploymentType() != null ? 1 : 0);
            if (score > maxScore) {
                maxScore = score;
                bestPolicies.clear();
                bestPolicies.add(p);
            } else if (score == maxScore) {
                bestPolicies.add(p);
            }
        }

        if (bestPolicies.size() > 1) {
            throw new PolicyConflictException("Multiple conflicting policies (" + bestPolicies.size() +
                    ") found with equal specificity for " + leaveType.getName() +
                    ". Please refine department or employment type policy definitions.");
        }

        return bestPolicies.get(0);
    }

    @Transactional
    public List<LeaveBalance> initializeEntitlements(LeaveInitializationDTO dto) {
        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + dto.getEmployeeId()));

        List<LeaveType> targetLeaveTypes;
        if (dto.getLeaveTypeId() != null) {
            LeaveType lt = leaveTypeRepository.findById(dto.getLeaveTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Leave type not found with ID: " + dto.getLeaveTypeId()));
            targetLeaveTypes = List.of(lt);
        } else {
            targetLeaveTypes = leaveTypeRepository.findByActiveTrueOrderByNameAsc();
        }

        int year = dto.getLeaveYear();
        String actor = dto.getActor() != null ? dto.getActor() : "HR_ADMIN";
        List<LeaveBalance> initializedBalances = new ArrayList<>();

        for (LeaveType leaveType : targetLeaveTypes) {
            LeavePolicy policy = resolveApplicablePolicy(employee, leaveType, year);

            LeaveBalance balance = leaveBalanceRepository
                    .findByEmployeeIdAndLeaveTypeIdAndLeaveYear(employee.getId(), leaveType.getId(), year)
                    .orElseGet(() -> {
                        LeaveBalance newBalance = new LeaveBalance(employee, leaveType, policy, year);
                        return leaveBalanceRepository.save(newBalance);
                    });

            balance.setApplicablePolicy(policy);

            // 1. Grant Entitlement (Idempotent)
            String grantIdempotencyKey = "GRANT-EMP_" + employee.getId() + "-LT_" + leaveType.getId() + "-YR_" + year;
            if (!leaveTransactionRepository.existsByIdempotencyKey(grantIdempotencyKey)) {
                BigDecimal grantAmount = BigDecimal.valueOf(policy.getAnnualAllocation()).setScale(2, RoundingMode.HALF_UP);
                BigDecimal prevBal = balance.getAvailableBalance();

                balance.setGrantedEntitlement(grantAmount);
                balance.recalculateBalance();
                leaveBalanceRepository.save(balance);

                LeaveTransaction grantTx = new LeaveTransaction(
                        balance, employee, leaveType, year,
                        LeaveTransactionType.ENTITLEMENT_GRANT,
                        grantAmount, prevBal, balance.getAvailableBalance(),
                        "Annual entitlement grant via policy: " + policy.getPolicyName(),
                        "POLICY-" + policy.getId(),
                        grantIdempotencyKey,
                        actor
                );
                leaveTransactionRepository.save(grantTx);
            }

            // 2. Carry Forward (Idempotent)
            if (dto.isIncludeCarryForward()) {
                String cfIdempotencyKey = "CF-EMP_" + employee.getId() + "-LT_" + leaveType.getId() + "-YR_" + year;
                if (!leaveTransactionRepository.existsByIdempotencyKey(cfIdempotencyKey)) {
                    Optional<LeaveBalance> prevYearBalOpt = leaveBalanceRepository
                            .findByEmployeeIdAndLeaveTypeIdAndLeaveYear(employee.getId(), leaveType.getId(), year - 1);

                    if (prevYearBalOpt.isPresent() && prevYearBalOpt.get().getAvailableBalance().compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal prevAvailable = prevYearBalOpt.get().getAvailableBalance();

                        if (policy.isAllowCarryForward()) {
                            BigDecimal maxCarry = BigDecimal.valueOf(policy.getMaxCarryForwardDays()).setScale(2, RoundingMode.HALF_UP);
                            BigDecimal actualCarry = prevAvailable.min(maxCarry);

                            if (actualCarry.compareTo(BigDecimal.ZERO) > 0) {
                                BigDecimal prevBal = balance.getAvailableBalance();
                                balance.setCarriedForward(actualCarry);
                                balance.recalculateBalance();
                                leaveBalanceRepository.save(balance);

                                LeaveTransaction cfTx = new LeaveTransaction(
                                        balance, employee, leaveType, year,
                                        LeaveTransactionType.CARRY_FORWARD,
                                        actualCarry, prevBal, balance.getAvailableBalance(),
                                        "Carried forward from " + (year - 1) + " (Available was: " + prevAvailable + ", Capped at max: " + maxCarry + ")",
                                        "CF-" + (year - 1),
                                        cfIdempotencyKey,
                                        actor
                                );
                                leaveTransactionRepository.save(cfTx);
                            }
                        }
                    }
                }
            }

            initializedBalances.add(balance);
        }

        if (auditService != null && !initializedBalances.isEmpty()) {
            Long deptId = employee.getDepartment() != null ? employee.getDepartment().getId() : null;
            auditService.recordEvent(dto.getActor() != null ? dto.getActor() : "HR_ADMIN",
                    "BALANCE_INITIALIZED", "LeaveBalance", "EMP-" + employee.getId(), deptId,
                    "Initialized " + initializedBalances.size() + " leave balance(s) for " +
                    employee.getFullName() + " for year " + dto.getLeaveYear());
        }

        return initializedBalances;
    }

    @Transactional
    public LeaveBalance applyAdjustment(LeaveAdjustmentDTO dto) {
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Adjustment amount must be greater than zero.");
        }

        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + dto.getEmployeeId()));

        LeaveType leaveType = leaveTypeRepository.findById(dto.getLeaveTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Leave type not found with ID: " + dto.getLeaveTypeId()));

        int year = dto.getLeaveYear();
        String actor = dto.getActor() != null ? dto.getActor() : "HR_ADMIN";

        LeaveBalance balance = leaveBalanceRepository
                .findByEmployeeIdAndLeaveTypeIdAndLeaveYear(employee.getId(), leaveType.getId(), year)
                .orElseGet(() -> {
                    LeaveBalance nb = new LeaveBalance(employee, leaveType, null, year);
                    return leaveBalanceRepository.save(nb);
                });

        BigDecimal prevBalance = balance.getAvailableBalance();
        BigDecimal amount = dto.getAmount().setScale(2, RoundingMode.HALF_UP);
        LeaveTransactionType txType;
        String idempotencyKey = "ADJ-" + UUID.randomUUID();

        if ("CREDIT".equalsIgnoreCase(dto.getAdjustmentType())) {
            txType = LeaveTransactionType.MANUAL_ADJUSTMENT_CREDIT;
            balance.setAdjustmentCredits(balance.getAdjustmentCredits().add(amount));
        } else if ("DEBIT".equalsIgnoreCase(dto.getAdjustmentType())) {
            txType = LeaveTransactionType.MANUAL_ADJUSTMENT_DEBIT;
            if (balance.getAvailableBalance().compareTo(amount) < 0) {
                throw new InsufficientLeaveBalanceException(
                        "Cannot debit " + amount + " days. Current available balance is only " +
                                balance.getAvailableBalance() + " days.");
            }
            balance.setAdjustmentDebits(balance.getAdjustmentDebits().add(amount));
        } else {
            throw new IllegalArgumentException("Invalid adjustment type: " + dto.getAdjustmentType() + ". Must be CREDIT or DEBIT.");
        }

        balance.recalculateBalance();
        LeaveBalance savedBalance = leaveBalanceRepository.save(balance);

        LeaveTransaction tx = new LeaveTransaction(
                savedBalance, employee, leaveType, year,
                txType, amount, prevBalance, savedBalance.getAvailableBalance(),
                dto.getReason(),
                dto.getReferenceNumber(),
                idempotencyKey,
                actor
        );
        leaveTransactionRepository.save(tx);

        if (auditService != null) {
            Long deptId = employee.getDepartment() != null ? employee.getDepartment().getId() : null;
            auditService.recordEvent(actor, "BALANCE_MANUALLY_ADJUSTED", "LeaveBalance", "BAL-" + savedBalance.getId(), deptId,
                    "Manual " + dto.getAdjustmentType().toUpperCase() + " of " + amount + " day(s) for " +
                    employee.getFullName() + " (" + leaveType.getName() + "). Reason: " + dto.getReason());
        }

        return savedBalance;
    }

    /**
     * Dedicated transactional and idempotent method prepared for approved leave requests.
     * Prevents double deductions, validates available balance, and appends an audit ledger record.
     */
    @Transactional
    public LeaveBalance deductLeaveForApprovedRequest(Long employeeId, Long leaveTypeId, int leaveYear,
                                                     BigDecimal daysToDeduct, String leaveRequestReference, String reason) {
        return deductLeaveForApprovedRequest(employeeId, leaveTypeId, leaveYear, daysToDeduct, leaveRequestReference, reason, "WORKFLOW_ENGINE");
    }

    @Transactional
    public LeaveBalance deductLeaveForApprovedRequest(Long employeeId, Long leaveTypeId, int leaveYear,
                                                     BigDecimal daysToDeduct, String leaveRequestReference, String reason, String actor) {
        String idempotencyKey = "LEAVE-DEDUCTION-" + leaveRequestReference;

        // Idempotency guard: if already processed for this reference, return current state without re-deducting
        if (leaveTransactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            return leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndLeaveYear(employeeId, leaveTypeId, leaveYear)
                    .orElseThrow(() -> new ResourceNotFoundException("Balance not found for employee " + employeeId));
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeeId));

        LeaveType leaveType = leaveTypeRepository.findById(leaveTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave type not found with ID: " + leaveTypeId));

        LeaveBalance balance = leaveBalanceRepository
                .findByEmployeeIdAndLeaveTypeIdAndLeaveYear(employeeId, leaveTypeId, leaveYear)
                .orElseThrow(() -> new ResourceNotFoundException("No leave balance record exists for " +
                        employee.getFullName() + " for " + leaveType.getName() + " in year " + leaveYear));

        BigDecimal prevBalance = balance.getAvailableBalance();
        BigDecimal deductDays = daysToDeduct.setScale(2, RoundingMode.HALF_UP);

        if (prevBalance.compareTo(deductDays) < 0) {
            throw new InsufficientLeaveBalanceException("Insufficient leave balance for deduction. Available: " +
                    prevBalance + " days, Requested: " + deductDays + " days.");
        }

        balance.setUsedLeave(balance.getUsedLeave().add(deductDays));
        balance.recalculateBalance();
        LeaveBalance savedBalance = leaveBalanceRepository.save(balance);

        LeaveTransaction deductionTx = new LeaveTransaction(
                savedBalance, employee, leaveType, leaveYear,
                LeaveTransactionType.APPROVED_LEAVE_DEDUCTION,
                deductDays, prevBalance, savedBalance.getAvailableBalance(),
                reason != null ? reason : "Leave deduction for request " + leaveRequestReference,
                leaveRequestReference,
                idempotencyKey,
                actor != null ? actor : "WORKFLOW_ENGINE"
        );
        leaveTransactionRepository.save(deductionTx);

        return savedBalance;
    }

    /**
     * Reverses deductions if an approved leave request is cancelled.
     * Prevents double reversal via unique idempotency key.
     */
    @Transactional
    public LeaveBalance reverseLeaveForCancelledRequest(Long employeeId, Long leaveTypeId, int leaveYear,
                                                       BigDecimal daysToReverse, String leaveRequestReference, String reason, String actor) {
        String idempotencyKey = "LEAVE-REVERSAL-" + leaveRequestReference;

        if (leaveTransactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            return leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndLeaveYear(employeeId, leaveTypeId, leaveYear)
                    .orElseThrow(() -> new ResourceNotFoundException("Balance not found for employee " + employeeId));
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeeId));

        LeaveType leaveType = leaveTypeRepository.findById(leaveTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave type not found with ID: " + leaveTypeId));

        LeaveBalance balance = leaveBalanceRepository
                .findByEmployeeIdAndLeaveTypeIdAndLeaveYear(employeeId, leaveTypeId, leaveYear)
                .orElseThrow(() -> new ResourceNotFoundException("No leave balance record exists for " +
                        employee.getFullName() + " for " + leaveType.getName() + " in year " + leaveYear));

        BigDecimal prevBalance = balance.getAvailableBalance();
        BigDecimal reverseDays = daysToReverse.setScale(2, RoundingMode.HALF_UP);

        BigDecimal newUsed = balance.getUsedLeave().subtract(reverseDays);
        if (newUsed.compareTo(BigDecimal.ZERO) < 0) {
            newUsed = BigDecimal.ZERO;
        }
        balance.setUsedLeave(newUsed);
        balance.recalculateBalance();
        LeaveBalance savedBalance = leaveBalanceRepository.save(balance);

        LeaveTransaction reversalTx = new LeaveTransaction(
                savedBalance, employee, leaveType, leaveYear,
                LeaveTransactionType.REVERSAL_CORRECTION,
                reverseDays, prevBalance, savedBalance.getAvailableBalance(),
                reason != null ? reason : "Leave reversal for cancelled request " + leaveRequestReference,
                leaveRequestReference,
                idempotencyKey,
                actor != null ? actor : "WORKFLOW_ENGINE"
        );
        leaveTransactionRepository.save(reversalTx);

        return savedBalance;
    }
}

