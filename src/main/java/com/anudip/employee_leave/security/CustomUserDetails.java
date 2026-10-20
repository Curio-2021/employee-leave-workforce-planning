package com.anudip.employee_leave.security;

import com.anudip.employee_leave.entity.Employee;
import com.anudip.employee_leave.entity.Role;
import com.anudip.employee_leave.entity.UserAccount;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class CustomUserDetails implements UserDetails {

    private final UserAccount userAccount;

    public CustomUserDetails(UserAccount userAccount) {
        this.userAccount = userAccount;
    }

    public UserAccount getUserAccount() {
        return userAccount;
    }

    public Employee getEmployee() {
        return userAccount.getEmployee();
    }

    public Long getEmployeeDbId() {
        return userAccount.getEmployee() != null ? userAccount.getEmployee().getId() : null;
    }

    public String getEmployeeCode() {
        return userAccount.getEmployee() != null ? userAccount.getEmployee().getEmployeeId() : null;
    }

    public String getFullName() {
        if (userAccount.getEmployee() != null) {
            return userAccount.getEmployee().getFullName();
        }
        return userAccount.getUsername();
    }

    public Role getRole() {
        return userAccount.getRole();
    }

    public boolean hasRole(String roleName) {
        if (roleName == null) return false;
        String normalized = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
        return userAccount.getRole().name().equals(normalized);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(userAccount.getRole().name()));
    }

    @Override
    public String getPassword() {
        return userAccount.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return userAccount.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return userAccount.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return userAccount.isEnabled();
    }
}
