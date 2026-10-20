package com.anudip.employee_leave;

import com.anudip.employee_leave.dto.DashboardSummaryDTO;
import com.anudip.employee_leave.dto.LeaveAdjustmentDTO;
import com.anudip.employee_leave.dto.LeaveApprovalDTO;
import com.anudip.employee_leave.dto.LeaveInitializationDTO;
import com.anudip.employee_leave.dto.LeaveRequestDTO;
import com.anudip.employee_leave.dto.PolicyExceptionItem;
import com.anudip.employee_leave.entity.AuditEvent;
import com.anudip.employee_leave.entity.Department;
import com.anudip.employee_leave.entity.DepartmentStaffingRule;
import com.anudip.employee_leave.entity.Employee;
import com.anudip.employee_leave.entity.EmploymentStatus;
import com.anudip.employee_leave.entity.EmploymentType;
import com.anudip.employee_leave.entity.LeavePolicy;
import com.anudip.employee_leave.entity.LeaveRequest;
import com.anudip.employee_leave.entity.LeaveRequestStatus;
import com.anudip.employee_leave.entity.LeaveType;
import com.anudip.employee_leave.repository.AuditEventRepository;
import com.anudip.employee_leave.repository.DepartmentRepository;
import com.anudip.employee_leave.repository.DepartmentStaffingRuleRepository;
import com.anudip.employee_leave.repository.EmployeeRepository;
import com.anudip.employee_leave.repository.LeaveBalanceRepository;
import com.anudip.employee_leave.repository.LeavePolicyRepository;
import com.anudip.employee_leave.repository.LeaveRequestRepository;
import com.anudip.employee_leave.repository.LeaveTransactionRepository;
import com.anudip.employee_leave.repository.LeaveTypeRepository;
import com.anudip.employee_leave.service.AuditService;
import com.anudip.employee_leave.service.LeaveBalanceService;
import com.anudip.employee_leave.service.LeaveRequestService;
import com.anudip.employee_leave.service.PolicyExceptionService;
import com.anudip.employee_leave.service.WorkforceDashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class WorkforceDashboardAuditIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Autowired
    private LeavePolicyRepository leavePolicyRepository;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private LeaveTransactionRepository leaveTransactionRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private DepartmentStaffingRuleRepository staffingRuleRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private LeaveBalanceService leaveBalanceService;

    @Autowired
    private LeaveRequestService leaveRequestService;

    @Autowired
    private WorkforceDashboardService workforceDashboardService;

    @Autowired
    private PolicyExceptionService policyExceptionService;

    @Autowired
    private AuditService auditService;

    private Department testDept;
    private Employee manager;
    private Employee employee;
    private LeaveType leaveType;
    private String uniqueSuffix;

    @BeforeEach
    void setUp() {
        uniqueSuffix = UUID.randomUUID().toString().substring(0, 6);

        testDept = new Department();
        testDept.setName("Logistics-" + uniqueSuffix);
        testDept.setDescription("Logistics department");
        departmentRepository.save(testDept);

        manager = new Employee();
        manager.setEmployeeId("MGR-" + uniqueSuffix);
        manager.setFullName("Manager " + uniqueSuffix);
        manager.setEmail("mgr-" + uniqueSuffix + "@company.com");
        manager.setDepartment(testDept);
        manager.setEmploymentType(EmploymentType.FULL_TIME);
        manager.setEmploymentStatus(EmploymentStatus.ACTIVE);
        manager.setJoiningDate(LocalDate.of(2022, 1, 1));
        employeeRepository.save(manager);

        employee = new Employee();
        employee.setEmployeeId("EMP-" + uniqueSuffix);
        employee.setFullName("Worker " + uniqueSuffix);
        employee.setEmail("worker-" + uniqueSuffix + "@company.com");
        employee.setDepartment(testDept);
        employee.setEmploymentType(EmploymentType.FULL_TIME);
        employee.setEmploymentStatus(EmploymentStatus.ACTIVE);
        employee.setJoiningDate(LocalDate.of(2023, 1, 1));
        employee.setReportingManager(manager);
        employeeRepository.save(employee);

        leaveType = new LeaveType();
        leaveType.setName("Casual Leave " + uniqueSuffix);
        leaveType.setCode("CL" + uniqueSuffix.substring(0, 3).toUpperCase());
        leaveType.setDescription("Casual leave");
        leaveType.setPaid(true);
        leaveType.setActive(true);
        leaveTypeRepository.save(leaveType);

        LeavePolicy policy = new LeavePolicy();
        policy.setPolicyName("Policy " + uniqueSuffix);
        policy.setLeaveType(leaveType);
        policy.setApplicableDepartment(testDept);
        policy.setApplicableEmploymentType(EmploymentType.FULL_TIME);
        policy.setAnnualAllocation(12);
        policy.setEffectiveYear(2026);
        policy.setActive(true);
        leavePolicyRepository.save(policy);

        LeaveInitializationDTO initDTO = new LeaveInitializationDTO();
        initDTO.setEmployeeId(employee.getId());
        initDTO.setLeaveYear(2026);
        leaveBalanceService.initializeEntitlements(initDTO);
    }

    @Test
    void testDashboardWebEndpoint_Returns200WithAllSections() throws Exception {
        mockMvc.perform(get("/").with(user("admin").roles("HR_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("dashboard"))
                .andExpect(content().string(containsString("Today's Department Workforce Availability")))
                .andExpect(content().string(containsString("Action Queue (Pending Approvals)")))
                .andExpect(content().string(containsString("Upcoming Leaves (Next 14 Days)")));

        mockMvc.perform(get("/dashboard").with(user("admin").roles("HR_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("dashboard"));
    }

    @Test
    void testPolicyExceptionsWebEndpoint_Returns200() throws Exception {
        mockMvc.perform(get("/policy-exceptions").with(user("admin").roles("HR_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("policy-exceptions/list"))
                .andExpect(model().attributeExists("exceptions", "counts", "departments"))
                .andExpect(content().string(containsString("Policy Exceptions &amp; Overrides")));
    }

    @Test
    void testAuditHistoryWebEndpoint_Returns200() throws Exception {
        mockMvc.perform(get("/audit-history").with(user("admin").roles("HR_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("audit-history/list"))
                .andExpect(model().attributeExists("auditEvents", "departments"))
                .andExpect(content().string(containsString("System Audit Trail")))
                .andExpect(content().string(containsString("DEV_UNAUTHENTICATED:")));
    }

    @Test
    void testLeaveRequestLifecycle_RecordsAuditEvents() {
        LocalDate startDate = LocalDate.of(2026, 11, 2); // Monday
        LocalDate endDate = LocalDate.of(2026, 11, 4);   // Wednesday

        LeaveRequestDTO dto = new LeaveRequestDTO();
        dto.setEmployeeId(employee.getId());
        dto.setLeaveTypeId(leaveType.getId());
        dto.setStartDate(startDate);
        dto.setEndDate(endDate);
        dto.setReason("Personal project leave");

        LeaveRequest submitted = leaveRequestService.submitLeaveRequest(dto);
        assertThat(submitted.getId()).isNotNull();

        // Verify audit event for submission
        List<AuditEvent> submitEvents = auditEventRepository.findAll().stream()
                .filter(e -> "LEAVE_REQUEST_SUBMITTED".equals(e.getAction()) && submitted.getRequestNumber().equals(e.getEntityId()))
                .toList();
        assertThat(submitEvents).hasSize(1);
        assertThat(submitEvents.get(0).getActor()).startsWith("DEV_UNAUTHENTICATED: ");

        // Approve request
        LeaveRequest approved = leaveRequestService.approveLeaveRequest(submitted.getId(), manager.getId(), "Standard approval");
        assertThat(approved.getStatus()).isEqualTo(LeaveRequestStatus.APPROVED);

        // Verify audit event for standard approval
        List<AuditEvent> approvalEvents = auditEventRepository.findAll().stream()
                .filter(e -> "LEAVE_REQUEST_APPROVED".equals(e.getAction()) && submitted.getRequestNumber().equals(e.getEntityId()))
                .toList();
        assertThat(approvalEvents).hasSize(1);
        assertThat(approvalEvents.get(0).getActor()).startsWith("DEV_UNAUTHENTICATED: ");
    }

    @Test
    void testManagerialStaffingOverride_RecordsStaffingOverrideAuditAndException() {
        // Configure strict department staffing rule requiring 2 active staff
        // Department has 2 total active staff (manager, employee)
        DepartmentStaffingRule rule = new DepartmentStaffingRule();
        rule.setDepartment(testDept);
        rule.setMinimumStaffCount(2);
        rule.setStrictEnforcement(false); // Advisory mode
        rule.setActive(true);
        staffingRuleRepository.save(rule);

        LocalDate startDate = LocalDate.of(2026, 11, 9); // Monday
        LocalDate endDate = LocalDate.of(2026, 11, 10);  // Tuesday

        LeaveRequestDTO dto = new LeaveRequestDTO();
        dto.setEmployeeId(employee.getId());
        dto.setLeaveTypeId(leaveType.getId());
        dto.setStartDate(startDate);
        dto.setEndDate(endDate);
        dto.setReason("Urgent errand");

        LeaveRequest submitted = leaveRequestService.submitLeaveRequest(dto);

        // Approve with managerial override
        LeaveRequest approved = leaveRequestService.approveLeaveRequest(
                submitted.getId(),
                manager.getId(),
                "Override granted",
                true,
                "Cover arranged by floating staff from central logistics team."
        );
        assertThat(approved.getStatus()).isEqualTo(LeaveRequestStatus.APPROVED);
        assertThat(approved.getStaffingOverrideJustification()).isNotNull();

        // Verify audit event
        List<AuditEvent> overrideEvents = auditEventRepository.findAll().stream()
                .filter(e -> "LEAVE_REQUEST_OVERRIDDEN".equals(e.getAction()) && submitted.getRequestNumber().equals(e.getEntityId()))
                .toList();
        assertThat(overrideEvents).hasSize(1);
        assertThat(overrideEvents.get(0).getDetails()).contains("Cover arranged by floating staff");

        // Verify it shows up in PolicyExceptionService
        List<PolicyExceptionItem> exceptions = policyExceptionService.getPolicyExceptions(
                PolicyExceptionService.CAT_AUTHORIZED_OVERRIDE, testDept.getId(), 2026);
        assertThat(exceptions).isNotEmpty();
        assertThat(exceptions.get(0).getReferenceNumber()).isEqualTo(submitted.getRequestNumber());
        assertThat(exceptions.get(0).getJustification()).contains("Cover arranged");
    }

    @Test
    void testManualBalanceAdjustment_RecordsAdjustmentAuditAndException() {
        LeaveAdjustmentDTO adj = new LeaveAdjustmentDTO();
        adj.setEmployeeId(employee.getId());
        adj.setLeaveTypeId(leaveType.getId());
        adj.setLeaveYear(2026);
        adj.setAdjustmentType("CREDIT");
        adj.setAmount(new java.math.BigDecimal("2.00"));
        adj.setReason("Special commendation allowance");
        adj.setActor("HR_DIRECTOR");

        leaveBalanceService.applyAdjustment(adj);

        List<AuditEvent> adjEvents = auditEventRepository.findAll().stream()
                .filter(e -> "BALANCE_MANUALLY_ADJUSTED".equals(e.getAction()))
                .toList();
        assertThat(adjEvents).isNotEmpty();

        List<PolicyExceptionItem> exceptions = policyExceptionService.getPolicyExceptions(
                PolicyExceptionService.CAT_ADMINISTRATIVE_ADJUSTMENT, testDept.getId(), 2026);
        assertThat(exceptions).isNotEmpty();
        assertThat(exceptions.get(0).getJustification()).isEqualTo("Special commendation allowance");
    }

    @Test
    void test14DayUpcomingLeaves_IncludesOverlappingRequests() {
        LocalDate today = LocalDate.now();

        // Create an approved leave request that started 2 days ago and ends 3 days from now (overlaps today)
        LeaveRequest req = new LeaveRequest();
        req.setRequestNumber("LR-OVERLAP-1");
        req.setEmployee(employee);
        req.setLeaveType(leaveType);
        req.setStartDate(today.minusDays(2));
        req.setEndDate(today.plusDays(3));
        req.setDuration(new java.math.BigDecimal("4.00"));
        req.setStatus(LeaveRequestStatus.APPROVED);
        req.setReason("Medical recovery");
        leaveRequestRepository.save(req);

        DashboardSummaryDTO summary = workforceDashboardService.getDashboardSummary(today);
        List<DashboardSummaryDTO.UpcomingLeaveItem> upcoming = summary.getUpcomingLeaves();

        assertThat(upcoming.stream().anyMatch(u -> "LR-OVERLAP-1".equals(u.getRequestNumber()))).isTrue();
    }
}
