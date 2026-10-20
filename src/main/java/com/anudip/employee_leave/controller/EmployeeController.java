package com.anudip.employee_leave.controller;

import com.anudip.employee_leave.dto.EmployeeDTO;
import com.anudip.employee_leave.entity.Employee;
import com.anudip.employee_leave.entity.EmploymentStatus;
import com.anudip.employee_leave.entity.EmploymentType;
import com.anudip.employee_leave.exception.DuplicateResourceException;
import com.anudip.employee_leave.service.DepartmentService;
import com.anudip.employee_leave.service.EmployeeService;
import jakarta.validation.Valid;
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

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final DepartmentService departmentService;

    public EmployeeController(EmployeeService employeeService, DepartmentService departmentService) {
        this.employeeService = employeeService;
        this.departmentService = departmentService;
    }

    @GetMapping
    public String listEmployees(@RequestParam(required = false) Long departmentId,
                                @RequestParam(required = false) EmploymentStatus status,
                                Model model) {
        List<Employee> employees = employeeService.getFilteredEmployees(departmentId, status);
        model.addAttribute("employees", employees);
        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("selectedDepartmentId", departmentId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("employmentStatuses", EmploymentStatus.values());
        return "employees/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setJoiningDate(LocalDate.now());
        dto.setEmploymentStatus(EmploymentStatus.ACTIVE);

        populateFormMetadata(model, null, "Add New Employee", false);
        model.addAttribute("employeeDTO", dto);
        return "employees/form";
    }

    @PostMapping
    public String createEmployee(@Valid @ModelAttribute("employeeDTO") EmployeeDTO employeeDTO,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateFormMetadata(model, null, "Add New Employee", false);
            return "employees/form";
        }

        try {
            employeeService.createEmployee(employeeDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Employee '" + employeeDTO.getFullName() + "' created successfully.");
            return "redirect:/employees";
        } catch (DuplicateResourceException ex) {
            handleDuplicateException(ex, bindingResult);
            populateFormMetadata(model, null, "Add New Employee", false);
            return "employees/form";
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("reportingManagerId", "invalid", ex.getMessage());
            populateFormMetadata(model, null, "Add New Employee", false);
            return "employees/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Employee employee = employeeService.getEmployeeById(id);
        EmployeeDTO dto = employeeService.convertToDTO(employee);

        populateFormMetadata(model, id, "Edit Employee: " + employee.getFullName(), true);
        model.addAttribute("employeeDTO", dto);
        return "employees/form";
    }

    @PostMapping("/{id}/edit")
    public String updateEmployee(@PathVariable Long id,
                                 @Valid @ModelAttribute("employeeDTO") EmployeeDTO employeeDTO,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateFormMetadata(model, id, "Edit Employee", true);
            return "employees/form";
        }

        try {
            employeeService.updateEmployee(id, employeeDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Employee '" + employeeDTO.getFullName() + "' updated successfully.");
            return "redirect:/employees";
        } catch (DuplicateResourceException ex) {
            handleDuplicateException(ex, bindingResult);
            populateFormMetadata(model, id, "Edit Employee", true);
            return "employees/form";
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("reportingManagerId", "invalid", ex.getMessage());
            populateFormMetadata(model, id, "Edit Employee", true);
            return "employees/form";
        }
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        employeeService.toggleEmployeeStatus(id);
        redirectAttributes.addFlashAttribute("successMessage", "Employee status updated successfully.");
        return "redirect:/employees";
    }

    private void populateFormMetadata(Model model, Long excludeEmployeeId, String pageTitle, boolean isEdit) {
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("managers", employeeService.getEligibleManagers(excludeEmployeeId));
        model.addAttribute("employmentTypes", EmploymentType.values());
        model.addAttribute("employmentStatuses", EmploymentStatus.values());
    }

    private void handleDuplicateException(DuplicateResourceException ex, BindingResult bindingResult) {
        String msg = ex.getMessage();
        if (msg.contains("Employee ID")) {
            bindingResult.rejectValue("employeeId", "duplicate", msg);
        } else if (msg.contains("Email")) {
            bindingResult.rejectValue("email", "duplicate", msg);
        } else {
            bindingResult.reject(null, msg);
        }
    }
}

