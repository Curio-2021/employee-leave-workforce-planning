package com.anudip.employee_leave.service;

import com.anudip.employee_leave.entity.Department;
import com.anudip.employee_leave.entity.DepartmentStaffingRule;
import com.anudip.employee_leave.entity.Employee;
import com.anudip.employee_leave.repository.DepartmentStaffingRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Centralized service for determining working days, non-working days, and holidays.
 *
 * Department-Specific Schedule Support:
 * - Each department can configure whether weekends (Saturday and Sunday) are working days
 *   via DepartmentStaffingRule#isIncludeWeekends().
 * - Standard business departments (e.g., Engineering, HR, Finance) default to Monday–Friday.
 * - Continuous / 24x7 departments (e.g., Operations, Support) can configure weekend inclusion.
 *
 * Future Extension Point:
 * - A fuller schedule model (e.g. per-employee shift rotations, custom weekly day sets)
 *   can be plugged in here without altering calling services.
 */
@Service
public class WorkingCalendarService {

    private final HolidayService holidayService;
    private final DepartmentStaffingRuleRepository staffingRuleRepository;

    public WorkingCalendarService(HolidayService holidayService,
                                  DepartmentStaffingRuleRepository staffingRuleRepository) {
        this.holidayService = holidayService;
        this.staffingRuleRepository = staffingRuleRepository;
    }

    /**
     * Checks if a date is an official company holiday.
     */
    @Transactional(readOnly = true)
    public boolean isHoliday(LocalDate date) {
        return holidayService.isHoliday(date);
    }

    /**
     * Checks if a date is a standard weekend day (Saturday or Sunday).
     */
    public boolean isStandardWeekend(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }

    /**
     * Determines whether a given date is a working day for a specific department.
     *
     * Rules:
     * 1. Official holidays are NEVER working days (excluded company-wide).
     * 2. If the department has an active staffing rule with `includeWeekends = true`,
     *    weekends are treated as working days.
     * 3. Fallback: If no rule exists or `includeWeekends = false`, Saturday and Sunday
     *    are treated as non-working days.
     */
    @Transactional(readOnly = true)
    public boolean isWorkingDayForDepartment(Long departmentId, LocalDate date) {
        if (isHoliday(date)) {
            return false;
        }

        boolean includeWeekends = false;
        if (departmentId != null) {
            Optional<DepartmentStaffingRule> ruleOpt = staffingRuleRepository.findByDepartmentId(departmentId);
            if (ruleOpt.isPresent() && ruleOpt.get().isActive()) {
                includeWeekends = ruleOpt.get().isIncludeWeekends();
            }
        }

        if (includeWeekends) {
            return true;
        }

        return !isStandardWeekend(date);
    }

    @Transactional(readOnly = true)
    public boolean isWorkingDayForDepartment(Department department, LocalDate date) {
        return isWorkingDayForDepartment(department != null ? department.getId() : null, date);
    }

    /**
     * Determines whether a given date is a working day for an employee based on their department.
     */
    @Transactional(readOnly = true)
    public boolean isWorkingDayForEmployee(Employee employee, LocalDate date) {
        return isWorkingDayForDepartment(employee != null ? employee.getDepartment() : null, date);
    }

    /**
     * Calculates the net working-day leave duration between two dates for a specific department.
     *
     * Rules:
     * 1. Official holidays are always excluded.
     * 2. Weekends are counted if the department has includeWeekends = true, otherwise skipped.
     * 3. Half-day leave requested on a working day returns 0.50 days; on a non-working day returns 0.00 days.
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateDuration(Long departmentId, LocalDate startDate, LocalDate endDate, boolean halfDay) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date must not be null.");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date.");
        }

        if (halfDay) {
            if (!startDate.equals(endDate)) {
                throw new IllegalArgumentException("Half-day leave can only be requested for a single calendar date.");
            }
            if (!isWorkingDayForDepartment(departmentId, startDate)) {
                return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
            return new BigDecimal("0.50");
        }

        long workingDays = 0;
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            if (isWorkingDayForDepartment(departmentId, current)) {
                workingDays++;
            }
            current = current.plusDays(1);
        }

        return BigDecimal.valueOf(workingDays).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateDuration(Department department, LocalDate startDate, LocalDate endDate, boolean halfDay) {
        return calculateDuration(department != null ? department.getId() : null, startDate, endDate, halfDay);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateDuration(Employee employee, LocalDate startDate, LocalDate endDate, boolean halfDay) {
        return calculateDuration(employee != null ? employee.getDepartment() : null, startDate, endDate, halfDay);
    }
}

