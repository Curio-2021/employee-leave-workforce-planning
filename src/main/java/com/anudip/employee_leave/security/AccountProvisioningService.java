package com.anudip.employee_leave.security;

import com.anudip.employee_leave.entity.Employee;
import com.anudip.employee_leave.entity.Role;
import com.anudip.employee_leave.entity.UserAccount;
import com.anudip.employee_leave.exception.InvalidLeaveRequestException;
import com.anudip.employee_leave.exception.ResourceNotFoundException;
import com.anudip.employee_leave.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
public class AccountProvisioningService {

    private static final int ACTIVATION_EXPIRY_HOURS = 48;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountProvisioningService(UserAccountRepository userAccountRepository,
                                     PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Provisions a new user account linked to an employee.
     * The account is created in a DISABLED state awaiting secure single-use activation.
     */
    @Transactional
    public UserAccount createAccountForEmployee(Employee employee, Role role) {
        if (employee == null) {
            throw new IllegalArgumentException("Employee cannot be null");
        }

        String username = employee.getEmployeeId();
        if (userAccountRepository.existsByUsername(username)) {
            return userAccountRepository.findByUsername(username).orElseThrow();
        }

        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setRole(role != null ? role : Role.ROLE_EMPLOYEE);
        account.setEmployee(employee);
        account.setEnabled(false); // Disabled until activated with unique credentials
        account.setPasswordHash("!UNACTIVATED_" + UUID.randomUUID());

        // Generate high-entropy single-use activation token
        byte[] randomBytes = new byte[24];
        SECURE_RANDOM.nextBytes(randomBytes);
        String token = UUID.randomUUID().toString().replace("-", "") + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        account.setActivationToken(token);
        account.setActivationTokenExpiry(LocalDateTime.now().plusHours(ACTIVATION_EXPIRY_HOURS));

        return userAccountRepository.save(account);
    }

    /**
     * Activates an account using a valid, non-expired single-use activation token.
     */
    @Transactional
    public UserAccount activateAccount(String token, String rawPassword) {
        if (token == null || token.isBlank()) {
            throw new InvalidLeaveRequestException("Activation token is required.");
        }
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new InvalidLeaveRequestException("Password must be at least 8 characters long.");
        }

        UserAccount account = userAccountRepository.findByActivationToken(token.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or already consumed activation token."));

        if (account.getActivationTokenExpiry() == null || LocalDateTime.now().isAfter(account.getActivationTokenExpiry())) {
            throw new InvalidLeaveRequestException("Activation token has expired. Please contact HR for a new token.");
        }

        // Secure BCrypt password hashing - never store plaintext
        account.setPasswordHash(passwordEncoder.encode(rawPassword));
        account.setEnabled(true);
        account.setActivationToken(null);
        account.setActivationTokenExpiry(null);
        account.setFailedLoginAttempts(0);
        account.setLockedUntil(null);

        return userAccountRepository.save(account);
    }

    /**
     * Directly provisions an active account for testing or controlled bootstrapping.
     */
    @Transactional
    public UserAccount provisionDirectActiveAccount(String username, String rawPassword, Role role, Employee employee) {
        if (userAccountRepository.existsByUsername(username)) {
            UserAccount existing = userAccountRepository.findByUsername(username).orElseThrow();
            existing.setPasswordHash(passwordEncoder.encode(rawPassword));
            existing.setRole(role);
            existing.setEnabled(true);
            existing.setFailedLoginAttempts(0);
            existing.setLockedUntil(null);
            return userAccountRepository.save(existing);
        }

        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setPasswordHash(passwordEncoder.encode(rawPassword));
        account.setRole(role);
        account.setEmployee(employee);
        account.setEnabled(true);
        account.setFailedLoginAttempts(0);

        return userAccountRepository.save(account);
    }
}
