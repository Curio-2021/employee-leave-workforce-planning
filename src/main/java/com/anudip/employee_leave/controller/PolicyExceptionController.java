package com.anudip.employee_leave.controller;

import com.anudip.employee_leave.dto.PolicyExceptionItem;
import com.anudip.employee_leave.service.DepartmentService;
import com.anudip.employee_leave.service.PolicyExceptionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/policy-exceptions")
public class PolicyExceptionController {

    private final PolicyExceptionService policyExceptionService;
    private final DepartmentService departmentService;

    public PolicyExceptionController(PolicyExceptionService policyExceptionService,
                                     DepartmentService departmentService) {
        this.policyExceptionService = policyExceptionService;
        this.departmentService = departmentService;
    }

    @GetMapping
    public String listExceptions(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer year,
            Model model) {

        List<PolicyExceptionItem> exceptions = policyExceptionService.getPolicyExceptions(category, departmentId, year);
        Map<String, Long> counts = policyExceptionService.getExceptionCounts();

        model.addAttribute("exceptions", exceptions);
        model.addAttribute("counts", counts);
        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("selectedCategory", category != null ? category.trim() : "");
        model.addAttribute("selectedDepartmentId", departmentId);
        model.addAttribute("selectedYear", year);
        model.addAttribute("currentYear", LocalDate.now().getYear());

        return "policy-exceptions/list";
    }
}

