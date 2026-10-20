package com.anudip.employee_leave.service;

import com.anudip.employee_leave.dto.PolicyExceptionItem;
import com.anudip.employee_leave.dto.WorkforceConflictReport;
import com.anudip.employee_leave.entity.LeaveRequest;
import com.anudip.employee_leave.entity.LeaveRequestStatus;
import com.anudip.employee_leave.entity.LeaveTransaction;
import com.anudip.employee_leave.entity.LeaveTransactionType;
import com.anudip.employee_leave.repository.LeaveRequestRepository;
import com.anudip.employee_leave.repository.LeaveTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PolicyExceptionService {

    public static final String CAT_AUTHORIZED_OVERRIDE = "AUTHORIZED_OVERRIDE";
    public static final String CAT_PENDING_SHORTAGE_RISK = "PENDING_SHORTAGE_RISK";
    public static final String CAT_ADMINISTRATIVE_ADJUSTMENT = "ADMINISTRATIVE_ADJUSTMENT";

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveTransactionRepository leaveTransactionRepository;
    private final LeaveConflictEngineService leaveConflictEngineService;

    public PolicyExceptionService(LeaveRequestRepository leaveRequestRepository,
                                  LeaveTransactionRepository leaveTransactionRepository,
                                  LeaveConflictEngineService leaveConflictEngineService) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveTransactionRepository = leaveTransactionRepository;
        this.leaveConflictEngineService = leaveConflictEngineService;
    }

    @Transactional(readOnly = true)
    public List<PolicyExceptionItem> getPolicyExceptions(String categoryFilter, Long departmentId, Integer year) {
        List<PolicyExceptionItem> items = new ArrayList<>();

        boolean includeOverrides = categoryFilter == null || categoryFilter.isBlank() || CAT_AUTHORIZED_OVERRIDE.equalsIgnoreCase(categoryFilter);
        boolean includeRisks = categoryFilter == null || categoryFilter.isBlank() || CAT_PENDING_SHORTAGE_RISK.equalsIgnoreCase(categoryFilter);
        boolean includeAdjustments = categoryFilter == null || categoryFilter.isBlank() || CAT_ADMINISTRATIVE_ADJUSTMENT.equalsIgnoreCase(categoryFilter);

        // 1. Authorized Overrides
        if (includeOverrides) {
            List<LeaveRequest> overrides = leaveRequestRepository.findStaffingOverridesWithDetails();
            for (LeaveRequest req : overrides) {
                if (departmentId != null && (req.getEmployee() == null || req.getEmployee().getDepartment() == null ||
                        !departmentId.equals(req.getEmployee().getDepartment().getId()))) {
                    continue;
                }
                if (year != null && req.getStartDate().getYear() != year) {
                    continue;
                }

                String deptName = (req.getEmployee() != null && req.getEmployee().getDepartment() != null)
                        ? req.getEmployee().getDepartment().getName() : "Unassigned";
                Long deptId = (req.getEmployee() != null && req.getEmployee().getDepartment() != null)
                        ? req.getEmployee().getDepartment().getId() : null;

                String empName = req.getEmployee() != null ? req.getEmployee().getFullName() : "Unknown";
                String empCode = req.getEmployee() != null ? req.getEmployee().getEmployeeId() : "—";

                String details = String.format("Approved %s leave for %.1f day(s) (%s to %s) despite advisory staffing conflict.",
                        req.getLeaveType() != null ? req.getLeaveType().getName() : "Leave",
                        req.getDuration(), req.getStartDate(), req.getEndDate());

                PolicyExceptionItem item = new PolicyExceptionItem(
                        "OVR-" + req.getId(),
                        CAT_AUTHORIZED_OVERRIDE,
                        "Authorized Staffing Override",
                        "WARNING",
                        req.getDecisionTimestamp() != null ? req.getDecisionTimestamp() : req.getSubmissionTimestamp(),
                        req.getRequestNumber(),
                        empName,
                        empCode,
                        deptName,
                        deptId,
                        details,
                        req.getStaffingOverrideJustification(),
                        "/leave-requests/" + req.getId()
                );
                items.add(item);
            }
        }

        // 2. Pending Shortage Risks
        if (includeRisks) {
            List<LeaveRequest> pendingRequests = leaveRequestRepository.findByStatusOrderBySubmissionTimestampAsc(LeaveRequestStatus.PENDING);
            for (LeaveRequest req : pendingRequests) {
                if (departmentId != null && (req.getEmployee() == null || req.getEmployee().getDepartment() == null ||
                        !departmentId.equals(req.getEmployee().getDepartment().getId()))) {
                    continue;
                }
                if (year != null && req.getStartDate().getYear() != year) {
                    continue;
                }

                WorkforceConflictReport report = leaveConflictEngineService.evaluateLeaveRequestConflicts(req);
                if (report.isHasConflict()) {
                    String deptName = (req.getEmployee() != null && req.getEmployee().getDepartment() != null)
                            ? req.getEmployee().getDepartment().getName() : "Unassigned";
                    Long deptId = (req.getEmployee() != null && req.getEmployee().getDepartment() != null)
                            ? req.getEmployee().getDepartment().getId() : null;

                    String empName = req.getEmployee() != null ? req.getEmployee().getFullName() : "Unknown";
                    String empCode = req.getEmployee() != null ? req.getEmployee().getEmployeeId() : "—";

                    String severity = report.isBlockingEnabled() ? "CRITICAL" : "HIGH";

                    int affectedDaysCount = report.getAffectedDates() != null ? report.getAffectedDates().size() : 0;
                    PolicyExceptionItem item = new PolicyExceptionItem(
                            "RSK-" + req.getId(),
                            CAT_PENDING_SHORTAGE_RISK,
                            "Pending Shortage Risk",
                            severity,
                            req.getSubmissionTimestamp(),
                            req.getRequestNumber(),
                            empName,
                            empCode,
                            deptName,
                            deptId,
                            report.getSummaryMessage() + " (" + affectedDaysCount + " day(s) affected)",
                            "Awaiting manager review with conflict warning.",
                            "/leave-requests/" + req.getId()
                    );
                    items.add(item);
                }
            }
        }

        // 3. Administrative Manual Adjustments
        if (includeAdjustments) {
            List<LeaveTransactionType> manualTypes = List.of(
                    LeaveTransactionType.MANUAL_ADJUSTMENT_CREDIT,
                    LeaveTransactionType.MANUAL_ADJUSTMENT_DEBIT
            );
            List<LeaveTransaction> manualTxs = leaveTransactionRepository.findByTransactionTypeInWithDetails(manualTypes);
            for (LeaveTransaction tx : manualTxs) {
                if (departmentId != null && (tx.getEmployee() == null || tx.getEmployee().getDepartment() == null ||
                        !departmentId.equals(tx.getEmployee().getDepartment().getId()))) {
                    continue;
                }
                if (year != null && tx.getLeaveYear() != year) {
                    continue;
                }

                String deptName = (tx.getEmployee() != null && tx.getEmployee().getDepartment() != null)
                        ? tx.getEmployee().getDepartment().getName() : "Unassigned";
                Long deptId = (tx.getEmployee() != null && tx.getEmployee().getDepartment() != null)
                        ? tx.getEmployee().getDepartment().getId() : null;

                String empName = tx.getEmployee() != null ? tx.getEmployee().getFullName() : "Unknown";
                String empCode = tx.getEmployee() != null ? tx.getEmployee().getEmployeeId() : "—";

                double amt = tx.getAmount() != null ? tx.getAmount().doubleValue() : 0.0;
                double prevBal = tx.getPreviousBalance() != null ? tx.getPreviousBalance().doubleValue() : 0.0;
                double resBal = tx.getResultingBalance() != null ? tx.getResultingBalance().doubleValue() : 0.0;

                String details = String.format("Manual %s of %.1f day(s) for %s (Balance: %.1f -> %.1f)",
                        tx.getTransactionType().name(),
                        amt,
                        tx.getLeaveType() != null ? tx.getLeaveType().getName() : "Leave",
                        prevBal,
                        resBal);

                PolicyExceptionItem item = new PolicyExceptionItem(
                        "ADJ-" + tx.getId(),
                        CAT_ADMINISTRATIVE_ADJUSTMENT,
                        "Administrative Balance Adjustment",
                        "INFO",
                        tx.getTransactionTimestamp(),
                        "TX-" + tx.getId(),
                        empName,
                        empCode,
                        deptName,
                        deptId,
                        details,
                        tx.getReason() != null ? tx.getReason() : "No reason specified",
                        "/leave-balances/transactions"
                );
                items.add(item);
            }
        }

        // Sort descending by timestamp
        return items.stream()
                .sorted(Comparator.comparing(PolicyExceptionItem::getTimestamp, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getExceptionCounts() {
        List<PolicyExceptionItem> allItems = getPolicyExceptions(null, null, null);
        Map<String, Long> counts = new HashMap<>();
        counts.put("TOTAL", (long) allItems.size());
        counts.put(CAT_AUTHORIZED_OVERRIDE, allItems.stream().filter(i -> CAT_AUTHORIZED_OVERRIDE.equals(i.getCategory())).count());
        counts.put(CAT_PENDING_SHORTAGE_RISK, allItems.stream().filter(i -> CAT_PENDING_SHORTAGE_RISK.equals(i.getCategory())).count());
        counts.put(CAT_ADMINISTRATIVE_ADJUSTMENT, allItems.stream().filter(i -> CAT_ADMINISTRATIVE_ADJUSTMENT.equals(i.getCategory())).count());
        return counts;
    }
}
