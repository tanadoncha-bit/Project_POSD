package com.example.itborrow.dto.request;

import jakarta.validation.constraints.*;

public record RegistrationDto(
        @NotBlank
                @Pattern(
                        regexp = "[A-Za-z0-9_.-]{3,50}",
                        message =
                                "Username must be 3-50 letters, numbers, dots, underscores or"
                                        + " hyphens.")
                String username,
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank
                @Size(
                        min = 8,
                        max = 72,
                        message = "Password must have 8-72 characters (at most 72 UTF-8 bytes).")
                String password,
        @NotBlank String confirmPassword,
        @NotBlank @Size(max = 150) String fullName,
        @Size(max = 20) String phone,
        @Size(max = 100) String department) {
    @Override
    public String toString() {
        return "RegistrationDto[credentials redacted]";
    }
}
