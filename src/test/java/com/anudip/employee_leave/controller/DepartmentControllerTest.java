package com.anudip.employee_leave.controller;

import com.anudip.employee_leave.dto.DepartmentDTO;
import com.anudip.employee_leave.entity.Department;
import com.anudip.employee_leave.exception.DuplicateResourceException;
import com.anudip.employee_leave.service.DepartmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(DepartmentController.class)
class DepartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DepartmentService departmentService;

    @Test
    void testListDepartmentsReturns200() throws Exception {
        Department dept = new Department("Engineering", "Software");
        dept.setId(1L);
        when(departmentService.getAllDepartments()).thenReturn(List.of(dept));

        mockMvc.perform(get("/departments"))
                .andExpect(status().isOk())
                .andExpect(view().name("departments/list"))
                .andExpect(model().attributeExists("departments"))
                .andExpect(model().attributeExists("departmentDTO"));
    }

    @Test
    void testCreateDepartmentSuccessRedirects() throws Exception {
        Department dept = new Department("Finance", "Accounting");
        dept.setId(2L);
        when(departmentService.createDepartment(any(DepartmentDTO.class))).thenReturn(dept);

        mockMvc.perform(post("/departments")
                        .param("name", "Finance")
                        .param("description", "Accounting"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/departments"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void testCreateDepartmentBlankNameHasValidationError() throws Exception {
        mockMvc.perform(post("/departments")
                        .param("name", "")
                        .param("description", "Desc"))
                .andExpect(status().isOk())
                .andExpect(view().name("departments/list"))
                .andExpect(model().hasErrors());
    }

    @Test
    void testCreateDepartmentDuplicateNameReturnsError() throws Exception {
        when(departmentService.createDepartment(any(DepartmentDTO.class)))
                .thenThrow(new DuplicateResourceException("Department with name 'Engineering' already exists."));

        mockMvc.perform(post("/departments")
                        .param("name", "Engineering")
                        .param("description", "Software"))
                .andExpect(status().isOk())
                .andExpect(view().name("departments/list"))
                .andExpect(model().attributeHasFieldErrors("departmentDTO", "name"));
    }
}

