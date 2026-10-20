package com.anudip.employee_leave.controller;

import com.anudip.employee_leave.service.DepartmentService;
import com.anudip.employee_leave.service.EmployeeService;
import com.anudip.employee_leave.service.HolidayService;
import com.anudip.employee_leave.service.LeavePolicyService;
import com.anudip.employee_leave.service.LeaveTypeService;
import com.anudip.employee_leave.service.WorkforceDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final EmployeeService employeeService;
    private final DepartmentService departmentService;
    private final LeaveTypeService leaveTypeService;
    private final LeavePolicyService leavePolicyService;
    private final HolidayService holidayService;
    private final WorkforceDashboardService workforceDashboardService;

    public HomeController(EmployeeService employeeService,
                          DepartmentService departmentService,
                          LeaveTypeService leaveTypeService,
                          LeavePolicyService leavePolicyService,
                          HolidayService holidayService) {
        this(employeeService, departmentService, leaveTypeService, leavePolicyService, holidayService, null);
    }

    @Autowired
    public HomeController(EmployeeService employeeService,
                          DepartmentService departmentService,
                          LeaveTypeService leaveTypeService,
                          LeavePolicyService leavePolicyService,
                          HolidayService holidayService,
                          @Autowired(required = false) WorkforceDashboardService workforceDashboardService) {
        this.employeeService = employeeService;
        this.departmentService = departmentService;
        this.leaveTypeService = leaveTypeService;
        this.leavePolicyService = leavePolicyService;
        this.holidayService = holidayService;
        this.workforceDashboardService = workforceDashboardService;
    }

    @GetMapping({"/", "/dashboard"})
    public String home(Model model) {
        model.addAttribute("appName", "Employee Leave & Workforce Availability Planning Platform");
        model.addAttribute("projectSubtitle", "BE CSE Major Project");
        model.addAttribute("activeEmployeeCount", employeeService.countActiveEmployees());
        model.addAttribute("departmentCount", departmentService.getAllDepartments().size());
        model.addAttribute("leaveTypeCount", leaveTypeService.getAllLeaveTypes().size());
        model.addAttribute("leavePolicyCount", leavePolicyService.getAllPolicies().size());
        model.addAttribute("holidayCount", holidayService.getAllHolidays().size());

        if (workforceDashboardService != null) {
            model.addAttribute("dashboard", workforceDashboardService.getDashboardSummary());
        }
        return "index";
    }
}
