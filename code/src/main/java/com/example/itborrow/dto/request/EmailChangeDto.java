package com.example.itborrow.dto.request;

import jakarta.validation.constraints.*;

public record EmailChangeDto(
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Size(max = 72) String currentPassword) {}
