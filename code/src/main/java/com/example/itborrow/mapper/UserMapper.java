package com.example.itborrow.mapper;

import com.example.itborrow.domain.entity.User;
import com.example.itborrow.dto.response.UserResponseDto;

import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public UserResponseDto toResponse(User user) {
        var dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        return dto;
    }
}
