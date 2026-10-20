package com.anudip.employee_leave.controller;

import com.anudip.employee_leave.dto.LeaveApprovalDTO;
import com.anudip.employee_leave.dto.LeaveRequestDTO;
import com.anudip.employee_leave.dto.WorkforceConflictReport;
import com.anudip.employee_leave.entity.Employee;
import com.anudip.employee_leave.entity.LeaveRequest;
import com.anudip.employee_leave.entity.LeaveRequestStatus;
import com.anudip.employee_leave.entity.Role;
import com.anudip.employee_leave.exception.InsufficientLeaveBalanceException;
import com.anudip.employee_leave.exception.InvalidLeaveRequestException;
import com.anudip.employee_leave.exception.ResourceNotFoundException;
import com.anudip.employee_leave.exception.StaffingShortageException;
import com.anudip.employee_leave.security.CustomUserDetails;
import com.anudip.employee_leave.security.LeaveSecurityService;
import com.anudip.employee_leave.service.LeaveConflictEngineService;
import com.anudip.employee_leave.service.LeaveRequestService;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/leave-requests")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;
    private final LeaveConflictEngineService leaveConflictEngineService;
    private final LeaveSecurityService leaveSecurityService;

    public LeaveRequestController(LeaveRequestService leaveRequestService,
                                  LeaveConflictEngineService leaveConflictEngineService) {
        this(leaveRequestService, leaveConflictEngineService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public LeaveRequestController(LeaveRequestService leaveRequestService,
                                  LeaveConflictEngineService leaveConflictEngineService,
                                  @org.springframework.beans.factory.annotation.Autowired(required = false) LeaveSecurityService leaveSecurityService) {
        this.leaveRequestService = leaveRequestService;
        this.leaveConflictEngineService = leaveConflictEngineService;
        this.leaveSecurityService = leaveSecurityService != null ? leaveSecurityService : new LeaveSecurityService(null, null);
    }

    @GetMapping
    public String listRequests(@RequestParam(required = false) Long employeeId,
                               @RequestParam(required = false) Long departmentId,
                               @RequestParam(required = false) Long leaveTypeId,
                               @RequestParam(required = false) LeaveRequestStatus status,
                               Model model) {
        CustomUserDetails currentUser = leaveSecurityService.getCurrentUserDetails();
        if (currentUser != null && currentUser.getRole() == Role.ROLE_EMPLOYEE && currentUser.getEmployee() != null) {
            // Employees may only view their own leave requests
            employeeId = currentUser.getEmployee().getId();
            model.addAttribute("employees", List.of(currentUser.getEmployee()));
        } else {
            model.addAttribute("employees", leaveRequestService.getAllActiveEmployees());
        }

        List<LeaveRequest> requests = leaveRequestService.getFilteredRequests(employeeId, departmentId, leaveTypeId, status);
        model.addAttribute("requests", requests);
        model.addAttribute("departments", leaveRequestService.getAllDepartments());
        model.addAttribute("leaveTypes", leaveRequestService.getAllActiveLeaveTypes());
        model.addAttribute("statuses", LeaveRequestStatus.values());
        model.addAttribute("selectedEmployeeId", employeeId);
        model.addAttribute("selectedDepartmentId", departmentId);
        model.addAttribute("selectedLeaveTypeId", leaveTypeId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("pendingCount", leaveRequestService.countPendingRequests());
        return "leave-requests/list";
    }

    @GetMapping("/pending")
    public String listPendingRequests(Model model) {
        CustomUserDetails currentUser = leaveSecurityService.getCurrentUserDetails();
        List<LeaveRequest> allPending = leaveRequestService.getPendingRequests();
        List<LeaveRequest> filteredPending;

        if (currentUser != null && currentUser.getRole() == Role.ROLE_MANAGER && currentUser.getEmployee() != null) {
            Long managerId = currentUser.getEmployee().getId();
            filteredPending = allPending.stream()
                    .filter(r -> r.getEmployee() != null &&
                            r.getEmployee().getReportingManager() != null &&
                            r.getEmployee().getReportingManager().getId().equals(managerId) &&
                            !r.getEmployee().getId().equals(managerId))
                    .toList();
        } else {
            filteredPending = allPending;
        }

        model.addAttribute("pendingRequests", filteredPending);
        model.addAttribute("eligibleApprovers", leaveRequestService.getAllActiveEmployees());
        model.addAttribute("approvalDTO", new LeaveApprovalDTO());

        Map<Long, WorkforceConflictReport> conflictMap = new HashMap<>();
        for (LeaveRequest req : filteredPending) {
            conflictMap.put(req.getId(), leaveConflictEngineService.evaluateLeaveRequestConflicts(req));
        }
        model.addAttribute("conflictMap", conflictMap);

        return "leave-requests/pending";
    }

    @GetMapping("/new")
    public String showCreateForm(@RequestParam(required = false) Long employeeId, Model model) {
        LeaveRequestDTO dto = new LeaveRequestDTO();
        CustomUserDetails currentUser = leaveSecurityService.getCurrentUserDetails();

        if (currentUser != null && currentUser.getRole() == Role.ROLE_EMPLOYEE && currentUser.getEmployee() != null) {
            dto.setEmployeeId(currentUser.getEmployee().getId());
            model.addAttribute("employees", List.of(currentUser.getEmployee()));
        } else {
            if (employeeId != null) {
                dto.setEmployeeId(employeeId);
            }
            model.addAttribute("employees", leaveRequestService.getAllActiveEmployees());
        }

        model.addAttribute("leaveRequestDTO", dto);
        model.addAttribute("leaveTypes", leaveRequestService.getAllActiveLeaveTypes());
        return "leave-requests/create";
    }

    @PostMapping
    public String submitRequest(@Valid @ModelAttribute("leaveRequestDTO") LeaveRequestDTO dto,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        CustomUserDetails currentUser = leaveSecurityService.getCurrentUserDetails();
        if (currentUser != null && currentUser.getRole() == Role.ROLE_EMPLOYEE && currentUser.getEmployee() != null) {
            // Guarantee ownership: override any client-tampered employeeId
            dto.setEmployeeId(currentUser.getEmployee().getId());
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("employees", getAvailableEmployeesForUser(currentUser));
            model.addAttribute("leaveTypes", leaveRequestService.getAllActiveLeaveTypes());
            return "leave-requests/create";
        }

        try {
            LeaveRequest saved = leaveRequestService.submitLeaveRequest(dto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Leave request " + saved.getRequestNumber() + " submitted successfully. Status: Pending Approval.");
            return "redirect:/leave-requests/" + saved.getId();
        } catch (InvalidLeaveRequestException | InsufficientLeaveBalanceException | ResourceNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("employees", getAvailableEmployeesForUser(currentUser));
            model.addAttribute("leaveTypes", leaveRequestService.getAllActiveLeaveTypes());
            return "leave-requests/create";
        }
    }

    @GetMapping("/{id}")
    public String viewRequestDetails(@PathVariable Long id, Model model) {
        if (!leaveSecurityService.canAccessRequest(id)) {
            throw new AccessDeniedException("You are not authorized to view this leave request.");
        }

        LeaveRequest request = leaveRequestService.getRequestById(id);
        model.addAttribute("request", request);
        model.addAttribute("eligibleApprovers", leaveRequestService.getAllActiveEmployees());

        WorkforceConflictReport conflictReport = leaveConflictEngineService.evaluateLeaveRequestConflicts(request);
        model.addAttribute("conflictReport", conflictReport);

        LeaveApprovalDTO approvalDTO = new LeaveApprovalDTO();
        approvalDTO.setRequestId(id);
        if (request.getAssignedApprover() != null) {
            approvalDTO.setActingApproverId(request.getAssignedApprover().getId());
        }
        model.addAttribute("approvalDTO", approvalDTO);
        model.addAttribute("canApprove", leaveSecurityService.canApproveRequest(id));
        model.addAttribute("canCancel", leaveSecurityService.canCancelRequest(id));
        return "leave-requests/view";
    }

    @PostMapping("/{id}/approve")
    public String approveRequest(@PathVariable Long id,
                                 @RequestParam(required = false) Long actingApproverId,
                                 @RequestParam(required = false) String approverComments,
                                 @RequestParam(required = false, defaultValue = "false") boolean overrideConfirmed,
                                 @RequestParam(required = false) String overrideJustification,
                                 RedirectAttributes redirectAttributes) {
        if (!leaveSecurityService.canApproveRequest(id)) {
            throw new AccessDeniedException("You are not authorized to approve this leave request.");
        }

        CustomUserDetails currentUser = leaveSecurityService.getCurrentUserDetails();
        if (currentUser != null && currentUser.getEmployee() != null) {
            actingApproverId = currentUser.getEmployee().getId();
        }

        try {
            LeaveRequest approved = leaveRequestService.approveLeaveRequest(
                    id, actingApproverId, approverComments, overrideConfirmed, overrideJustification);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Leave request " + approved.getRequestNumber() + " has been approved successfully. " +
                            approved.getDuration() + " days deducted from available balance.");
        } catch (StaffingShortageException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Approval blocked: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Approval failed: " + e.getMessage());
        }
        return "redirect:/leave-requests/" + id;
    }

    @PostMapping("/{id}/reject")
    public String rejectRequest(@PathVariable Long id,
                                @RequestParam(required = false) Long actingApproverId,
                                @RequestParam String rejectionReason,
                                @RequestParam(required = false) String approverComments,
                                RedirectAttributes redirectAttributes) {
        if (!leaveSecurityService.canApproveRequest(id)) {
            throw new AccessDeniedException("You are not authorized to reject this leave request.");
        }

        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Rejection reason is mandatory.");
            return "redirect:/leave-requests/" + id;
        }

        CustomUserDetails currentUser = leaveSecurityService.getCurrentUserDetails();
        if (currentUser != null && currentUser.getEmployee() != null) {
            actingApproverId = currentUser.getEmployee().getId();
        }

        try {
            LeaveRequest rejected = leaveRequestService.rejectLeaveRequest(id, actingApproverId, rejectionReason, approverComments);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Leave request " + rejected.getRequestNumber() + " has been rejected. No balance was deducted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Rejection failed: " + e.getMessage());
        }
        return "redirect:/leave-requests/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancelRequest(@PathVariable Long id,
                                @RequestParam(required = false) String cancellationReason,
                                RedirectAttributes redirectAttributes) {
        if (!leaveSecurityService.canCancelRequest(id)) {
            throw new AccessDeniedException("You are not authorized to cancel this leave request.");
        }

        CustomUserDetails currentUser = leaveSecurityService.getCurrentUserDetails();
        String actor = (currentUser != null && currentUser.getEmployee() != null)
                ? currentUser.getEmployee().getEmployeeId()
                : "AUTHENTICATED_USER";

        try {
            LeaveRequest cancelled = leaveRequestService.cancelLeaveRequest(id, cancellationReason, actor);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Leave request " + cancelled.getRequestNumber() + " has been cancelled.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cancellation failed: " + e.getMessage());
        }
        return "redirect:/leave-requests/" + id;
    }

    private List<Employee> getAvailableEmployeesForUser(CustomUserDetails currentUser) {
        if (currentUser != null && currentUser.getRole() == Role.ROLE_EMPLOYEE && currentUser.getEmployee() != null) {
            return List.of(currentUser.getEmployee());
        }
        return leaveRequestService.getAllActiveEmployees();
    }
}
