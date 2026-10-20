package com.anudip.employee_leave.controller;

import com.anudip.employee_leave.dto.HolidayDTO;
import com.anudip.employee_leave.entity.Holiday;
import com.anudip.employee_leave.entity.HolidayType;
import com.anudip.employee_leave.exception.DuplicateResourceException;
import com.anudip.employee_leave.service.HolidayService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
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

@WebMvcTest(HolidayController.class)
class HolidayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HolidayService holidayService;

    @Test
    void testListHolidaysReturns200() throws Exception {
        Holiday h = new Holiday("Republic Day", LocalDate.of(2026, 1, 26), HolidayType.MANDATORY, "Desc", true);
        h.setId(1L);
        when(holidayService.getAllHolidays()).thenReturn(List.of(h));

        mockMvc.perform(get("/holidays"))
                .andExpect(status().isOk())
                .andExpect(view().name("holidays/list"))
                .andExpect(model().attributeExists("holidays"))
                .andExpect(model().attributeExists("holidayDTO"));
    }

    @Test
    void testCreateHolidaySuccessRedirects() throws Exception {
        Holiday h = new Holiday("Independence Day", LocalDate.of(2026, 8, 15), HolidayType.MANDATORY, "Desc", true);
        h.setId(2L);
        when(holidayService.createHoliday(any(HolidayDTO.class))).thenReturn(h);

        mockMvc.perform(post("/holidays")
                        .param("name", "Independence Day")
                        .param("holidayDate", "2026-08-15")
                        .param("holidayType", "MANDATORY")
                        .param("description", "National Holiday"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/holidays"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void testCreateHolidayValidationErrors() throws Exception {
        mockMvc.perform(post("/holidays")
                        .param("name", "")
                        .param("holidayDate", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("holidays/list"))
                .andExpect(model().hasErrors());
    }

    @Test
    void testCreateHolidayDuplicateDateAndNameError() throws Exception {
        when(holidayService.createHoliday(any(HolidayDTO.class)))
                .thenThrow(new DuplicateResourceException("Holiday already exists."));

        mockMvc.perform(post("/holidays")
                        .param("name", "Republic Day")
                        .param("holidayDate", "2026-01-26")
                        .param("holidayType", "MANDATORY"))
                .andExpect(status().isOk())
                .andExpect(view().name("holidays/list"))
                .andExpect(model().attributeHasFieldErrors("holidayDTO", "name"));
    }
}

