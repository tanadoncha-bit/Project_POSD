package com.example.itborrow.controller.web;
import com.example.itborrow.dto.request.*;
import com.example.itborrow.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller
public class AccountController {
    private final AccountService accounts;
    public AccountController(AccountService accounts) { this.accounts=accounts; }
    @PostMapping("/register") public String register(@Valid @ModelAttribute RegistrationDto dto, BindingResult errors, RedirectAttributes flash) {
        flash.addFlashAttribute("registrationValues", java.util.Map.of(
            "username", dto.username() == null ? "" : dto.username(), "email", dto.email() == null ? "" : dto.email(),
            "fullName", dto.fullName() == null ? "" : dto.fullName(), "phone", dto.phone() == null ? "" : dto.phone(),
            "department", dto.department() == null ? "" : dto.department()));
        if (errors.hasErrors()) { flash.addFlashAttribute("registrationError",errors.getAllErrors().get(0).getDefaultMessage()); return "redirect:/"; }
        try { accounts.register(dto); flash.addFlashAttribute("accountMessage","Registration complete. Please sign in."); }
        catch (IllegalArgumentException ex) { flash.addFlashAttribute("registrationError",ex.getMessage()); }
        catch (org.springframework.dao.DataIntegrityViolationException ex) { flash.addFlashAttribute("registrationError","Username or email already exists."); }
        return "redirect:/";
    }
    @PostMapping("/profile") public String update(@Valid @ModelAttribute ProfileUpdateDto dto, BindingResult errors, RedirectAttributes flash) {
        if (errors.hasErrors()) { flash.addFlashAttribute("accountError","Check profile fields."); return "redirect:/profile"; }
        try { accounts.update(dto); flash.addFlashAttribute("accountMessage","Profile saved."); }
        catch (IllegalArgumentException | org.springframework.dao.DataIntegrityViolationException ex) { flash.addFlashAttribute("accountError","Unable to save profile. Check the email address."); }
        return "redirect:/profile";
    }
    @GetMapping("/profile/change-password") public String passwordForm() { return "profile/change-password"; }
    @PostMapping("/profile/change-password") public String password(@Valid @ModelAttribute PasswordChangeDto dto, BindingResult errors, RedirectAttributes flash) {
        if (errors.hasErrors()) { flash.addFlashAttribute("accountError","Password must have 8 to 72 characters."); return "redirect:/profile/change-password"; }
        try { accounts.changePassword(dto); flash.addFlashAttribute("accountMessage","Password changed."); return "redirect:/profile"; }
        catch (IllegalArgumentException ex) { flash.addFlashAttribute("accountError",ex.getMessage()); return "redirect:/profile/change-password"; }
    }
}
