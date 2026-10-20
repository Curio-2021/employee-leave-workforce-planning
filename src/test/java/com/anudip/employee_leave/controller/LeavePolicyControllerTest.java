package com.anudip.employee_leave.controller;

import com.anudip.employee_leave.dto.LeavePolicyDTO;
import com.anudip.employee_leave.entity.LeavePolicy;
import com.anudip.employee_leave.entity.LeaveType;
import com.anudip.employee_leave.service.DepartmentService;
import com.anudip.employee_leave.service.LeavePolicyService;
import com.anudip.employee_leave.service.LeaveTypeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
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

@WebMvcTest(LeavePolicyController.class)
class LeavePolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LeavePolicyService leavePolicyService;

    @MockitoBean
    private LeaveTypeService leaveTypeService;

    @MockitoBean
    private DepartmentService departmentService;

    @Test
    void testListPoliciesReturns200() throws Exception {
        LeaveType lt = new LeaveType("Casual Leave", "CL", "Desc", true, true, 3, false);
        LeavePolicy policy = new LeavePolicy("Casual Policy 2026", lt, null, null, 12, false, 0, 0, 2026, true);
        when(leavePolicyService.getAllPolicies()).thenReturn(List.of(policy));

        mockMvc.perform(get("/leave-policies"))
                .andExpect(status().isOk())
                .andExpect(view().name("leave-policies/list"))
                .andExpect(model().attributeExists("policies"));
    }

    @Test
    void testShowCreateFormReturns200() throws Exception {
        when(leaveTypeService.getActiveLeaveTypes()).thenReturn(Collections.emptyList());
        when(departmentService.getAllDepartments()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/leave-policies/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("leave-policies/form"))
                .andExpect(model().attributeExists("leavePolicyDTO"))
                .andExpect(model().attributeExists("leaveTypes"));
    }

    @Test
    void testCreatePolicySuccessRedirects() throws Exception {
        LeaveType lt = new LeaveType("Casual Leave", "CL", "Desc", true, true, 3, false);
        LeavePolicy policy = new LeavePolicy("Casual Policy 2026", lt, null, null, 12, false, 0, 0, 2026, true);
        when(leavePolicyService.createPolicy(any(LeavePolicyDTO.class))).thenReturn(policy);

        mockMvc.perform(post("/leave-policies")
                        .param("policyName", "Casual Policy 2026")
                        .param("leaveTypeId", "1")
                        .param("annualAllocation", "12")
                        .param("effectiveYear", "2026"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/leave-policies"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void testCreatePolicyValidationErrors() throws Exception {
        when(leaveTypeService.getActiveLeaveTypes()).thenReturn(Collections.emptyList());
        when(departmentService.getAllDepartments()).thenReturn(Collections.emptyList());

        mockMvc.perform(post("/leave-policies")
                        .param("policyName", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("leave-policies/form"))
                .andExpect(model().hasErrors());
    }
}

