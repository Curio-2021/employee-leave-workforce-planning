package com.anudip.employee_leave.controller;

import com.anudip.employee_leave.dto.LeaveTypeDTO;
import com.anudip.employee_leave.entity.LeaveType;
import com.anudip.employee_leave.exception.DuplicateResourceException;
import com.anudip.employee_leave.service.LeaveTypeService;
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

@WebMvcTest(LeaveTypeController.class)
class LeaveTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LeaveTypeService leaveTypeService;

    @Test
    void testListLeaveTypesReturns200() throws Exception {
        LeaveType lt = new LeaveType("Casual Leave", "CL", "Desc", true, true, 3, false);
        lt.setId(1L);
        when(leaveTypeService.getAllLeaveTypes()).thenReturn(List.of(lt));

        mockMvc.perform(get("/leave-types"))
                .andExpect(status().isOk())
                .andExpect(view().name("leave-types/list"))
                .andExpect(model().attributeExists("leaveTypes"))
                .andExpect(model().attributeExists("leaveTypeDTO"));
    }

    @Test
    void testCreateLeaveTypeSuccessRedirects() throws Exception {
        LeaveType lt = new LeaveType("Sick Leave", "SL", "Desc", true, true, 5, true);
        lt.setId(2L);
        when(leaveTypeService.createLeaveType(any(LeaveTypeDTO.class))).thenReturn(lt);

        mockMvc.perform(post("/leave-types")
                        .param("name", "Sick Leave")
                        .param("code", "SL")
                        .param("description", "Medical reason")
                        .param("paid", "true")
                        .param("active", "true")
                        .param("maxConsecutiveDays", "5")
                        .param("requiresAttachment", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/leave-types"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void testCreateLeaveTypeValidationErrors() throws Exception {
        mockMvc.perform(post("/leave-types")
                        .param("name", "")
                        .param("code", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("leave-types/list"))
                .andExpect(model().hasErrors());
    }

    @Test
    void testCreateLeaveTypeDuplicateError() throws Exception {
        when(leaveTypeService.createLeaveType(any(LeaveTypeDTO.class)))
                .thenThrow(new DuplicateResourceException("Leave type with name 'Casual Leave' already exists."));

        mockMvc.perform(post("/leave-types")
                        .param("name", "Casual Leave")
                        .param("code", "CL"))
                .andExpect(status().isOk())
                .andExpect(view().name("leave-types/list"))
                .andExpect(model().attributeHasFieldErrors("leaveTypeDTO", "name"));
    }
}

