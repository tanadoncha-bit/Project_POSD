package com.example.itborrow.dto.request;
import jakarta.validation.constraints.*;
public record LoginSetupDto(@NotBlank @Pattern(regexp="[A-Za-z0-9_.-]{3,50}") String username,
    @NotBlank @Size(min=8,max=72) String password,@NotBlank @Size(min=8,max=72) String confirmPassword) {}
