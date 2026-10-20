package com.anudip.employee_leave.controller;

import com.anudip.employee_leave.dto.DepartmentStaffingRuleDTO;
import com.anudip.employee_leave.entity.Department;
import com.anudip.employee_leave.entity.DepartmentStaffingRule;
import com.anudip.employee_leave.service.DepartmentStaffingRuleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(StaffingRuleController.class)
class StaffingRuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DepartmentStaffingRuleService staffingRuleService;

    private DepartmentStaffingRuleDTO sampleDTO;
    private DepartmentStaffingRule savedRule;

    @BeforeEach
    void setUp() {
        Department dept = new Department("Human Resources", "HR Unit");
        dept.setId(2L);

        sampleDTO = new DepartmentStaffingRuleDTO();
        sampleDTO.setId(10L);
        sampleDTO.setDepartmentId(2L);
        sampleDTO.setDepartmentName("Human Resources");
        sampleDTO.setMinimumStaffCount(2);
        sampleDTO.setMinimumStaffPercentage(40);
        sampleDTO.setStrictEnforcement(true);
        sampleDTO.setIncludeWeekends(false);
        sampleDTO.setActive(true);
        sampleDTO.setDescription("HR core policy");

        savedRule = new DepartmentStaffingRule(dept, 2, 40, true, false, true, "HR core policy");
        savedRule.setId(10L);
    }

    @Test
    void testListRules_Returns200_WithListView() throws Exception {
        when(staffingRuleService.getAllDepartmentRules()).thenReturn(List.of(sampleDTO));

        mockMvc.perform(get("/staffing-rules"))
                .andExpect(status().isOk())
                .andExpect(view().name("staffing-rules/list"))
                .andExpect(model().attributeExists("rules"))
                .andExpect(content().string(containsString("Department Staffing Rules")))
                .andExpect(content().string(containsString("Human Resources")));
    }

    @Test
    void testEditRuleForm_Returns200_WithEditView() throws Exception {
        when(staffingRuleService.getRuleDTOByDepartmentId(2L)).thenReturn(sampleDTO);

        mockMvc.perform(get("/staffing-rules/2/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("staffing-rules/edit"))
                .andExpect(model().attributeExists("ruleDTO"))
                .andExpect(content().string(containsString("Configure Staffing Rule")))
                .andExpect(content().string(containsString("Human Resources")));
    }

    @Test
    void testSaveRule_Success_RedirectsToStaffingRules() throws Exception {
        when(staffingRuleService.saveOrUpdateRule(any(DepartmentStaffingRuleDTO.class))).thenReturn(savedRule);

        mockMvc.perform(post("/staffing-rules/2")
                        .param("minimumStaffCount", "2")
                        .param("minimumStaffPercentage", "40")
                        .param("strictEnforcement", "true")
                        .param("includeWeekends", "false")
                        .param("active", "true")
                        .param("description", "HR updated"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staffing-rules"));
    }

    @Test
    void testSaveRule_ValidationFailure_NegativeCount_ReturnsEditView() throws Exception {
        mockMvc.perform(post("/staffing-rules/2")
                        .param("minimumStaffCount", "-5")
                        .param("minimumStaffPercentage", "40"))
                .andExpect(status().isOk())
                .andExpect(view().name("staffing-rules/edit"))
                .andExpect(model().hasErrors());
    }
}

