package com.anudip.employee_leave.controller;

import com.anudip.employee_leave.entity.Role;
import com.anudip.employee_leave.entity.UserAccount;
import com.anudip.employee_leave.exception.InvalidLeaveRequestException;
import com.anudip.employee_leave.exception.ResourceNotFoundException;
import com.anudip.employee_leave.security.AccountProvisioningService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountProvisioningService accountProvisioningService;

    @Test
    @DisplayName("GET /login returns login view for unauthenticated user")
    void showLoginForm_Unauthenticated() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("GET /login redirects to / if user is already authenticated")
    void showLoginForm_AlreadyAuthenticated_Redirects() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("GET /login with error query param adds errorMessage to model")
    void showLoginForm_WithError() throws Exception {
        mockMvc.perform(get("/login").param("error", "true"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(view().name("login"));
    }

    @Test
    @DisplayName("GET /access-denied returns access-denied view")
    void showAccessDenied() throws Exception {
        mockMvc.perform(get("/access-denied"))
                .andExpect(status().isOk())
                .andExpect(view().name("access-denied"));
    }

    @Test
    @DisplayName("GET /activate displays activation view with token")
    void showActivationForm() throws Exception {
        mockMvc.perform(get("/activate").param("token", "my-token-123"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("token", "my-token-123"))
                .andExpect(view().name("activate"));
    }

    @Test
    @DisplayName("POST /activate with matching passwords activates account and redirects to /login")
    void processActivation_Success() throws Exception {
        UserAccount account = new UserAccount("EMP001", "hash", Role.ROLE_EMPLOYEE, null);
        when(accountProvisioningService.activateAccount(eq("valid-token"), eq("ValidPass123!"))).thenReturn(account);

        mockMvc.perform(post("/activate")
                        .param("token", "valid-token")
                        .param("password", "ValidPass123!")
                        .param("confirmPassword", "ValidPass123!")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(accountProvisioningService).activateAccount("valid-token", "ValidPass123!");
    }

    @Test
    @DisplayName("POST /activate with mismatched passwords redirects with error")
    void processActivation_MismatchedPasswords() throws Exception {
        mockMvc.perform(post("/activate")
                        .param("token", "valid-token")
                        .param("password", "Password123!")
                        .param("confirmPassword", "Different123!")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/activate?token=valid-token"))
                .andExpect(flash().attribute("errorMessage", "Passwords do not match."));
    }

    @Test
    @DisplayName("POST /activate with expired or invalid token handles service exception")
    void processActivation_InvalidToken_HandlesException() throws Exception {
        when(accountProvisioningService.activateAccount(eq("expired-token"), eq("Password123!")))
                .thenThrow(new InvalidLeaveRequestException("Activation token has expired."));

        mockMvc.perform(post("/activate")
                        .param("token", "expired-token")
                        .param("password", "Password123!")
                        .param("confirmPassword", "Password123!")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/activate?token=expired-token"))
                .andExpect(flash().attribute("errorMessage", "Activation token has expired."));
    }
}

