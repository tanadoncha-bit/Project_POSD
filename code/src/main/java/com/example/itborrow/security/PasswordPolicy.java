package com.example.itborrow.security;

import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {
    public void validate(String password, String confirm) {
        if (password == null || !password.equals(confirm))
            throw new IllegalArgumentException("Passwords do not match.");
        if (password.length() < 8)
            throw new IllegalArgumentException("Use at least 8 characters.");
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("Password must not exceed 72 UTF-8 bytes.");
    }
}
