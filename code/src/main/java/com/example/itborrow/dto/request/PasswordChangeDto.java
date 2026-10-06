package com.example.itborrow.dto.request;

import jakarta.validation.constraints.*;

public record PasswordChangeDto(@NotBlank String currentPassword,
        @NotBlank @Size(min = 8, max = 72) String password, @NotBlank String confirmPassword) {
    @Override
    public String toString() {
        return "PasswordChangeDto[credentials redacted]";
    }
}
