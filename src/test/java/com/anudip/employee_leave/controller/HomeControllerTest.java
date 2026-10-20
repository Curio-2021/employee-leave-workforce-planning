package com.anudip.employee_leave.controller;

import com.anudip.employee_leave.service.DepartmentService;
import com.anudip.employee_leave.service.EmployeeService;
import com.anudip.employee_leave.service.HolidayService;
import com.anudip.employee_leave.service.LeavePolicyService;
import com.anudip.employee_leave.service.LeaveTypeService;
import com.anudip.employee_leave.service.WorkforceDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(HomeController.class)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmployeeService employeeService;

    @MockitoBean
    private DepartmentService departmentService;

    @MockitoBean
    private LeaveTypeService leaveTypeService;

    @MockitoBean
    private LeavePolicyService leavePolicyService;

    @MockitoBean
    private HolidayService holidayService;

    @MockitoBean
    private WorkforceDashboardService workforceDashboardService;

    @Test
    void testHomePageReturns200AndResolvesIndexView() throws Exception {
        when(employeeService.countActiveEmployees()).thenReturn(5L);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(containsString("Employee Leave")))
                .andExpect(content().string(containsString("Workforce Availability Planning Platform")))
                .andExpect(content().string(containsString("BE CSE Major Project")));
    }

    @Test
    void testDashboardPathReturns200AndResolvesIndexView() throws Exception {
        when(employeeService.countActiveEmployees()).thenReturn(8L);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(containsString("Employee Leave")));
    }
}
