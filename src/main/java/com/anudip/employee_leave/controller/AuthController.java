package com.anudip.employee_leave.controller;

import com.anudip.employee_leave.exception.InvalidLeaveRequestException;
import com.anudip.employee_leave.exception.ResourceNotFoundException;
import com.anudip.employee_leave.security.AccountProvisioningService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final AccountProvisioningService accountProvisioningService;

    public AuthController(AccountProvisioningService accountProvisioningService) {
        this.accountProvisioningService = accountProvisioningService;
    }

    @GetMapping("/login")
    public String showLoginForm(@RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "logout", required = false) String logout,
                                @RequestParam(value = "activated", required = false) String activated,
                                Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:/";
        }

        if (error != null) {
            model.addAttribute("errorMessage", "Invalid username or password, or account is locked/deactivated.");
        }
        if (logout != null) {
            model.addAttribute("infoMessage", "You have been successfully logged out.");
        }
        if (activated != null) {
            model.addAttribute("successMessage", "Account activated successfully! You may now log in.");
        }

        return "login";
    }

    @GetMapping("/access-denied")
    public String showAccessDenied(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            model.addAttribute("username", auth.getName());
        }
        return "access-denied";
    }

    @GetMapping("/activate")
    public String showActivationForm(@RequestParam(value = "token", required = false) String token,
                                     Model model) {
        model.addAttribute("token", token != null ? token.trim() : "");
        return "activate";
    }

    @PostMapping("/activate")
    public String processActivation(@RequestParam("token") String token,
                                    @RequestParam("password") String password,
                                    @RequestParam("confirmPassword") String confirmPassword,
                                    RedirectAttributes redirectAttributes) {
        if (token == null || token.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Activation token is required.");
            return "redirect:/activate";
        }
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Passwords do not match.");
            return "redirect:/activate?token=" + token.trim();
        }
        if (password.length() < 8) {
            redirectAttributes.addFlashAttribute("errorMessage", "Password must be at least 8 characters long.");
            return "redirect:/activate?token=" + token.trim();
        }

        try {
            accountProvisioningService.activateAccount(token.trim(), password);
            redirectAttributes.addFlashAttribute("successMessage", "Account successfully activated! Please log in with your credentials.");
            return "redirect:/login";
        } catch (InvalidLeaveRequestException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/activate?token=" + token.trim();
        }
    }
}

