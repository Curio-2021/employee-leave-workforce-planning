package com.anudip.employee_leave.controller;

import com.anudip.employee_leave.entity.AuditEvent;
import com.anudip.employee_leave.service.AuditService;
import com.anudip.employee_leave.service.DepartmentService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/audit-history")
public class AuditHistoryController {

    private final AuditService auditService;
    private final DepartmentService departmentService;

    public AuditHistoryController(AuditService auditService, DepartmentService departmentService) {
        this.auditService = auditService;
        this.departmentService = departmentService;
    }

    @GetMapping
    public String listAuditEvents(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Model model) {

        List<AuditEvent> events = auditService.getFilteredEvents(action, entityType, departmentId, fromDate, toDate);

        model.addAttribute("auditEvents", events);
        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("selectedAction", action != null ? action.trim() : "");
        model.addAttribute("selectedEntityType", entityType != null ? entityType.trim() : "");
        model.addAttribute("selectedDepartmentId", departmentId);
        model.addAttribute("selectedFromDate", fromDate);
        model.addAttribute("selectedToDate", toDate);

        return "audit-history/list";
    }
}

