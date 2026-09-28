package com.example.itborrow.controller.web;
import com.example.itborrow.repository.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import java.security.Principal;
@ControllerAdvice(assignableTypes={DashboardController.class, EquipmentPageController.class, AccountController.class, UserManagementController.class})
public class AccountModelAdvice {
    private final com.example.itborrow.service.AvatarService avatars;
    private final UserRepository users; private final UserProfileRepository profiles;
    public AccountModelAdvice(UserRepository users, UserProfileRepository profiles, com.example.itborrow.service.AvatarService avatars) { this.avatars=avatars; this.users=users; this.profiles=profiles; }
    @ModelAttribute public void account(Principal principal, Model model) {
        if (principal != null) users.findByUsername(principal.getName()).ifPresent(user -> {
            model.addAttribute("profileImageUrl", avatars.url(user.getId()));
            model.addAttribute("user", user); model.addAttribute("currentUser",user);
            profiles.findByUserId(user.getId()).ifPresent(profile -> model.addAttribute("profile",profile));
        });
    }
}
