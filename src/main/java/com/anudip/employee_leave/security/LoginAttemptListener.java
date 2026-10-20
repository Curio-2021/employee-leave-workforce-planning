package com.anudip.employee_leave.security;

import com.anudip.employee_leave.entity.UserAccount;
import com.anudip.employee_leave.repository.UserAccountRepository;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class LoginAttemptListener {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    private final UserAccountRepository userAccountRepository;

    public LoginAttemptListener(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @EventListener
    @Transactional
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        String username = event.getAuthentication().getName();
        if (username != null && !username.isBlank()) {
            userAccountRepository.findByUsername(username.trim()).ifPresent(userAccount -> {
                int newAttempts = userAccount.getFailedLoginAttempts() + 1;
                userAccount.setFailedLoginAttempts(newAttempts);
                if (newAttempts >= MAX_FAILED_ATTEMPTS) {
                    userAccount.setLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES));
                }
                userAccountRepository.save(userAccount);
            });
        }
    }

    @EventListener
    @Transactional
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        if (username != null && !username.isBlank()) {
            userAccountRepository.findByUsername(username.trim()).ifPresent(userAccount -> {
                if (userAccount.getFailedLoginAttempts() > 0 || userAccount.getLockedUntil() != null) {
                    userAccount.setFailedLoginAttempts(0);
                    userAccount.setLockedUntil(null);
                    userAccountRepository.save(userAccount);
                }
            });
        }
    }
}
