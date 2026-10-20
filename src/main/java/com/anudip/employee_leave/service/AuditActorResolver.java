package com.anudip.employee_leave.service;

import com.anudip.employee_leave.entity.Employee;
import com.anudip.employee_leave.security.CustomUserDetails;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Resolves actor attribution for audit events.
 *
 * Checks SecurityContextHolder.getContext().getAuthentication().
 * If authenticated with a valid principal, attributes the event to that authenticated user.
 * If unauthenticated or running under legacy/test conditions, safely attributes to DEV_UNAUTHENTICATED or SYSTEM_WORKFLOW.
 */
@Component
public class AuditActorResolver {

    private static final String DEV_UNAUTHENTICATED_PREFIX = "DEV_UNAUTHENTICATED: ";
    private static final String SYSTEM_PREFIX = "SYSTEM_WORKFLOW: ";
    private static final String AUTH_PREFIX = "AUTH: ";

    /**
     * Resolves actor attribution for a user/employee action.
     * Prefers active SecurityContext authentication.
     */
    public String resolveUserActor(Employee employee) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (isAuthenticated(auth)) {
            return formatAuthenticatedActor(auth);
        }
        if (employee == null) {
            return DEV_UNAUTHENTICATED_PREFIX + "Anonymous User";
        }
        return DEV_UNAUTHENTICATED_PREFIX + employee.getFullName() + " (" + employee.getEmployeeId() + ")";
    }

    /**
     * Resolves actor attribution from a free-form string or parameter.
     */
    public String resolveUserActor(String actorInput) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (isAuthenticated(auth)) {
            return formatAuthenticatedActor(auth);
        }
        if (actorInput == null || actorInput.trim().isEmpty()) {
            return DEV_UNAUTHENTICATED_PREFIX + "Anonymous Operator";
        }
        String trimmed = actorInput.trim();
        if (trimmed.startsWith("DEV_UNAUTHENTICATED") || trimmed.startsWith("SYSTEM_WORKFLOW") || trimmed.startsWith("AUTH:")) {
            return trimmed;
        }
        return DEV_UNAUTHENTICATED_PREFIX + trimmed;
    }

    /**
     * Resolves a purely internal system/engine event.
     */
    public String resolveSystemActor(String componentName) {
        if (componentName == null || componentName.trim().isEmpty()) {
            return SYSTEM_PREFIX + "Engine";
        }
        return SYSTEM_PREFIX + componentName.trim();
    }

    private boolean isAuthenticated(Authentication auth) {
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }

    private String formatAuthenticatedActor(Authentication auth) {
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails cud) {
            if (cud.getEmployee() != null) {
                return AUTH_PREFIX + cud.getFullName() + " (" + cud.getEmployeeCode() + ") [" + cud.getRole().name() + "]";
            }
            return AUTH_PREFIX + cud.getUsername() + " [" + cud.getRole().name() + "]";
        }
        if (principal instanceof UserDetails ud) {
            return AUTH_PREFIX + ud.getUsername();
        }
        return AUTH_PREFIX + auth.getName();
    }
}
