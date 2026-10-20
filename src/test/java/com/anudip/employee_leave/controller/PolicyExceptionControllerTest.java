package com.anudip.employee_leave.controller;

import com.anudip.employee_leave.dto.PolicyExceptionItem;
import com.anudip.employee_leave.entity.Department;
import com.anudip.employee_leave.service.DepartmentService;
import com.anudip.employee_leave.service.PolicyExceptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(PolicyExceptionController.class)
class PolicyExceptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PolicyExceptionService policyExceptionService;

    @MockitoBean
    private DepartmentService departmentService;

    @Test
    void testListExceptions_Returns200AndPopulatesModel() throws Exception {
        PolicyExceptionItem item = new PolicyExceptionItem(
                "OVR-1", "AUTHORIZED_OVERRIDE", "Authorized Staffing Override",
                "WARNING", LocalDateTime.now(), "LR-2026-0001", "Alice", "EMP01",
                "IT", 1L, "Details", "Justification text", "/leave-requests/1"
        );

        Department dept = new Department();
        dept.setId(1L);
        dept.setName("IT");

        when(policyExceptionService.getPolicyExceptions(any(), any(), any())).thenReturn(List.of(item));
        when(policyExceptionService.getExceptionCounts()).thenReturn(Map.of("TOTAL", 1L, "AUTHORIZED_OVERRIDE", 1L));
        when(departmentService.getAllDepartments()).thenReturn(List.of(dept));

        mockMvc.perform(get("/policy-exceptions"))
                .andExpect(status().isOk())
                .andExpect(view().name("policy-exceptions/list"))
                .andExpect(model().attributeExists("exceptions", "counts", "departments"))
                .andExpect(content().string(containsString("Policy Exceptions &amp; Overrides")))
                .andExpect(content().string(containsString("LR-2026-0001")))
                .andExpect(content().string(containsString("Authorized Staffing Override")));
    }
}

