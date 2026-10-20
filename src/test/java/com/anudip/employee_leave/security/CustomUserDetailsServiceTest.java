package com.anudip.employee_leave.security;

import com.anudip.employee_leave.entity.Department;
import com.anudip.employee_leave.entity.Employee;
import com.anudip.employee_leave.entity.EmploymentStatus;
import com.anudip.employee_leave.entity.EmploymentType;
import com.anudip.employee_leave.entity.Role;
import com.anudip.employee_leave.entity.UserAccount;
import com.anudip.employee_leave.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private UserAccount userAccount;
    private Employee employee;

    @BeforeEach
    void setUp() {
        Department dept = new Department("Engineering", "ENG");
        dept.setId(10L);

        employee = new Employee("EMP001", "Alice Smith", "alice@example.com", dept,
                EmploymentType.FULL_TIME, LocalDate.of(2023, 1, 1), EmploymentStatus.ACTIVE, null);
        employee.setId(1L);

        userAccount = new UserAccount("alice", "$2a$10$hash", Role.ROLE_EMPLOYEE, employee);
        userAccount.setId(100L);
        userAccount.setEnabled(true);
        userAccount.setFailedLoginAttempts(0);
    }

    @Test
    @DisplayName("Successfully load active user by username")
    void loadUserByUsername_Success() {
        when(userAccountRepository.findByUsernameWithDetails("alice")).thenReturn(Optional.of(userAccount));

        UserDetails userDetails = userDetailsService.loadUserByUsername("alice");

        assertThat(userDetails).isInstanceOf(CustomUserDetails.class);
        CustomUserDetails cud = (CustomUserDetails) userDetails;
        assertThat(cud.getUsername()).isEqualTo("alice");
        assertThat(cud.getRole()).isEqualTo(Role.ROLE_EMPLOYEE);
        assertThat(cud.getEmployeeCode()).isEqualTo("EMP001");
        assertThat(cud.getAuthorities()).extracting("authority").containsExactly("ROLE_EMPLOYEE");
    }

    @Test
    @DisplayName("Throw UsernameNotFoundException for non-existent username (without user enumeration)")
    void loadUserByUsername_NotFound() {
        when(userAccountRepository.findByUsernameWithDetails("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Invalid username or password");
    }

    @Test
    @DisplayName("Throw LockedException when account is actively locked")
    void loadUserByUsername_Locked() {
        userAccount.setLockedUntil(LocalDateTime.now().plusMinutes(10));
        when(userAccountRepository.findByUsernameWithDetails("alice")).thenReturn(Optional.of(userAccount));

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("alice"))
                .isInstanceOf(LockedException.class)
                .hasMessageContaining("Account is temporarily locked");
    }

    @Test
    @DisplayName("Load account successfully if lockout period has expired")
    void loadUserByUsername_LockExpired_AllowsLogin() {
        userAccount.setLockedUntil(LocalDateTime.now().minusMinutes(5));
        userAccount.setFailedLoginAttempts(5);
        when(userAccountRepository.findByUsernameWithDetails("alice")).thenReturn(Optional.of(userAccount));

        UserDetails userDetails = userDetailsService.loadUserByUsername("alice");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
    }

    @Test
    @DisplayName("Throw DisabledException when user account is not enabled")
    void loadUserByUsername_DisabledAccount() {
        userAccount.setEnabled(false);
        when(userAccountRepository.findByUsernameWithDetails("alice")).thenReturn(Optional.of(userAccount));

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("alice"))
                .isInstanceOf(DisabledException.class)
                .hasMessageContaining("Account has been disabled");
    }

    @Test
    @DisplayName("Throw DisabledException when linked employee is deactivated")
    void loadUserByUsername_DeactivatedEmployee() {
        employee.setEmploymentStatus(EmploymentStatus.INACTIVE);
        when(userAccountRepository.findByUsernameWithDetails("alice")).thenReturn(Optional.of(userAccount));

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("alice"))
                .isInstanceOf(DisabledException.class)
                .hasMessageContaining("Employee account is deactivated");
    }
}
