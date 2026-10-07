package com.example.itborrow.controller.api;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import java.util.Map;

@RestController
public class AuthProviderController {
    private final ObjectProvider<ClientRegistrationRepository> registrations;
    private final com.example.itborrow.service.GoogleAccountService accounts;

    public AuthProviderController(ObjectProvider<ClientRegistrationRepository> registrations,
            com.example.itborrow.service.GoogleAccountService accounts) {
        this.registrations = registrations;
        this.accounts = accounts;
    }

    @GetMapping("/api/v1/auth/providers")
    public Map<String, Boolean> status(java.security.Principal principal) {
        boolean enabled = registrations.getIfAvailable() != null;
        return Map.of("google", enabled, "kku", false, "googleLinked",
                enabled && principal != null && accounts.linked(principal.getName()));
    }
}
