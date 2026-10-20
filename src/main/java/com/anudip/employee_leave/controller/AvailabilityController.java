package com.anudip.employee_leave.controller;

import com.anudip.employee_leave.dto.DailyAvailabilitySnapshot;
import com.anudip.employee_leave.entity.ConflictSeverity;
import com.anudip.employee_leave.entity.Department;
import com.anudip.employee_leave.service.DepartmentService;
import com.anudip.employee_leave.service.WorkforceAvailabilityService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/availability")
public class AvailabilityController {

    private final WorkforceAvailabilityService workforceAvailabilityService;
    private final DepartmentService departmentService;

    public AvailabilityController(WorkforceAvailabilityService workforceAvailabilityService,
                                  DepartmentService departmentService) {
        this.workforceAvailabilityService = workforceAvailabilityService;
        this.departmentService = departmentService;
    }

    @GetMapping
    public String viewAvailabilityCalendar(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {

        List<Department> departments = departmentService.getAllDepartments();
        model.addAttribute("departments", departments);

        LocalDate now = LocalDate.now();
        if (startDate == null) {
            startDate = now.withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = startDate.plusMonths(1).minusDays(1);
        }

        if (departmentId == null && !departments.isEmpty()) {
            departmentId = departments.get(0).getId();
        }

        model.addAttribute("selectedDepartmentId", departmentId);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        if (departmentId != null) {
            try {
                Department selectedDepartment = departmentService.getDepartmentById(departmentId);
                model.addAttribute("selectedDepartment", selectedDepartment);

                List<DailyAvailabilitySnapshot> timeline = workforceAvailabilityService
                        .getDepartmentAvailabilityTimeline(departmentId, startDate, endDate);
                model.addAttribute("timeline", timeline);

                long criticalCount = timeline.stream()
                        .filter(s -> s.getSeverity() == ConflictSeverity.CRITICAL_STAFFING_SHORTAGE)
                        .count();
                long warningCount = timeline.stream()
                        .filter(s -> s.getSeverity() == ConflictSeverity.WARNING_PENDING_RISK)
                        .count();
                long optimalCount = timeline.stream()
                        .filter(s -> s.isWorkingDay() && s.getSeverity() == ConflictSeverity.NONE)
                        .count();
                long nonWorkingCount = timeline.stream()
                        .filter(s -> !s.isWorkingDay())
                        .count();

                model.addAttribute("criticalCount", criticalCount);
                model.addAttribute("warningCount", warningCount);
                model.addAttribute("optimalCount", optimalCount);
                model.addAttribute("nonWorkingCount", nonWorkingCount);
                model.addAttribute("totalDays", timeline.size());
            } catch (Exception e) {
                model.addAttribute("errorMessage", "Error loading department availability: " + e.getMessage());
            }
        }

        return "availability/calendar";
    }

    @GetMapping("/department/{departmentId}")
    public String viewDailyRoster(
            @PathVariable Long departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {

        if (date == null) {
            date = LocalDate.now();
        }

        Department department = departmentService.getDepartmentById(departmentId);
        DailyAvailabilitySnapshot snapshot = workforceAvailabilityService.getDepartmentDailyAvailability(departmentId, date);

        model.addAttribute("department", department);
        model.addAttribute("date", date);
        model.addAttribute("snapshot", snapshot);

        return "availability/department";
    }
}

