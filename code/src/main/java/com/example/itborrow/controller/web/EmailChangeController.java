package com.example.itborrow.controller.web;

import com.example.itborrow.dto.request.EmailChangeDto;
import com.example.itborrow.service.EmailChangeService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class EmailChangeController {
    private final EmailChangeService service;

    public EmailChangeController(EmailChangeService service) {
        this.service = service;
    }

    @GetMapping("/profile/change-email")
    public String form() {
        return "profile/change-email";
    }

    @PostMapping("/profile/change-email")
    public String request(@Valid @ModelAttribute EmailChangeDto dto, BindingResult errors, RedirectAttributes flash) {
        try {
            if (errors.hasErrors())
                throw new IllegalArgumentException("Check your email and current password.");
            service.request(dto);
            flash.addFlashAttribute("accountMessage",
                    "Confirmation email queued. Confirm your new address before it changes.");
            return "redirect:/profile";
        } catch (IllegalArgumentException error) {
            flash.addFlashAttribute("accountError", error.getMessage());
            return "redirect:/profile/change-email";
        }
    }

    @GetMapping("/confirm-email-change")
    public String confirmation(@RequestParam(required = false) String token, Model model,
            jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Cache-Control", "no-store");
        model.addAttribute("complete", false);
        model.addAttribute("token", token);
        model.addAttribute("valid", service.valid(token));
        return "profile/confirm-email-change";
    }

    @PostMapping("/confirm-email-change")
    public String confirm(@RequestParam String token, Model model, jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Cache-Control", "no-store");
        model.addAttribute("complete", false);
        model.addAttribute("valid", false);
        try {
            service.confirm(token);
            model.addAttribute("complete", true);
        } catch (IllegalArgumentException | org.springframework.dao.DataIntegrityViolationException error) {
            model.addAttribute("valid", false);
        }
        return "profile/confirm-email-change";
    }
}
