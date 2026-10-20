package com.anudip.employee_leave.security;

import com.anudip.employee_leave.entity.Department;
import com.anudip.employee_leave.entity.Employee;
import com.anudip.employee_leave.entity.EmploymentStatus;
import com.anudip.employee_leave.entity.EmploymentType;
import com.anudip.employee_leave.entity.Role;
import com.anudip.employee_leave.entity.UserAccount;
import com.anudip.employee_leave.exception.InvalidLeaveRequestException;
import com.anudip.employee_leave.exception.ResourceNotFoundException;
import com.anudip.employee_leave.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountProvisioningServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountProvisioningService accountProvisioningService;

    private Employee employee;

    @BeforeEach
    void setUp() {
        Department dept = new Department("IT", "IT");
        dept.setId(1L);
        employee = new Employee("EMP100", "Bob Hope", "bob@example.com", dept,
                EmploymentType.FULL_TIME, LocalDate.of(2024, 1, 1), EmploymentStatus.ACTIVE, null);
        employee.setId(10L);
    }

    @Test
    @DisplayName("Create account for employee in disabled state with high-entropy activation token")
    void createAccountForEmployee_DisabledState() {
        when(userAccountRepository.existsByUsername("EMP100")).thenReturn(false);
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserAccount created = accountProvisioningService.createAccountForEmployee(employee, Role.ROLE_EMPLOYEE);

        assertThat(created.getUsername()).isEqualTo("EMP100");
        assertThat(created.isEnabled()).isFalse();
        assertThat(created.getRole()).isEqualTo(Role.ROLE_EMPLOYEE);
        assertThat(created.getActivationToken()).isNotBlank();
        assertThat(created.getActivationTokenExpiry()).isAfter(LocalDateTime.now().plusHours(40));
        verify(userAccountRepository).save(any(UserAccount.class));
    }

    @Test
    @DisplayName("Activate account successfully with valid token and password")
    void activateAccount_Success() {
        UserAccount account = new UserAccount();
        account.setUsername("EMP100");
        account.setEnabled(false);
        account.setActivationToken("valid-token-123");
        account.setActivationTokenExpiry(LocalDateTime.now().plusHours(24));

        when(userAccountRepository.findByActivationToken("valid-token-123")).thenReturn(Optional.of(account));
        when(passwordEncoder.encode("SecurePass2026!")).thenReturn("$2a$10$encodedHash");
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserAccount activated = accountProvisioningService.activateAccount("valid-token-123", "SecurePass2026!");

        assertThat(activated.isEnabled()).isTrue();
        assertThat(activated.getPasswordHash()).isEqualTo("$2a$10$encodedHash");
        assertThat(activated.getActivationToken()).isNull();
        assertThat(activated.getActivationTokenExpiry()).isNull();
        assertThat(activated.getFailedLoginAttempts()).isEqualTo(0);
    }

    @Test
    @DisplayName("Fail activation when token has expired")
    void activateAccount_ExpiredToken() {
        UserAccount account = new UserAccount();
        account.setActivationToken("expired-token");
        account.setActivationTokenExpiry(LocalDateTime.now().minusHours(1));

        when(userAccountRepository.findByActivationToken("expired-token")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountProvisioningService.activateAccount("expired-token", "SecurePass2026!"))
                .isInstanceOf(InvalidLeaveRequestException.class)
                .hasMessageContaining("Activation token has expired");
    }

    @Test
    @DisplayName("Fail activation when password is shorter than 8 characters")
    void activateAccount_PasswordTooShort() {
        assertThatThrownBy(() -> accountProvisioningService.activateAccount("any-token", "short"))
                .isInstanceOf(InvalidLeaveRequestException.class)
                .hasMessageContaining("Password must be at least 8 characters long");
    }

    @Test
    @DisplayName("Fail activation when token is unknown or already consumed")
    void activateAccount_UnknownToken() {
        when(userAccountRepository.findByActivationToken("bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountProvisioningService.activateAccount("bad-token", "SecurePass2026!"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Invalid or already consumed activation token");
    }
}
