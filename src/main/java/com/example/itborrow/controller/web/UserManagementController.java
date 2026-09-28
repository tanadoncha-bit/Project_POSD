package com.example.itborrow.controller.web;

import com.example.itborrow.domain.enums.Role;
import com.example.itborrow.repository.UserRepository;
import com.example.itborrow.service.UserManagementService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Controller
@RequestMapping("/admin/users")
public class UserManagementController {
    private final UserRepository users;
    private final UserManagementService management;
    public UserManagementController(UserRepository users, UserManagementService management) { this.users=users; this.management=management; }
    @GetMapping public String list(@RequestParam(defaultValue="0") int page, Model model) {
        management.requireAdmin();
        model.addAttribute("accounts",users.findAll(PageRequest.of(Math.max(0,page),25,Sort.by("username"))));
        model.addAttribute("roles",Role.values()); model.addAttribute("history",management.history());
        return "admin/index";
    }
    @PostMapping("/{id}/role") public String change(@PathVariable Long id,@RequestParam Role role,RedirectAttributes flash) {
        try {
            Role remainingRole=management.changeRole(id,role);
            flash.addFlashAttribute("message","Role updated. Permissions take effect on the next request.");
            if(remainingRole!=Role.ADMIN) return remainingRole==Role.STAFF ? "redirect:/admin" : "redirect:/";
        }
        catch (IllegalArgumentException ex) { flash.addFlashAttribute("error",ex.getMessage()); }
        return "redirect:/admin/users";
    }
}
