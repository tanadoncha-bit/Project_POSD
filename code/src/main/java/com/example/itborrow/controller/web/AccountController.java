package com.example.itborrow.controller.web;

import com.example.itborrow.domain.entity.User;
import com.example.itborrow.dto.request.*;
import com.example.itborrow.security.AccountPrincipal;
import com.example.itborrow.service.AccountService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
public class AccountController {
    private final AccountService accounts;

    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute RegistrationDto dto,
            BindingResult errors,
            RedirectAttributes flash) {
        flash.addFlashAttribute(
                "registrationValues",
                Map.of(
                        "username",
                        dto.username() == null ? "" : dto.username(),
                        "email",
                        dto.email() == null ? "" : dto.email(),
                        "fullName",
                        dto.fullName() == null ? "" : dto.fullName(),
                        "phone",
                        dto.phone() == null ? "" : dto.phone(),
                        "department",
                        dto.department() == null ? "" : dto.department()));
        if (errors.hasErrors()) {
            flash.addFlashAttribute(
                    "registrationError", errors.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/";
        }
        try {
            accounts.register(dto);
            flash.addFlashAttribute("accountMessage", "Registration complete. Please sign in.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("registrationError", ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            flash.addFlashAttribute("registrationError", "Username or email already exists.");
        }
        return "redirect:/";
    }

    @PostMapping("/profile")
    public String update(
            @Valid @ModelAttribute ProfileUpdateDto dto,
            BindingResult errors,
            RedirectAttributes flash) {
        if (errors.hasErrors()) {
            flash.addFlashAttribute("accountError", "Check profile fields.");
            return "redirect:/profile";
        }
        try {
            accounts.update(dto);
            flash.addFlashAttribute("accountMessage", "Profile saved.");
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
            flash.addFlashAttribute(
                    "accountError", "Unable to save profile. Check the email address.");
        }
        return "redirect:/profile";
    }

    @GetMapping("/profile/change-password")
    public String passwordForm() {
        return accounts.requiresLoginSetup()
                ? "redirect:/profile/setup-login"
                : "profile/change-password";
    }

    @PostMapping("/profile/change-password")
    public String password(
            @Valid @ModelAttribute PasswordChangeDto dto,
            BindingResult errors,
            RedirectAttributes flash,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (errors.hasErrors()) {
            flash.addFlashAttribute("accountError", "Password must have 8 to 72 characters.");
            return "redirect:/profile/change-password";
        }
        try {
            var account = accounts.changePassword(dto);
            saveAuthentication(account, request, response);
            flash.addFlashAttribute(
                    "accountMessage", "Password changed. Other sessions have been signed out.");
            return "redirect:/profile";
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("accountError", ex.getMessage());
            return "redirect:/profile/change-password";
        }
    }

    @GetMapping("/profile/setup-login")
    public String setupForm() {
        return accounts.requiresLoginSetup() ? "profile/setup-login" : "redirect:/profile";
    }

    @PostMapping("/profile/setup-login")
    public String setup(
            @Valid @ModelAttribute LoginSetupDto dto,
            BindingResult errors,
            RedirectAttributes flash,
            HttpServletRequest request,
            HttpServletResponse response) {
        flash.addFlashAttribute(
                "setupValues",
                Map.of(
                        "username", dto.username() == null ? "" : dto.username(),
                        "fullName", dto.fullName() == null ? "" : dto.fullName(),
                        "phone", dto.phone() == null ? "" : dto.phone(),
                        "department", dto.department() == null ? "" : dto.department()));
        if (errors.hasErrors()) {
            flash.addFlashAttribute(
                    "accountError", "Check your name, username and password fields.");
            return "redirect:/profile/setup-login";
        }
        try {
            var account = accounts.setupLogin(dto);
            saveAuthentication(account, request, response);
            flash.addFlashAttribute("accountMessage", "Registration complete.");
            return "redirect:/profile";
        } catch (IllegalArgumentException | DataIntegrityViolationException error) {
            flash.addFlashAttribute(
                    "accountError",
                    error instanceof IllegalArgumentException
                            ? error.getMessage()
                            : "Username is already taken.");
            return "redirect:/profile/setup-login";
        }
    }

    private void saveAuthentication(
            User account, HttpServletRequest request, HttpServletResponse response) {
        var principal = new AccountPrincipal(account);
        principal.eraseCredentials();
        var auth =
                UsernamePasswordAuthenticationToken.authenticated(
                        principal, null, principal.getAuthorities());
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        if (request.getSession(false) != null) request.changeSessionId();
        new HttpSessionSecurityContextRepository().saveContext(context, request, response);
    }
}
