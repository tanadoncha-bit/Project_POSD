package com.example.itborrow.controller.api;

import com.example.itborrow.service.EmailVerificationService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/profile/verification")
public class EmailVerificationController {
    private final EmailVerificationService service;

    public EmailVerificationController(EmailVerificationService service) {
        this.service = service;
    }

    @GetMapping
    public Map<String, Boolean> status() {
        return service.status();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void request() {
        service.request();
    }

    public record Code(String token) {
    }

    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirm(@RequestBody Code code) {
        service.confirm(code.token());
    }
}
