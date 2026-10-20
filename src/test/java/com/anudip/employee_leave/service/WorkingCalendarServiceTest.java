package com.anudip.employee_leave.service;

import com.anudip.employee_leave.entity.Department;
import com.anudip.employee_leave.entity.DepartmentStaffingRule;
import com.anudip.employee_leave.repository.DepartmentStaffingRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkingCalendarServiceTest {

    @Mock
    private HolidayService holidayService;

    @Mock
    private DepartmentStaffingRuleRepository staffingRuleRepository;

    @InjectMocks
    private WorkingCalendarService workingCalendarService;

    private Department dept;
    private DepartmentStaffingRule rule;

    @BeforeEach
    void setUp() {
        dept = new Department("Engineering", "Eng Unit");
        dept.setId(1L);

        rule = new DepartmentStaffingRule(dept, 2, 50, false, false, true, "Standard Mon-Fri");
    }

    @Test
    void testIsWorkingDay_WeekdayWithoutHoliday_ReturnsTrue() {
        // Wednesday, 2026-10-21
        LocalDate date = LocalDate.of(2026, 10, 21);
        when(holidayService.isHoliday(date)).thenReturn(false);
        when(staffingRuleRepository.findByDepartmentId(1L)).thenReturn(Optional.of(rule));

        assertTrue(workingCalendarService.isWorkingDayForDepartment(1L, date));
    }

    @Test
    void testIsWorkingDay_WeekendStandard_ReturnsFalse() {
        // Saturday, 2026-10-24
        LocalDate date = LocalDate.of(2026, 10, 24);
        rule.setIncludeWeekends(false);
        when(holidayService.isHoliday(date)).thenReturn(false);
        when(staffingRuleRepository.findByDepartmentId(1L)).thenReturn(Optional.of(rule));

        assertFalse(workingCalendarService.isWorkingDayForDepartment(1L, date));
    }

    @Test
    void testIsWorkingDay_WeekendSevenDayOps_ReturnsTrue() {
        // Saturday, 2026-10-24 with includeWeekends = true
        LocalDate date = LocalDate.of(2026, 10, 24);
        rule.setIncludeWeekends(true);
        when(holidayService.isHoliday(date)).thenReturn(false);
        when(staffingRuleRepository.findByDepartmentId(1L)).thenReturn(Optional.of(rule));

        assertTrue(workingCalendarService.isWorkingDayForDepartment(1L, date));
    }

    @Test
    void testIsWorkingDay_CompanyHoliday_ReturnsFalseEvenOnWeekday() {
        // Wednesday, 2026-10-21 is a company holiday
        LocalDate date = LocalDate.of(2026, 10, 21);
        when(holidayService.isHoliday(date)).thenReturn(true);

        assertFalse(workingCalendarService.isWorkingDayForDepartment(1L, date));
    }

    @Test
    void testIsStandardWeekend_DetectsSaturdayAndSunday() {
        LocalDate saturday = LocalDate.of(2026, 10, 24);
        LocalDate sunday = LocalDate.of(2026, 10, 25);
        LocalDate monday = LocalDate.of(2026, 10, 26);

        assertTrue(workingCalendarService.isStandardWeekend(saturday));
        assertTrue(workingCalendarService.isStandardWeekend(sunday));
        assertFalse(workingCalendarService.isStandardWeekend(monday));
    }
}

