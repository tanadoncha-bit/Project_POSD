package com.example.itborrow.dto.request;

import jakarta.validation.constraints.*;

public record LoginSetupDto(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_.-]{3,50}") String username,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(min = 8, max = 72) String confirmPassword,
        @NotBlank @Size(max = 150) String fullName,
        @Size(max = 20) String phone,
        @Size(max = 100) String department) {
    public LoginSetupDto(String username, String password, String confirmPassword) {
        this(username, password, confirmPassword, "Member", "", "");
    }
}
