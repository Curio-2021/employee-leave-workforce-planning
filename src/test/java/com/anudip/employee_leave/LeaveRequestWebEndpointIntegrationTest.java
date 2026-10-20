package com.anudip.employee_leave;

import com.anudip.employee_leave.dto.LeaveInitializationDTO;
import com.anudip.employee_leave.dto.LeaveRequestDTO;
import com.anudip.employee_leave.entity.Department;
import com.anudip.employee_leave.entity.Employee;
import com.anudip.employee_leave.entity.EmploymentStatus;
import com.anudip.employee_leave.entity.EmploymentType;
import com.anudip.employee_leave.entity.LeavePolicy;
import com.anudip.employee_leave.entity.LeaveRequest;
import com.anudip.employee_leave.entity.LeaveType;
import com.anudip.employee_leave.repository.DepartmentRepository;
import com.anudip.employee_leave.repository.EmployeeRepository;
import com.anudip.employee_leave.repository.LeavePolicyRepository;
import com.anudip.employee_leave.repository.LeaveRequestRepository;
import com.anudip.employee_leave.repository.LeaveTypeRepository;
import com.anudip.employee_leave.service.LeaveBalanceService;
import com.anudip.employee_leave.service.LeaveRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class LeaveRequestWebEndpointIntegrationTest {

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
    private LeaveBalanceService leaveBalanceService;

    @Autowired
    private LeaveRequestService leaveRequestService;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    private LeaveRequest testRequest;
    private Department testDept;
    private Employee manager;

    @BeforeEach
    void setUp() {
        String uid = UUID.randomUUID().toString().substring(0, 6);
        testDept = departmentRepository.save(new Department("Dept " + uid, "Web Test Dept"));

        manager = employeeRepository.save(new Employee("MGR-" + uid, "Manager " + uid,
                "mgr." + uid + "@web.com", testDept, EmploymentType.FULL_TIME,
                LocalDate.of(2021, 1, 1), EmploymentStatus.ACTIVE, null));

        Employee employee = employeeRepository.save(new Employee("EMP-" + uid, "Emp " + uid,
                "emp." + uid + "@web.com", testDept, EmploymentType.FULL_TIME,
                LocalDate.of(2022, 1, 1), EmploymentStatus.ACTIVE, manager));

        LeaveType lt = leaveTypeRepository.save(new LeaveType("LT " + uid, "L" + uid.substring(0, 3).toUpperCase(),
                "Leave Type Web Test", true, true, 10, false));

        leavePolicyRepository.save(new LeavePolicy("Policy " + uid, lt, null, null,
                20, false, 0, 0, 2026, true));

        LeaveInitializationDTO initDTO = new LeaveInitializationDTO();
        initDTO.setEmployeeId(employee.getId());
        initDTO.setLeaveTypeId(lt.getId());
        initDTO.setLeaveYear(2026);
        leaveBalanceService.initializeEntitlements(initDTO);

        LeaveRequestDTO reqDTO = new LeaveRequestDTO(employee.getId(), lt.getId(),
                LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 12), false, "Attending training program");
        testRequest = leaveRequestService.submitLeaveRequest(reqDTO);
    }

    @Test
    void testOpenHomePageReturns200AndRendersIndex() throws Exception {
        mockMvc.perform(get("/").with(user("admin").roles("HR_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(containsString("Employee Leave")))
                .andExpect(content().string(containsString("Workforce Availability Planning Platform")))
                .andExpect(content().string(containsString("Leave Requests")));
    }

    @Test
    void testOpenLeaveRequestsDirectoryReturns200() throws Exception {
        mockMvc.perform(get("/leave-requests").with(user("admin").roles("HR_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("leave-requests/list"))
                .andExpect(content().string(containsString(testRequest.getRequestNumber())))
                .andExpect(content().string(containsString("Leave Requests & Approvals")));
    }

    @Test
    void testOpenLeaveApplicationFormReturns200() throws Exception {
        mockMvc.perform(get("/leave-requests/new").with(user("admin").roles("HR_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("leave-requests/create"))
                .andExpect(content().string(containsString("Apply for Leave")))
                .andExpect(content().string(containsString("Holiday Exclusion")));
    }

    @Test
    void testOpenPendingApprovalsInboxReturns200() throws Exception {
        mockMvc.perform(get("/leave-requests/pending").with(user("admin").roles("HR_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("leave-requests/pending"))
                .andExpect(content().string(containsString("Pending Approvals Inbox")))
                .andExpect(content().string(containsString(testRequest.getRequestNumber())));
    }

    @Test
    void testOpenRequestDetailPageReturns200() throws Exception {
        mockMvc.perform(get("/leave-requests/" + testRequest.getId()).with(user("admin").roles("HR_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("leave-requests/view"))
                .andExpect(content().string(containsString(testRequest.getRequestNumber())))
                .andExpect(content().string(containsString("Attending training program")))
                .andExpect(content().string(containsString("Pending Approval")))
                .andExpect(content().string(containsString("Workforce Availability &amp; Conflict Analysis")));
    }

    @Test
    void testOpenAvailabilityCalendarReturns200() throws Exception {
        mockMvc.perform(get("/availability")
                        .with(user("admin").roles("HR_ADMIN"))
                        .param("departmentId", testDept.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("availability/calendar"))
                .andExpect(content().string(containsString("Workforce Availability Calendar")))
                .andExpect(content().string(containsString(testDept.getName())));
    }

    @Test
    void testOpenDepartmentDailyRosterReturns200() throws Exception {
        mockMvc.perform(get("/availability/department/" + testDept.getId())
                        .with(user("admin").roles("HR_ADMIN"))
                        .param("date", "2026-08-10"))
                .andExpect(status().isOk())
                .andExpect(view().name("availability/department"))
                .andExpect(content().string(containsString(testDept.getName())))
                .andExpect(content().string(containsString("Daily Roster")))
                .andExpect(content().string(containsString("Staffing Assessment")));
    }

    @Test
    void testOpenStaffingRulesListReturns200() throws Exception {
        mockMvc.perform(get("/staffing-rules").with(user("admin").roles("HR_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("staffing-rules/list"))
                .andExpect(content().string(containsString("Department Staffing Rules")))
                .andExpect(content().string(containsString(testDept.getName())));
    }

    @Test
    void testOpenStaffingRuleEditReturns200() throws Exception {
        mockMvc.perform(get("/staffing-rules/" + testDept.getId() + "/edit").with(user("admin").roles("HR_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("staffing-rules/edit"))
                .andExpect(content().string(containsString("Configure Staffing Rule")))
                .andExpect(content().string(containsString(testDept.getName())));
    }

    @Test
    void testViewLeaveRequest_WithStaffingOverride_RendersDedicatedOverrideJustificationAndBadge() throws Exception {
        // Set up approved request with dedicated staffing override justification
        testRequest.approve(manager, "Approver remarks", "Special cross-coverage arranged");
        leaveRequestRepository.save(testRequest);

        mockMvc.perform(get("/leave-requests/" + testRequest.getId()).with(user("admin").roles("HR_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("leave-requests/view"))
                .andExpect(content().string(containsString("Staffing Overridden")))
                .andExpect(content().string(containsString("Staffing Override Justification")))
                .andExpect(content().string(containsString("Special cross-coverage arranged")));
    }
}

