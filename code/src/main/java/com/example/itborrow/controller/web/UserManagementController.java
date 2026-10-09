package com.example.itborrow.controller.web;

import com.example.itborrow.domain.enums.Role;
import com.example.itborrow.service.UserManagementService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
public class UserManagementController {
    private final UserManagementService management;

    public UserManagementController(UserManagementService management) {
        this.management = management;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String keyword,
            Model model) {
        management.requireAdmin();
        model.addAttribute("accounts", management.findAccounts(page, keyword));
        model.addAttribute("userKeyword", keyword);
        model.addAttribute("roles", Role.values());
        model.addAttribute("history", management.history());
        return "admin/index";
    }

    @PostMapping("/{id}/role")
    public String change(@PathVariable Long id, @RequestParam Role role, RedirectAttributes flash) {
        try {
            Role remainingRole = management.changeRole(id, role);
            flash.addFlashAttribute(
                    "message", "Role updated. Permissions take effect on the next request.");
            if (remainingRole != Role.ADMIN)
                return remainingRole == Role.STAFF ? "redirect:/admin" : "redirect:/";
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/users";
    }
}
