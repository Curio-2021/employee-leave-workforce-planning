package com.anudip.employee_leave.controller;

import com.anudip.employee_leave.dto.DepartmentDTO;
import com.anudip.employee_leave.entity.Department;
import com.anudip.employee_leave.exception.DuplicateResourceException;
import com.anudip.employee_leave.service.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public String listDepartments(Model model) {
        List<Department> departments = departmentService.getAllDepartments();
        model.addAttribute("departments", departments);
        if (!model.containsAttribute("departmentDTO")) {
            model.addAttribute("departmentDTO", new DepartmentDTO());
        }
        return "departments/list";
    }

    @PostMapping
    public String createDepartment(@Valid @ModelAttribute("departmentDTO") DepartmentDTO departmentDTO,
                                   BindingResult bindingResult,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("departments", departmentService.getAllDepartments());
            return "departments/list";
        }

        try {
            departmentService.createDepartment(departmentDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Department '" + departmentDTO.getName() + "' created successfully.");
            return "redirect:/departments";
        } catch (DuplicateResourceException ex) {
            bindingResult.rejectValue("name", "duplicate", ex.getMessage());
            model.addAttribute("departments", departmentService.getAllDepartments());
            return "departments/list";
        }
    }
}

