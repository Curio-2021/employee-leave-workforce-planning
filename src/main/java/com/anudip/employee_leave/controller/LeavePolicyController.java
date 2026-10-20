package com.anudip.employee_leave.controller;

import com.anudip.employee_leave.dto.LeavePolicyDTO;
import com.anudip.employee_leave.entity.EmploymentType;
import com.anudip.employee_leave.entity.LeavePolicy;
import com.anudip.employee_leave.exception.DuplicateResourceException;
import com.anudip.employee_leave.service.DepartmentService;
import com.anudip.employee_leave.service.LeavePolicyService;
import com.anudip.employee_leave.service.LeaveTypeService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/leave-policies")
public class LeavePolicyController {

    private final LeavePolicyService leavePolicyService;
    private final LeaveTypeService leaveTypeService;
    private final DepartmentService departmentService;

    public LeavePolicyController(LeavePolicyService leavePolicyService,
                                 LeaveTypeService leaveTypeService,
                                 DepartmentService departmentService) {
        this.leavePolicyService = leavePolicyService;
        this.leaveTypeService = leaveTypeService;
        this.departmentService = departmentService;
    }

    @GetMapping
    public String listPolicies(Model model) {
        model.addAttribute("policies", leavePolicyService.getAllPolicies());
        return "leave-policies/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        populateFormMetadata(model, "Create Leave Policy", false);
        model.addAttribute("leavePolicyDTO", new LeavePolicyDTO());
        return "leave-policies/form";
    }

    @PostMapping
    public String createPolicy(@Valid @ModelAttribute("leavePolicyDTO") LeavePolicyDTO leavePolicyDTO,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateFormMetadata(model, "Create Leave Policy", false);
            return "leave-policies/form";
        }

        try {
            leavePolicyService.createPolicy(leavePolicyDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Leave policy '" + leavePolicyDTO.getPolicyName() + "' created successfully.");
            return "redirect:/leave-policies";
        } catch (DuplicateResourceException ex) {
            bindingResult.rejectValue("policyName", "duplicate", ex.getMessage());
            populateFormMetadata(model, "Create Leave Policy", false);
            return "leave-policies/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        LeavePolicy policy = leavePolicyService.getPolicyById(id);
        populateFormMetadata(model, "Edit Leave Policy", true);
        model.addAttribute("leavePolicyDTO", leavePolicyService.convertToDTO(policy));
        return "leave-policies/form";
    }

    @PostMapping("/{id}/edit")
    public String updatePolicy(@PathVariable Long id,
                               @Valid @ModelAttribute("leavePolicyDTO") LeavePolicyDTO leavePolicyDTO,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateFormMetadata(model, "Edit Leave Policy", true);
            return "leave-policies/form";
        }

        try {
            leavePolicyService.updatePolicy(id, leavePolicyDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Leave policy '" + leavePolicyDTO.getPolicyName() + "' updated successfully.");
            return "redirect:/leave-policies";
        } catch (DuplicateResourceException ex) {
            bindingResult.rejectValue("policyName", "duplicate", ex.getMessage());
            populateFormMetadata(model, "Edit Leave Policy", true);
            return "leave-policies/form";
        }
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        leavePolicyService.togglePolicyStatus(id);
        redirectAttributes.addFlashAttribute("successMessage", "Leave policy status updated successfully.");
        return "redirect:/leave-policies";
    }

    private void populateFormMetadata(Model model, String pageTitle, boolean isEdit) {
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("leaveTypes", leaveTypeService.getActiveLeaveTypes());
        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("employmentTypes", EmploymentType.values());
    }
}

