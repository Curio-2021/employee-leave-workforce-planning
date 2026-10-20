package com.anudip.employee_leave.controller;

import com.anudip.employee_leave.dto.LeaveAdjustmentDTO;
import com.anudip.employee_leave.dto.LeaveInitializationDTO;
import com.anudip.employee_leave.entity.Department;
import com.anudip.employee_leave.entity.Employee;
import com.anudip.employee_leave.entity.EmploymentStatus;
import com.anudip.employee_leave.entity.EmploymentType;
import com.anudip.employee_leave.entity.LeaveBalance;
import com.anudip.employee_leave.entity.LeaveType;
import com.anudip.employee_leave.service.DepartmentService;
import com.anudip.employee_leave.service.EmployeeService;
import com.anudip.employee_leave.service.LeaveBalanceService;
import com.anudip.employee_leave.service.LeaveTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
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

@WebMvcTest(LeaveBalanceController.class)
class LeaveBalanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LeaveBalanceService leaveBalanceService;

    @MockitoBean
    private EmployeeService employeeService;

    @MockitoBean
    private DepartmentService departmentService;

    @MockitoBean
    private LeaveTypeService leaveTypeService;

    private Employee employee;
    private LeaveType leaveType;
    private LeaveBalance balance;

    @BeforeEach
    void setUp() {
        Department dept = new Department("Engineering", "Software");
        dept.setId(1L);

        employee = new Employee("EMP-001", "Jane Doe", "jane@example.com", dept,
                EmploymentType.FULL_TIME, LocalDate.of(2024, 1, 1), EmploymentStatus.ACTIVE, null);
        employee.setId(1L);

        leaveType = new LeaveType("Casual Leave", "CL", "Desc", true, true, 3, false);
        leaveType.setId(1L);

        balance = new LeaveBalance(employee, leaveType, null, 2026);
        balance.setId(1L);
        balance.setGrantedEntitlement(BigDecimal.valueOf(12.00));
        balance.recalculateBalance();
    }

    @Test
    void testListBalancesReturns200() throws Exception {
        when(leaveBalanceService.getFilteredBalances(null, null, null, 2026)).thenReturn(List.of(balance));
        when(employeeService.getAllEmployees()).thenReturn(List.of(employee));
        when(departmentService.getAllDepartments()).thenReturn(Collections.emptyList());
        when(leaveTypeService.getAllLeaveTypes()).thenReturn(List.of(leaveType));

        mockMvc.perform(get("/leave-balances"))
                .andExpect(status().isOk())
                .andExpect(view().name("leave-balances/list"))
                .andExpect(model().attributeExists("balances"))
                .andExpect(model().attributeExists("employees"));
    }

    @Test
    void testShowInitializeFormReturns200() throws Exception {
        when(employeeService.getAllEmployees()).thenReturn(List.of(employee));
        when(leaveTypeService.getActiveLeaveTypes()).thenReturn(List.of(leaveType));

        mockMvc.perform(get("/leave-balances/initialize"))
                .andExpect(status().isOk())
                .andExpect(view().name("leave-balances/initialize"))
                .andExpect(model().attributeExists("initializationDTO"));
    }

    @Test
    void testInitializeEntitlementsSuccessRedirects() throws Exception {
        when(leaveBalanceService.initializeEntitlements(any(LeaveInitializationDTO.class))).thenReturn(List.of(balance));

        mockMvc.perform(post("/leave-balances/initialize")
                        .param("employeeId", "1")
                        .param("leaveYear", "2026")
                        .param("includeCarryForward", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/leave-balances?employeeId=1&leaveYear=2026"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void testShowAdjustFormReturns200() throws Exception {
        when(leaveBalanceService.getBalanceById(1L)).thenReturn(balance);
        when(employeeService.getAllEmployees()).thenReturn(List.of(employee));
        when(leaveTypeService.getActiveLeaveTypes()).thenReturn(List.of(leaveType));

        mockMvc.perform(get("/leave-balances/adjust").param("balanceId", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("leave-balances/adjust"))
                .andExpect(model().attributeExists("adjustmentDTO"))
                .andExpect(model().attributeExists("currentBalance"));
    }

    @Test
    void testApplyAdjustmentCreditSuccessRedirects() throws Exception {
        when(leaveBalanceService.applyAdjustment(any(LeaveAdjustmentDTO.class))).thenReturn(balance);

        mockMvc.perform(post("/leave-balances/adjust")
                        .param("employeeId", "1")
                        .param("leaveTypeId", "1")
                        .param("leaveYear", "2026")
                        .param("adjustmentType", "CREDIT")
                        .param("amount", "2.0")
                        .param("reason", "Weekend on-call comp-off"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/leave-balances?employeeId=1&leaveYear=2026"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void testApplyAdjustmentValidationErrors() throws Exception {
        when(employeeService.getAllEmployees()).thenReturn(List.of(employee));
        when(leaveTypeService.getActiveLeaveTypes()).thenReturn(List.of(leaveType));

        mockMvc.perform(post("/leave-balances/adjust")
                        .param("employeeId", "")
                        .param("amount", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("leave-balances/adjust"))
                .andExpect(model().hasErrors());
    }

    @Test
    void testViewBalanceTransactionsReturns200() throws Exception {
        when(leaveBalanceService.getBalanceById(1L)).thenReturn(balance);
        when(leaveBalanceService.getTransactionsForBalance(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/leave-balances/1/transactions"))
                .andExpect(status().isOk())
                .andExpect(view().name("leave-balances/transactions"))
                .andExpect(model().attributeExists("balance"))
                .andExpect(model().attributeExists("transactions"));
    }

    @Test
    void testViewAllTransactionsReturns200() throws Exception {
        when(leaveBalanceService.getFilteredTransactions(null, null, null)).thenReturn(Collections.emptyList());
        when(employeeService.getAllEmployees()).thenReturn(List.of(employee));
        when(leaveTypeService.getAllLeaveTypes()).thenReturn(List.of(leaveType));

        mockMvc.perform(get("/leave-balances/transactions"))
                .andExpect(status().isOk())
                .andExpect(view().name("leave-balances/all-transactions"))
                .andExpect(model().attributeExists("transactions"));
    }
}

