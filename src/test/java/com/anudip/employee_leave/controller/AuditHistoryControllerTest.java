package com.anudip.employee_leave.controller;

import com.anudip.employee_leave.entity.AuditEvent;
import com.anudip.employee_leave.entity.Department;
import com.anudip.employee_leave.service.AuditService;
import com.anudip.employee_leave.service.DepartmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AuditHistoryController.class)
class AuditHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean
    private DepartmentService departmentService;

    @Test
    void testListAuditEvents_Returns200AndPopulatesModel() throws Exception {
        AuditEvent event = new AuditEvent(
                "DEV_UNAUTHENTICATED: Admin (EMP001)",
                "LEAVE_REQUEST_APPROVED",
                "LeaveRequest",
                "LR-2026-0001",
                1L,
                "Approved 3 days leave"
        );

        Department dept = new Department();
        dept.setId(1L);
        dept.setName("HR");

        when(auditService.getFilteredEvents(any(), any(), any(), any(), any())).thenReturn(List.of(event));
        when(departmentService.getAllDepartments()).thenReturn(List.of(dept));

        mockMvc.perform(get("/audit-history"))
                .andExpect(status().isOk())
                .andExpect(view().name("audit-history/list"))
                .andExpect(model().attributeExists("auditEvents", "departments"))
                .andExpect(content().string(containsString("System Audit Trail")))
                .andExpect(content().string(containsString("LEAVE_REQUEST_APPROVED")))
                .andExpect(content().string(containsString("DEV_UNAUTHENTICATED: Admin (EMP001)")));
    }
}

