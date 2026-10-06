package com.example.itborrow.dto.request;

import jakarta.validation.constraints.*;

public record CategoryRequestDto(@NotBlank @Size(max = 100) String name, @Size(max = 300) String description) {
}
