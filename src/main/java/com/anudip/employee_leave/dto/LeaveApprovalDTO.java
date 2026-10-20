package com.anudip.employee_leave.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class LeaveApprovalDTO {

    @NotNull(message = "Request ID is required")
    private Long requestId;

    private Long actingApproverId;

    @Size(max = 500, message = "Comments cannot exceed 500 characters")
    private String approverComments;

    @Size(max = 500, message = "Rejection reason cannot exceed 500 characters")
    private String rejectionReason;

    @Size(max = 500, message = "Cancellation reason cannot exceed 500 characters")
    private String cancellationReason;

    private boolean overrideConfirmed = false;

    @Size(max = 500, message = "Override justification cannot exceed 500 characters")
    private String overrideJustification;

    public LeaveApprovalDTO() {
    }

    public LeaveApprovalDTO(Long requestId, Long actingApproverId, String approverComments) {
        this.requestId = requestId;
        this.actingApproverId = actingApproverId;
        this.approverComments = approverComments;
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public Long getActingApproverId() {
        return actingApproverId;
    }

    public void setActingApproverId(Long actingApproverId) {
        this.actingApproverId = actingApproverId;
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

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public boolean isOverrideConfirmed() {
        return overrideConfirmed;
    }

    public void setOverrideConfirmed(boolean overrideConfirmed) {
        this.overrideConfirmed = overrideConfirmed;
    }

    public String getOverrideJustification() {
        return overrideJustification;
    }

    public void setOverrideJustification(String overrideJustification) {
        this.overrideJustification = overrideJustification;
    }
}

