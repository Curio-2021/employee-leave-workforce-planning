package com.anudip.employee_leave.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                // Static resources and public pages
                .requestMatchers(
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/webjars/**",
                    "/favicon.ico",
                    "/login",
                    "/activate",
                    "/access-denied"
                ).permitAll()

                // HR Admin management and mutations
                .requestMatchers(
                    "/employees/new",
                    "/employees/*/edit",
                    "/employees/*/toggle-status",
                    "/departments/new",
                    "/leave-types/new",
                    "/leave-types/*/edit",
                    "/leave-types/*/toggle-status",
                    "/leave-policies/new",
                    "/leave-policies/*/edit",
                    "/leave-policies/*/toggle-status",
                    "/holidays/new",
                    "/holidays/*/edit",
                    "/holidays/*/delete",
                    "/staffing-rules/*/edit",
                    "/leave-balances/initialize",
                    "/leave-balances/adjust",
                    "/audit-history/**"
                ).hasRole("HR_ADMIN")

                // Manager and HR Admin approvals & policy exceptions
                .requestMatchers(
                    "/leave-requests/pending",
                    "/leave-requests/*/approve",
                    "/leave-requests/*/reject",
                    "/policy-exceptions/**"
                ).hasAnyRole("MANAGER", "HR_ADMIN")

                // General authenticated routes
                .requestMatchers(
                    "/",
                    "/dashboard",
                    "/employees",
                    "/employees/*",
                    "/departments",
                    "/leave-types",
                    "/leave-policies",
                    "/holidays",
                    "/availability/**",
                    "/staffing-rules",
                    "/leave-requests",
                    "/leave-requests/new",
                    "/leave-requests/*",
                    "/leave-requests/*/cancel",
                    "/leave-balances",
                    "/leave-balances/*"
                ).authenticated()

                // All other requests must be authenticated
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(exceptions -> exceptions
                .accessDeniedPage("/access-denied")
            );

        return http.build();
    }
}

