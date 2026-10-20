package com.anudip.employee_leave.controller;

import com.anudip.employee_leave.dto.LeaveRequestDTO;
import com.anudip.employee_leave.dto.WorkforceConflictReport;
import com.anudip.employee_leave.entity.Department;
import com.anudip.employee_leave.entity.Employee;
import com.anudip.employee_leave.entity.EmploymentStatus;
import com.anudip.employee_leave.entity.EmploymentType;
import com.anudip.employee_leave.entity.LeaveRequest;
import com.anudip.employee_leave.entity.LeaveRequestStatus;
import com.anudip.employee_leave.entity.LeaveType;
import com.anudip.employee_leave.exception.InsufficientLeaveBalanceException;
import com.anudip.employee_leave.exception.StaffingShortageException;
import com.anudip.employee_leave.service.LeaveConflictEngineService;
import com.anudip.employee_leave.service.LeaveRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(LeaveRequestController.class)
class LeaveRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LeaveRequestService leaveRequestService;

    @MockitoBean
    private LeaveConflictEngineService leaveConflictEngineService;

    private Employee employee;
    private Employee manager;
    private LeaveType casualLeave;
    private LeaveRequest sampleRequest;

    @BeforeEach
    void setUp() {
        Department dept = new Department("Engineering", "Eng Dept");
        dept.setId(1L);

        manager = new Employee("MGR01", "Alice Manager", "alice@example.com", dept,
                EmploymentType.FULL_TIME, LocalDate.of(2020, 1, 1), EmploymentStatus.ACTIVE, null);
        manager.setId(2L);

        employee = new Employee("EMP01", "John Developer", "john@example.com", dept,
                EmploymentType.FULL_TIME, LocalDate.of(2024, 1, 1), EmploymentStatus.ACTIVE, manager);
        employee.setId(1L);

        casualLeave = new LeaveType("Casual Leave", "CL", "Casual Leave", true, true, 5, false);
        casualLeave.setId(1L);

        sampleRequest = new LeaveRequest("LR-2026-TEST01", employee, casualLeave,
                LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 5), BigDecimal.valueOf(2.00),
                false, "Attending conference", manager, "MANAGER_APPROVAL");
        sampleRequest.setId(10L);

        when(leaveConflictEngineService.evaluateLeaveRequestConflicts(any())).thenReturn(new WorkforceConflictReport());
    }

    @Test
    void testListRequestsReturns200AndListView() throws Exception {
        when(leaveRequestService.getFilteredRequests(any(), any(), any(), any()))
                .thenReturn(List.of(sampleRequest));
        when(leaveRequestService.getAllActiveEmployees()).thenReturn(List.of(employee));
        when(leaveRequestService.getAllActiveLeaveTypes()).thenReturn(List.of(casualLeave));
        when(leaveRequestService.getAllDepartments()).thenReturn(Collections.emptyList());
        when(leaveRequestService.countPendingRequests()).thenReturn(1L);

        mockMvc.perform(get("/leave-requests"))
                .andExpect(status().isOk())
                .andExpect(view().name("leave-requests/list"))
                .andExpect(model().attributeExists("requests", "employees", "leaveTypes", "statuses"))
                .andExpect(content().string(containsString("LR-2026-TEST01")))
                .andExpect(content().string(containsString("John Developer")));
    }

    @Test
    void testListPendingRequestsReturns200AndPendingView() throws Exception {
        when(leaveRequestService.getPendingRequests()).thenReturn(List.of(sampleRequest));
        when(leaveRequestService.getAllActiveEmployees()).thenReturn(List.of(manager));

        mockMvc.perform(get("/leave-requests/pending"))
                .andExpect(status().isOk())
                .andExpect(view().name("leave-requests/pending"))
                .andExpect(model().attributeExists("pendingRequests", "eligibleApprovers"))
                .andExpect(content().string(containsString("LR-2026-TEST01")));
    }

    @Test
    void testShowCreateFormReturns200AndCreateView() throws Exception {
        when(leaveRequestService.getAllActiveEmployees()).thenReturn(List.of(employee));
        when(leaveRequestService.getAllActiveLeaveTypes()).thenReturn(List.of(casualLeave));

        mockMvc.perform(get("/leave-requests/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("leave-requests/create"))
                .andExpect(model().attributeExists("leaveRequestDTO", "employees", "leaveTypes"))
                .andExpect(content().string(containsString("Apply for Leave")));
    }

    @Test
    void testSubmitRequestSuccessRedirectsToDetails() throws Exception {
        when(leaveRequestService.submitLeaveRequest(any(LeaveRequestDTO.class))).thenReturn(sampleRequest);

        mockMvc.perform(post("/leave-requests")
                        .param("employeeId", "1")
                        .param("leaveTypeId", "1")
                        .param("startDate", "2026-05-04")
                        .param("endDate", "2026-05-05")
                        .param("halfDay", "false")
                        .param("reason", "Attending official technical conference"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/leave-requests/10"));
    }

    @Test
    void testSubmitRequestInsufficientBalanceReturnsCreateViewWithError() throws Exception {
        when(leaveRequestService.submitLeaveRequest(any(LeaveRequestDTO.class)))
                .thenThrow(new InsufficientLeaveBalanceException("Insufficient leave balance for Casual Leave."));
        when(leaveRequestService.getAllActiveEmployees()).thenReturn(List.of(employee));
        when(leaveRequestService.getAllActiveLeaveTypes()).thenReturn(List.of(casualLeave));

        mockMvc.perform(post("/leave-requests")
                        .param("employeeId", "1")
                        .param("leaveTypeId", "1")
                        .param("startDate", "2026-05-04")
                        .param("endDate", "2026-05-05")
                        .param("halfDay", "false")
                        .param("reason", "Attending official technical conference"))
                .andExpect(status().isOk())
                .andExpect(view().name("leave-requests/create"))
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(content().string(containsString("Insufficient leave balance for Casual Leave.")));
    }

    @Test
    void testViewRequestDetailsReturns200AndView() throws Exception {
        when(leaveRequestService.getRequestById(10L)).thenReturn(sampleRequest);
        when(leaveRequestService.getAllActiveEmployees()).thenReturn(List.of(manager));

        mockMvc.perform(get("/leave-requests/10"))
                .andExpect(status().isOk())
                .andExpect(view().name("leave-requests/view"))
                .andExpect(model().attributeExists("request", "eligibleApprovers", "approvalDTO", "conflictReport"))
                .andExpect(content().string(containsString("LR-2026-TEST01")))
                .andExpect(content().string(containsString("Attending conference")));
    }

    @Test
    void testApproveRequestRedirectsToDetails() throws Exception {
        sampleRequest.setStatus(LeaveRequestStatus.APPROVED);
        when(leaveRequestService.approveLeaveRequest(eq(10L), any(), any(), anyBoolean(), any())).thenReturn(sampleRequest);

        mockMvc.perform(post("/leave-requests/10/approve")
                        .param("actingApproverId", "2")
                        .param("approverComments", "Approved. Handover done."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/leave-requests/10"));
    }

    @Test
    void testApproveRequest_StaffingShortageException_RedirectsWithFlashError() throws Exception {
        when(leaveRequestService.approveLeaveRequest(eq(10L), any(), any(), anyBoolean(), any()))
                .thenThrow(new StaffingShortageException("Strict policy breached: insufficient on-duty staff."));

        mockMvc.perform(post("/leave-requests/10/approve")
                        .param("actingApproverId", "2")
                        .param("approverComments", "Approved"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/leave-requests/10"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    void testApproveRequest_WithOverrideParameters_PassesToService() throws Exception {
        sampleRequest.setStatus(LeaveRequestStatus.APPROVED);
        when(leaveRequestService.approveLeaveRequest(eq(10L), eq(2L), eq("Override applied"), eq(true), eq("Critical coverage arranged")))
                .thenReturn(sampleRequest);

        mockMvc.perform(post("/leave-requests/10/approve")
                        .param("actingApproverId", "2")
                        .param("approverComments", "Override applied")
                        .param("overrideConfirmed", "true")
                        .param("overrideJustification", "Critical coverage arranged"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/leave-requests/10"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void testRejectRequestRedirectsToDetails() throws Exception {
        sampleRequest.setStatus(LeaveRequestStatus.REJECTED);
        when(leaveRequestService.rejectLeaveRequest(eq(10L), any(), anyString(), any())).thenReturn(sampleRequest);

        mockMvc.perform(post("/leave-requests/10/reject")
                        .param("actingApproverId", "2")
                        .param("rejectionReason", "Team capacity issue")
                        .param("approverComments", "Please reschedule"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/leave-requests/10"));
    }

    @Test
    void testCancelRequestRedirectsToDetails() throws Exception {
        sampleRequest.setStatus(LeaveRequestStatus.CANCELLED);
        when(leaveRequestService.cancelLeaveRequest(eq(10L), anyString(), anyString())).thenReturn(sampleRequest);

        mockMvc.perform(post("/leave-requests/10/cancel")
                        .param("cancellationReason", "Personal plans changed"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/leave-requests/10"));
    }
}

