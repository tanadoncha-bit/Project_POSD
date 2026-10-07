package com.example.itborrow.controller.web;
import com.example.itborrow.service.EmailVerificationService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpServletResponse;
@Controller
public class EmailVerificationPageController {
    private final EmailVerificationService verification;
    public EmailVerificationPageController(EmailVerificationService verification) {this.verification=verification;}
    @GetMapping("/verify-email") public String page(@RequestParam String token,Model model,HttpServletResponse response) {
        response.setHeader("Referrer-Policy","no-referrer");response.setHeader("Cache-Control","no-store");
        model.addAttribute("valid",verification.validLink(token));model.addAttribute("token",token);return "profile/verify-email";
    }
    @PostMapping("/verify-email") public String confirm(@RequestParam String token,Model model,HttpServletResponse response) {
        response.setHeader("Referrer-Policy","no-referrer");response.setHeader("Cache-Control","no-store");
        try {verification.confirmLink(token);model.addAttribute("verified",true);}
        catch(IllegalArgumentException error) {model.addAttribute("valid",false);}
        return "profile/verify-email";
    }
}
