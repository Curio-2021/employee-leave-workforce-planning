package com.anudip.employee_leave.controller;

import com.anudip.employee_leave.dto.DepartmentStaffingRuleDTO;
import com.anudip.employee_leave.entity.DepartmentStaffingRule;
import com.anudip.employee_leave.service.DepartmentStaffingRuleService;
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

import java.util.List;

@Controller
@RequestMapping("/staffing-rules")
public class StaffingRuleController {

    private final DepartmentStaffingRuleService staffingRuleService;

    public StaffingRuleController(DepartmentStaffingRuleService staffingRuleService) {
        this.staffingRuleService = staffingRuleService;
    }

    @GetMapping
    public String listRules(Model model) {
        List<DepartmentStaffingRuleDTO> rules = staffingRuleService.getAllDepartmentRules();
        model.addAttribute("rules", rules);
        return "staffing-rules/list";
    }

    @GetMapping("/{departmentId}/edit")
    public String editRuleForm(@PathVariable Long departmentId, Model model) {
        DepartmentStaffingRuleDTO ruleDTO = staffingRuleService.getRuleDTOByDepartmentId(departmentId);
        model.addAttribute("ruleDTO", ruleDTO);
        return "staffing-rules/edit";
    }

    @PostMapping("/{departmentId}")
    public String saveRule(@PathVariable Long departmentId,
                           @Valid @ModelAttribute("ruleDTO") DepartmentStaffingRuleDTO ruleDTO,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes) {

        ruleDTO.setDepartmentId(departmentId);

        if (bindingResult.hasErrors()) {
            return "staffing-rules/edit";
        }

        try {
            DepartmentStaffingRule saved = staffingRuleService.saveOrUpdateRule(ruleDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Staffing rule for " + saved.getDepartment().getName() + " saved successfully.");
            return "redirect:/staffing-rules";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Failed to save staffing rule: " + e.getMessage());
            return "staffing-rules/edit";
        }
    }
}

