package com.anudip.employee_leave.controller;

import com.anudip.employee_leave.dto.LeaveAdjustmentDTO;
import com.anudip.employee_leave.dto.LeaveInitializationDTO;
import com.anudip.employee_leave.entity.LeaveBalance;
import com.anudip.employee_leave.entity.LeaveTransaction;
import com.anudip.employee_leave.entity.Role;
import com.anudip.employee_leave.exception.InsufficientLeaveBalanceException;
import com.anudip.employee_leave.exception.PolicyConflictException;
import com.anudip.employee_leave.exception.ResourceNotFoundException;
import com.anudip.employee_leave.security.CustomUserDetails;
import com.anudip.employee_leave.security.LeaveSecurityService;
import com.anudip.employee_leave.service.DepartmentService;
import com.anudip.employee_leave.service.EmployeeService;
import com.anudip.employee_leave.service.LeaveBalanceService;
import com.anudip.employee_leave.service.LeaveTypeService;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/leave-balances")
public class LeaveBalanceController {

    private final LeaveBalanceService leaveBalanceService;
    private final EmployeeService employeeService;
    private final DepartmentService departmentService;
    private final LeaveTypeService leaveTypeService;
    private final LeaveSecurityService leaveSecurityService;

    public LeaveBalanceController(LeaveBalanceService leaveBalanceService,
                                  EmployeeService employeeService,
                                  DepartmentService departmentService,
                                  LeaveTypeService leaveTypeService) {
        this(leaveBalanceService, employeeService, departmentService, leaveTypeService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public LeaveBalanceController(LeaveBalanceService leaveBalanceService,
                                  EmployeeService employeeService,
                                  DepartmentService departmentService,
                                  LeaveTypeService leaveTypeService,
                                  @org.springframework.beans.factory.annotation.Autowired(required = false) LeaveSecurityService leaveSecurityService) {
        this.leaveBalanceService = leaveBalanceService;
        this.employeeService = employeeService;
        this.departmentService = departmentService;
        this.leaveTypeService = leaveTypeService;
        this.leaveSecurityService = leaveSecurityService != null ? leaveSecurityService : new LeaveSecurityService(null, null);
    }

    @GetMapping
    public String listBalances(@RequestParam(required = false) Long employeeId,
                               @RequestParam(required = false) Long departmentId,
                               @RequestParam(required = false) Long leaveTypeId,
                               @RequestParam(required = false) Integer leaveYear,
                               Model model) {
        int year = (leaveYear != null) ? leaveYear : 2026;
        CustomUserDetails currentUser = leaveSecurityService.getCurrentUserDetails();

        if (currentUser != null && currentUser.getRole() == Role.ROLE_EMPLOYEE && currentUser.getEmployee() != null) {
            employeeId = currentUser.getEmployee().getId();
            model.addAttribute("employees", List.of(currentUser.getEmployee()));
        } else {
            model.addAttribute("employees", employeeService.getAllEmployees());
        }

        List<LeaveBalance> balances = leaveBalanceService.getFilteredBalances(employeeId, departmentId, leaveTypeId, year);
        model.addAttribute("balances", balances);
        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("leaveTypes", leaveTypeService.getAllLeaveTypes());
        model.addAttribute("selectedEmployeeId", employeeId);
        model.addAttribute("selectedDepartmentId", departmentId);
        model.addAttribute("selectedLeaveTypeId", leaveTypeId);
        model.addAttribute("selectedYear", year);

        return "leave-balances/list";
    }

    @GetMapping("/initialize")
    public String showInitializeForm(Model model) {
        model.addAttribute("initializationDTO", new LeaveInitializationDTO());
        model.addAttribute("employees", employeeService.getAllEmployees());
        model.addAttribute("leaveTypes", leaveTypeService.getActiveLeaveTypes());
        return "leave-balances/initialize";
    }

    @PostMapping("/initialize")
    public String initializeEntitlements(@Valid @ModelAttribute("initializationDTO") LeaveInitializationDTO dto,
                                         BindingResult bindingResult,
                                         Model model,
                                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("employees", employeeService.getAllEmployees());
            model.addAttribute("leaveTypes", leaveTypeService.getActiveLeaveTypes());
            return "leave-balances/initialize";
        }

        try {
            List<LeaveBalance> initialized = leaveBalanceService.initializeEntitlements(dto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Successfully processed leave entitlements for " + initialized.size() + " leave type(s) for year " + dto.getLeaveYear() + ".");
            return "redirect:/leave-balances?employeeId=" + dto.getEmployeeId() + "&leaveYear=" + dto.getLeaveYear();
        } catch (ResourceNotFoundException | PolicyConflictException ex) {
            bindingResult.reject(null, ex.getMessage());
            model.addAttribute("employees", employeeService.getAllEmployees());
            model.addAttribute("leaveTypes", leaveTypeService.getActiveLeaveTypes());
            return "leave-balances/initialize";
        }
    }

    @GetMapping("/adjust")
    public String showAdjustForm(@RequestParam(required = false) Long balanceId,
                                 @RequestParam(required = false) Long employeeId,
                                 @RequestParam(required = false) Long leaveTypeId,
                                 @RequestParam(required = false) Integer leaveYear,
                                 Model model) {
        LeaveAdjustmentDTO dto = new LeaveAdjustmentDTO();
        LeaveBalance balance = null;

        if (balanceId != null) {
            balance = leaveBalanceService.getBalanceById(balanceId);
            dto.setLeaveBalanceId(balance.getId());
            dto.setEmployeeId(balance.getEmployee().getId());
            dto.setLeaveTypeId(balance.getLeaveType().getId());
            dto.setLeaveYear(balance.getLeaveYear());
        } else {
            if (employeeId != null) dto.setEmployeeId(employeeId);
            if (leaveTypeId != null) dto.setLeaveTypeId(leaveTypeId);
            if (leaveYear != null) dto.setLeaveYear(leaveYear);
        }

        model.addAttribute("adjustmentDTO", dto);
        model.addAttribute("currentBalance", balance);
        model.addAttribute("employees", employeeService.getAllEmployees());
        model.addAttribute("leaveTypes", leaveTypeService.getActiveLeaveTypes());
        return "leave-balances/adjust";
    }

    @PostMapping("/adjust")
    public String applyAdjustment(@Valid @ModelAttribute("adjustmentDTO") LeaveAdjustmentDTO dto,
                                  BindingResult bindingResult,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        CustomUserDetails currentUser = leaveSecurityService.getCurrentUserDetails();
        if (currentUser != null) {
            dto.setActor(currentUser.getUsername());
        }

        if (bindingResult.hasErrors()) {
            populateAdjustFormMetadata(model, dto);
            return "leave-balances/adjust";
        }

        try {
            LeaveBalance updated = leaveBalanceService.applyAdjustment(dto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Leave adjustment applied successfully. Resulting balance for " +
                            updated.getLeaveType().getName() + " is " + updated.getAvailableBalance() + " days.");
            return "redirect:/leave-balances?employeeId=" + updated.getEmployee().getId() + "&leaveYear=" + updated.getLeaveYear();
        } catch (InsufficientLeaveBalanceException ex) {
            bindingResult.rejectValue("amount", "insufficient", ex.getMessage());
            populateAdjustFormMetadata(model, dto);
            return "leave-balances/adjust";
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("amount", "invalid", ex.getMessage());
            populateAdjustFormMetadata(model, dto);
            return "leave-balances/adjust";
        } catch (ResourceNotFoundException ex) {
            bindingResult.reject(null, ex.getMessage());
            populateAdjustFormMetadata(model, dto);
            return "leave-balances/adjust";
        }
    }

    @GetMapping("/{id}/transactions")
    public String viewBalanceTransactions(@PathVariable Long id, Model model) {
        LeaveBalance balance = leaveBalanceService.getBalanceById(id);
        if (!leaveSecurityService.canAccessEmployeeData(balance.getEmployee().getId())) {
            throw new AccessDeniedException("You are not authorized to view this transaction ledger.");
        }

        List<LeaveTransaction> transactions = leaveBalanceService.getTransactionsForBalance(id);
        model.addAttribute("balance", balance);
        model.addAttribute("transactions", transactions);
        return "leave-balances/transactions";
    }

    @GetMapping("/transactions")
    public String viewAllTransactions(@RequestParam(required = false) Long employeeId,
                                      @RequestParam(required = false) Long leaveTypeId,
                                      @RequestParam(required = false) Integer leaveYear,
                                      Model model) {
        CustomUserDetails currentUser = leaveSecurityService.getCurrentUserDetails();
        if (currentUser != null && currentUser.getRole() == Role.ROLE_EMPLOYEE && currentUser.getEmployee() != null) {
            employeeId = currentUser.getEmployee().getId();
            model.addAttribute("employees", List.of(currentUser.getEmployee()));
        } else {
            model.addAttribute("employees", employeeService.getAllEmployees());
        }

        List<LeaveTransaction> transactions = leaveBalanceService.getFilteredTransactions(employeeId, leaveTypeId, leaveYear);
        model.addAttribute("transactions", transactions);
        model.addAttribute("leaveTypes", leaveTypeService.getAllLeaveTypes());
        model.addAttribute("selectedEmployeeId", employeeId);
        model.addAttribute("selectedLeaveTypeId", leaveTypeId);
        model.addAttribute("selectedYear", leaveYear);
        return "leave-balances/all-transactions";
    }

    private void populateAdjustFormMetadata(Model model, LeaveAdjustmentDTO dto) {
        model.addAttribute("employees", employeeService.getAllEmployees());
        model.addAttribute("leaveTypes", leaveTypeService.getActiveLeaveTypes());
        if (dto.getLeaveBalanceId() != null) {
            try {
                model.addAttribute("currentBalance", leaveBalanceService.getBalanceById(dto.getLeaveBalanceId()));
            } catch (Exception ignored) {
            }
        }
    }
}
