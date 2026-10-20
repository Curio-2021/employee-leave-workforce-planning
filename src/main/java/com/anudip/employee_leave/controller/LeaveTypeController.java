package com.anudip.employee_leave.controller;

import com.anudip.employee_leave.dto.LeaveTypeDTO;
import com.anudip.employee_leave.entity.LeaveType;
import com.anudip.employee_leave.exception.DuplicateResourceException;
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
@RequestMapping("/leave-types")
public class LeaveTypeController {

    private final LeaveTypeService leaveTypeService;

    public LeaveTypeController(LeaveTypeService leaveTypeService) {
        this.leaveTypeService = leaveTypeService;
    }

    @GetMapping
    public String listLeaveTypes(Model model) {
        model.addAttribute("leaveTypes", leaveTypeService.getAllLeaveTypes());
        if (!model.containsAttribute("leaveTypeDTO")) {
            model.addAttribute("leaveTypeDTO", new LeaveTypeDTO());
        }
        return "leave-types/list";
    }

    @PostMapping
    public String createLeaveType(@Valid @ModelAttribute("leaveTypeDTO") LeaveTypeDTO leaveTypeDTO,
                                  BindingResult bindingResult,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("leaveTypes", leaveTypeService.getAllLeaveTypes());
            return "leave-types/list";
        }

        try {
            leaveTypeService.createLeaveType(leaveTypeDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Leave type '" + leaveTypeDTO.getName() + "' (" + leaveTypeDTO.getCode() + ") created successfully.");
            return "redirect:/leave-types";
        } catch (DuplicateResourceException ex) {
            handleDuplicateException(ex, bindingResult);
            model.addAttribute("leaveTypes", leaveTypeService.getAllLeaveTypes());
            return "leave-types/list";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        LeaveType leaveType = leaveTypeService.getLeaveTypeById(id);
        model.addAttribute("leaveTypeDTO", leaveTypeService.convertToDTO(leaveType));
        return "leave-types/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateLeaveType(@PathVariable Long id,
                                  @Valid @ModelAttribute("leaveTypeDTO") LeaveTypeDTO leaveTypeDTO,
                                  BindingResult bindingResult,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "leave-types/edit";
        }

        try {
            leaveTypeService.updateLeaveType(id, leaveTypeDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Leave type '" + leaveTypeDTO.getName() + "' updated successfully.");
            return "redirect:/leave-types";
        } catch (DuplicateResourceException ex) {
            handleDuplicateException(ex, bindingResult);
            return "leave-types/edit";
        }
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        leaveTypeService.toggleLeaveTypeStatus(id);
        redirectAttributes.addFlashAttribute("successMessage", "Leave type status updated successfully.");
        return "redirect:/leave-types";
    }

    private void handleDuplicateException(DuplicateResourceException ex, BindingResult bindingResult) {
        String msg = ex.getMessage();
        if (msg.contains("name")) {
            bindingResult.rejectValue("name", "duplicate", msg);
        } else if (msg.contains("code")) {
            bindingResult.rejectValue("code", "duplicate", msg);
        } else {
            bindingResult.reject(null, msg);
        }
    }
}

