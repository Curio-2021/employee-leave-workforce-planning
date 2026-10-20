package com.anudip.employee_leave.security;

import com.anudip.employee_leave.entity.Role;
import com.anudip.employee_leave.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class AdminAccountInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final UserAccountRepository userAccountRepository;
    private final AccountProvisioningService accountProvisioningService;

    @Value("${app.security.admin.username:${APP_ADMIN_USERNAME:admin}}")
    private String adminUsername;

    @Value("${app.security.admin.password:${APP_ADMIN_PASSWORD:}}")
    private String adminPassword;

    public AdminAccountInitializer(UserAccountRepository userAccountRepository,
                                  AccountProvisioningService accountProvisioningService) {
        this.userAccountRepository = userAccountRepository;
        this.accountProvisioningService = accountProvisioningService;
    }

    @Override
    public void run(String... args) {
        if (userAccountRepository.countByRole(Role.ROLE_HR_ADMIN) == 0) {
            if (adminPassword == null || adminPassword.isBlank()) {
                log.warn("No HR Admin accounts detected in database and no APP_ADMIN_PASSWORD provided. " +
                        "Skipping automated administrator creation. Set APP_ADMIN_PASSWORD environment variable to bootstrap an administrator.");
                return;
            }
            log.info("No HR Admin accounts detected in database. Initializing bootstrap administrator [{}]...", adminUsername);
            accountProvisioningService.provisionDirectActiveAccount(adminUsername, adminPassword, Role.ROLE_HR_ADMIN, null);
            log.info("Bootstrap administrator account [{}] initialized successfully.", adminUsername);
        }
    }
}
