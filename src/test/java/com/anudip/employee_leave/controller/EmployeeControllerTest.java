package com.anudip.employee_leave.controller;

import com.anudip.employee_leave.dto.EmployeeDTO;
import com.anudip.employee_leave.entity.Department;
import com.anudip.employee_leave.entity.Employee;
import com.anudip.employee_leave.entity.EmploymentStatus;
import com.anudip.employee_leave.entity.EmploymentType;
import com.anudip.employee_leave.service.DepartmentService;
import com.anudip.employee_leave.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmployeeService employeeService;

    @MockitoBean
    private DepartmentService departmentService;

    private Department department;
    private Employee employee;

    @BeforeEach
    void setUp() {
        department = new Department("Engineering", "Software");
        department.setId(1L);

        employee = new Employee();
        employee.setId(1L);
        employee.setEmployeeId("EMP-001");
        employee.setFullName("Jane Doe");
        employee.setEmail("jane.doe@workforce.com");
        employee.setDepartment(department);
        employee.setEmploymentType(EmploymentType.FULL_TIME);
        employee.setJoiningDate(LocalDate.of(2024, 1, 15));
        employee.setEmploymentStatus(EmploymentStatus.ACTIVE);
    }

    @Test
    void testListEmployeesReturns200() throws Exception {
        when(employeeService.getFilteredEmployees(null, null)).thenReturn(List.of(employee));
        when(departmentService.getAllDepartments()).thenReturn(List.of(department));

        mockMvc.perform(get("/employees"))
                .andExpect(status().isOk())
                .andExpect(view().name("employees/list"))
                .andExpect(model().attributeExists("employees"))
                .andExpect(model().attributeExists("departments"));
    }

    @Test
    void testShowCreateFormReturns200() throws Exception {
        when(departmentService.getAllDepartments()).thenReturn(List.of(department));
        when(employeeService.getEligibleManagers(null)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/employees/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("employees/form"))
                .andExpect(model().attributeExists("employeeDTO"))
                .andExpect(model().attributeExists("departments"));
    }

    @Test
    void testCreateEmployeeSuccessRedirects() throws Exception {
        when(employeeService.createEmployee(any(EmployeeDTO.class))).thenReturn(employee);

        mockMvc.perform(post("/employees")
                        .param("employeeId", "EMP-001")
                        .param("fullName", "Jane Doe")
                        .param("email", "jane.doe@workforce.com")
                        .param("departmentId", "1")
                        .param("employmentType", "FULL_TIME")
                        .param("joiningDate", "2024-01-15")
                        .param("employmentStatus", "ACTIVE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employees"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void testCreateEmployeeValidationErrors() throws Exception {
        when(departmentService.getAllDepartments()).thenReturn(List.of(department));

        mockMvc.perform(post("/employees")
                        .param("employeeId", "")
                        .param("fullName", "")
                        .param("email", "not-an-email"))
                .andExpect(status().isOk())
                .andExpect(view().name("employees/form"))
                .andExpect(model().hasErrors());
    }

    @Test
    void testToggleStatusRedirects() throws Exception {
        mockMvc.perform(post("/employees/1/toggle-status"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employees"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(employeeService).toggleEmployeeStatus(eq(1L));
    }
}

