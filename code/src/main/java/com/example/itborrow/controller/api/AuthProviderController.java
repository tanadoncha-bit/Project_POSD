package com.example.itborrow.controller.api;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import java.util.Map;
@RestController
public class AuthProviderController {
    private final ObjectProvider<ClientRegistrationRepository> registrations;
    public AuthProviderController(ObjectProvider<ClientRegistrationRepository> registrations) {this.registrations=registrations;}
    @GetMapping("/api/v1/auth/providers") public Map<String,Boolean> status() {return Map.of("google",registrations.getIfAvailable()!=null,"kku",false);}
}
