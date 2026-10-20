package com.anudip.employee_leave.security;

import com.anudip.employee_leave.entity.EmploymentStatus;
import com.anudip.employee_leave.entity.UserAccount;
import com.anudip.employee_leave.repository.UserAccountRepository;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    public CustomUserDetailsService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.trim().isEmpty()) {
            throw new UsernameNotFoundException("Invalid username or password");
        }

        UserAccount userAccount = userAccountRepository.findByUsernameWithDetails(username.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid username or password"));

        if (!userAccount.isAccountNonLocked()) {
            throw new LockedException("Account is temporarily locked due to repeated failed login attempts.");
        }

        if (userAccount.getEmployee() != null && userAccount.getEmployee().getEmploymentStatus() != EmploymentStatus.ACTIVE) {
            throw new DisabledException("Employee account is deactivated.");
        }

        if (!userAccount.isEnabled()) {
            throw new DisabledException("Account has been disabled.");
        }

        return new CustomUserDetails(userAccount);
    }
}
