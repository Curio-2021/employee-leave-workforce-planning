package com.anudip.employee_leave;

import com.anudip.employee_leave.entity.Department;
import com.anudip.employee_leave.entity.Employee;
import com.anudip.employee_leave.entity.EmploymentStatus;
import com.anudip.employee_leave.entity.EmploymentType;
import com.anudip.employee_leave.entity.Role;
import com.anudip.employee_leave.entity.UserAccount;
import com.anudip.employee_leave.repository.DepartmentRepository;
import com.anudip.employee_leave.repository.EmployeeRepository;
import com.anudip.employee_leave.repository.UserAccountRepository;
import com.anudip.employee_leave.security.AccountProvisioningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthenticationRbacIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountProvisioningService accountProvisioningService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private String testSuffix;
    private String adminUser;
    private String employeeUser;
    private String deactivatedUser;
    private String password = "Password@123!";

    @BeforeEach
    void setUp() {
        testSuffix = UUID.randomUUID().toString().substring(0, 6);

        Department dept = new Department("Test Dept " + testSuffix, "TD_" + testSuffix);
        dept = departmentRepository.save(dept);

        Employee emp = new Employee("EMP_" + testSuffix, "John Test " + testSuffix, "emp." + testSuffix + "@test.com",
                dept, EmploymentType.FULL_TIME, LocalDate.of(2023, 1, 1), EmploymentStatus.ACTIVE, null);
        emp = employeeRepository.save(emp);

        Employee inactiveEmp = new Employee("INACT_" + testSuffix, "Inactive Test " + testSuffix, "inact." + testSuffix + "@test.com",
                dept, EmploymentType.FULL_TIME, LocalDate.of(2023, 1, 1), EmploymentStatus.INACTIVE, null);
        inactiveEmp = employeeRepository.save(inactiveEmp);

        adminUser = "admin_" + testSuffix;
        employeeUser = "emp_" + testSuffix;
        deactivatedUser = "inact_" + testSuffix;

        accountProvisioningService.provisionDirectActiveAccount(adminUser, password, Role.ROLE_HR_ADMIN, null);
        accountProvisioningService.provisionDirectActiveAccount(employeeUser, password, Role.ROLE_EMPLOYEE, emp);
        accountProvisioningService.provisionDirectActiveAccount(deactivatedUser, password, Role.ROLE_EMPLOYEE, inactiveEmp);
    }

    @Test
    @DisplayName("Unauthenticated request to protected page redirects to /login")
    void unauthenticatedAccess_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/leave-requests"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(get("/employees/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("Public authentication and activation pages are accessible without login")
    void publicPages_Accessible() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/activate"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/access-denied"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Form login with valid credentials succeeds and redirects to /")
    void formLogin_Success() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", adminUser)
                        .param("password", password)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("Form login with invalid credentials redirects to /login?error=true")
    void formLogin_InvalidPassword_RedirectsToError() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", adminUser)
                        .param("password", "WrongPassword!")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    @DisplayName("Brute-force lockout triggers after 5 failed login attempts for 15 minutes")
    void bruteForceLockout_TriggersAfter5FailedAttempts() throws Exception {
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/login")
                            .param("username", employeeUser)
                            .param("password", "BadPass" + i)
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?error=true"));
        }

        UserAccount account = userAccountRepository.findByUsername(employeeUser).orElseThrow();
        assertThat(account.getFailedLoginAttempts()).isGreaterThanOrEqualTo(5);
        assertThat(account.getLockedUntil()).isNotNull();
        assertThat(account.isAccountNonLocked()).isFalse();

        // 6th attempt with correct password must still be rejected due to lockout
        mockMvc.perform(post("/login")
                        .param("username", employeeUser)
                        .param("password", password)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    @DisplayName("Deactivated employee cannot authenticate and is rejected")
    void deactivatedEmployee_LoginRejected() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", deactivatedUser)
                        .param("password", password)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    @DisplayName("ROLE_EMPLOYEE is denied access to HR Admin mutation routes (403 Forbidden)")
    void employeeRole_ForbiddenFromHrAdminRoutes() throws Exception {
        mockMvc.perform(get("/employees/new").with(user("regular_emp").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/leave-balances/adjust").with(user("regular_emp").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/leave-balances/initialize").with(user("regular_emp").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/audit-history").with(user("regular_emp").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/leave-requests/pending").with(user("regular_emp").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_HR_ADMIN has access to admin configuration and audit routes")
    void hrAdminRole_CanAccessAdminRoutes() throws Exception {
        mockMvc.perform(get("/employees/new").with(user("admin").roles("HR_ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/leave-balances/adjust").with(user("admin").roles("HR_ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/leave-balances/initialize").with(user("admin").roles("HR_ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/audit-history").with(user("admin").roles("HR_ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/leave-requests/pending").with(user("admin").roles("HR_ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ROLE_MANAGER can access pending approvals but cannot access HR Admin config")
    void managerRole_AccessPendingApprovals() throws Exception {
        mockMvc.perform(get("/leave-requests/pending").with(user("team_lead").roles("MANAGER")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/audit-history").with(user("team_lead").roles("MANAGER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/leave-balances/adjust").with(user("team_lead").roles("MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST request without CSRF token is rejected with 403 Forbidden")
    void csrfProtection_RejectsMissingToken() throws Exception {
        mockMvc.perform(post("/leave-requests/1/cancel")
                        .with(user("emp1").roles("EMPLOYEE"))
                        .param("cancellationReason", "Plans cancelled"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Logout via POST invalidates session and redirects to /login?logout=true")
    void logout_Post_InvalidatesSession() throws Exception {
        mockMvc.perform(post("/logout")
                        .with(user("emp1").roles("EMPLOYEE"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"));
    }
}

