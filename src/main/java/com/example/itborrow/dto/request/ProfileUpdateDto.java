package com.example.itborrow.dto.request;
import jakarta.validation.constraints.*;
public record ProfileUpdateDto(@NotBlank @Email @Size(max=100) String email,
    @NotBlank @Size(max=150) String fullName, @Size(max=20) String phone,
    @Size(max=100) String department) {}
