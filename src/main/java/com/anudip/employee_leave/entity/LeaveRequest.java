package com.anudip.employee_leave.entity;

import com.anudip.employee_leave.exception.InvalidLeaveRequestException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_requests")
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Request number is required")
    @Size(max = 50, message = "Request number cannot exceed 50 characters")
    @Column(name = "request_number", nullable = false, unique = true, length = 50)
    private String requestNumber;

    @NotNull(message = "Employee is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @NotNull(message = "Leave type is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @NotNull(message = "Start date is required")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @NotNull(message = "Duration is required")
    @DecimalMin(value = "0.50", message = "Leave duration must be at least 0.50 days")
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal duration;

    @Column(name = "is_half_day", nullable = false)
    private boolean halfDay = false;

    @NotBlank(message = "Reason is required")
    @Size(min = 5, max = 500, message = "Reason must be between 5 and 500 characters")
    @Column(nullable = false, length = 500)
    private String reason;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeaveRequestStatus status = LeaveRequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_approver_id")
    private Employee assignedApprover;

    @Column(name = "approval_route", length = 50)
    private String approvalRoute;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by_id")
    private Employee decidedBy;

    @NotNull(message = "Submission timestamp is required")
    @Column(name = "submission_timestamp", nullable = false)
    private LocalDateTime submissionTimestamp;

    @Column(name = "decision_timestamp")
    private LocalDateTime decisionTimestamp;

    @Size(max = 500, message = "Approver comments cannot exceed 500 characters")
    @Column(name = "approver_comments", length = 500)
    private String approverComments;

    @Size(max = 500, message = "Rejection reason cannot exceed 500 characters")
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Size(max = 500, message = "Staffing override justification cannot exceed 500 characters")
    @Column(name = "staffing_override_justification", length = 500)
    private String staffingOverrideJustification;

    @Size(max = 500, message = "Cancellation reason cannot exceed 500 characters")
    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "cancelled_timestamp")
    private LocalDateTime cancelledTimestamp;

    @Version
    private Long version;

    public LeaveRequest() {
        this.submissionTimestamp = LocalDateTime.now();
    }

    public LeaveRequest(String requestNumber, Employee employee, LeaveType leaveType,
                        LocalDate startDate, LocalDate endDate, BigDecimal duration,
                        boolean halfDay, String reason, Employee assignedApprover,
                        String approvalRoute) {
        this();
        this.requestNumber = requestNumber;
        this.employee = employee;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.duration = duration;
        this.halfDay = halfDay;
        this.reason = reason;
        this.assignedApprover = assignedApprover;
        this.approvalRoute = approvalRoute;
        this.status = LeaveRequestStatus.PENDING;
    }

    /**
     * Enforces valid status transition to APPROVED.
     */
    public void approve(Employee approver, String comments) {
        approve(approver, comments, null);
    }

    /**
     * Enforces valid status transition to APPROVED with optional staffing override justification.
     */
    public void approve(Employee approver, String comments, String overrideJustification) {
        if (this.status != LeaveRequestStatus.PENDING) {
            throw new InvalidLeaveRequestException(
                    "Cannot approve leave request with status '" + this.status + "'. Only PENDING requests can be approved.");
        }
        this.status = LeaveRequestStatus.APPROVED;
        this.decidedBy = approver;
        this.approverComments = comments != null ? comments.trim() : null;
        this.staffingOverrideJustification = overrideJustification != null && !overrideJustification.trim().isEmpty()
                ? overrideJustification.trim() : null;
        this.rejectionReason = null;
        this.decisionTimestamp = LocalDateTime.now();
    }

    /**
     * Enforces valid status transition to REJECTED with mandatory reason.
     */
    public void reject(Employee approver, String rejectionReason, String comments) {
        if (this.status != LeaveRequestStatus.PENDING) {
            throw new InvalidLeaveRequestException(
                    "Cannot reject leave request with status '" + this.status + "'. Only PENDING requests can be rejected.");
        }
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            throw new InvalidLeaveRequestException("Rejection reason is mandatory when rejecting a leave request.");
        }
        this.status = LeaveRequestStatus.REJECTED;
        this.decidedBy = approver;
        this.rejectionReason = rejectionReason.trim();
        this.staffingOverrideJustification = null;
        this.approverComments = comments != null ? comments.trim() : null;
        this.decisionTimestamp = LocalDateTime.now();
    }

    /**
     * Enforces valid status transition to CANCELLED.
     */
    public void cancel(String cancellationReason) {
        if (this.status != LeaveRequestStatus.PENDING && this.status != LeaveRequestStatus.APPROVED) {
            throw new InvalidLeaveRequestException(
                    "Cannot cancel leave request with status '" + this.status + "'. Only PENDING or APPROVED requests can be cancelled.");
        }
        this.status = LeaveRequestStatus.CANCELLED;
        this.cancellationReason = cancellationReason != null ? cancellationReason.trim() : "Cancelled by user";
        this.cancelledTimestamp = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRequestNumber() {
        return requestNumber;
    }

    public void setRequestNumber(String requestNumber) {
        this.requestNumber = requestNumber;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(LeaveType leaveType) {
        this.leaveType = leaveType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getDuration() {
        return duration;
    }

    public void setDuration(BigDecimal duration) {
        this.duration = duration;
    }

    public boolean isHalfDay() {
        return halfDay;
    }

    public void setHalfDay(boolean halfDay) {
        this.halfDay = halfDay;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LeaveRequestStatus getStatus() {
        return status;
    }

    public void setStatus(LeaveRequestStatus status) {
        this.status = status;
    }

    public Employee getAssignedApprover() {
        return assignedApprover;
    }

    public void setAssignedApprover(Employee assignedApprover) {
        this.assignedApprover = assignedApprover;
    }

    public String getApprovalRoute() {
        return approvalRoute;
    }

    public void setApprovalRoute(String approvalRoute) {
        this.approvalRoute = approvalRoute;
    }

    public Employee getDecidedBy() {
        return decidedBy;
    }

    public void setDecidedBy(Employee decidedBy) {
        this.decidedBy = decidedBy;
    }

    public LocalDateTime getSubmissionTimestamp() {
        return submissionTimestamp;
    }

    public void setSubmissionTimestamp(LocalDateTime submissionTimestamp) {
        this.submissionTimestamp = submissionTimestamp;
    }

    public LocalDateTime getDecisionTimestamp() {
        return decisionTimestamp;
    }

    public void setDecisionTimestamp(LocalDateTime decisionTimestamp) {
        this.decisionTimestamp = decisionTimestamp;
    }

    public String getApproverComments() {
        return approverComments;
    }

    public void setApproverComments(String approverComments) {
        this.approverComments = approverComments;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getStaffingOverrideJustification() {
        return staffingOverrideJustification;
    }

    public void setStaffingOverrideJustification(String staffingOverrideJustification) {
        this.staffingOverrideJustification = staffingOverrideJustification;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public LocalDateTime getCancelledTimestamp() {
        return cancelledTimestamp;
    }

    public void setCancelledTimestamp(LocalDateTime cancelledTimestamp) {
        this.cancelledTimestamp = cancelledTimestamp;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}

