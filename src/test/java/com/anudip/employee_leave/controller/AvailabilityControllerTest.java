package com.anudip.employee_leave.controller;

import com.anudip.employee_leave.dto.DailyAvailabilitySnapshot;
import com.anudip.employee_leave.entity.ConflictSeverity;
import com.anudip.employee_leave.entity.Department;
import com.anudip.employee_leave.service.DepartmentService;
import com.anudip.employee_leave.service.WorkforceAvailabilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AvailabilityController.class)
class AvailabilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkforceAvailabilityService workforceAvailabilityService;

    @MockitoBean
    private DepartmentService departmentService;

    private Department dept;
    private DailyAvailabilitySnapshot sampleSnapshot;

    @BeforeEach
    void setUp() {
        dept = new Department("Engineering", "Tech Team");
        dept.setId(1L);

        sampleSnapshot = new DailyAvailabilitySnapshot();
        sampleSnapshot.setDate(LocalDate.of(2026, 10, 21));
        sampleSnapshot.setWorkingDay(true);
        sampleSnapshot.setTotalActiveStaff(5);
        sampleSnapshot.setRequiredStaff(2);
        sampleSnapshot.setAvailableStaff(4.0);
        sampleSnapshot.setSeverity(ConflictSeverity.NONE);
        sampleSnapshot.setExplanation("Optimal staffing");
    }

    @Test
    void testViewAvailabilityCalendar_Returns200_WithCalendarView() throws Exception {
        when(departmentService.getAllDepartments()).thenReturn(List.of(dept));
        when(departmentService.getDepartmentById(1L)).thenReturn(dept);
        when(workforceAvailabilityService.getDepartmentAvailabilityTimeline(eq(1L), any(), any()))
                .thenReturn(List.of(sampleSnapshot));

        mockMvc.perform(get("/availability")
                        .param("departmentId", "1")
                        .param("startDate", "2026-10-01")
                        .param("endDate", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(view().name("availability/calendar"))
                .andExpect(model().attributeExists("departments", "selectedDepartment", "timeline", "optimalCount"))
                .andExpect(content().string(containsString("Workforce Availability Calendar")))
                .andExpect(content().string(containsString("Engineering")));
    }

    @Test
    void testViewDailyRoster_Returns200_WithDepartmentView() throws Exception {
        when(departmentService.getDepartmentById(1L)).thenReturn(dept);
        when(workforceAvailabilityService.getDepartmentDailyAvailability(eq(1L), eq(LocalDate.of(2026, 10, 21))))
                .thenReturn(sampleSnapshot);

        mockMvc.perform(get("/availability/department/1")
                        .param("date", "2026-10-21"))
                .andExpect(status().isOk())
                .andExpect(view().name("availability/department"))
                .andExpect(model().attributeExists("department", "date", "snapshot"))
                .andExpect(content().string(containsString("Daily Roster")))
                .andExpect(content().string(containsString("Engineering")))
                .andExpect(content().string(containsString("Staffing Assessment")));
    }
}

