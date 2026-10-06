package com.example.itborrow.controller.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.itborrow.domain.entity.User;
import com.example.itborrow.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final com.example.itborrow.mapper.UserMapper mapper;
    private final UserService userService;
    private final com.example.itborrow.service.AccountService accounts;

    public UserController(UserService userService, com.example.itborrow.service.AccountService accounts,
            com.example.itborrow.mapper.UserMapper mapper) {
        this.mapper = mapper;
        this.userService = userService;
        this.accounts = accounts;
    }

    // Read: ดึงข้อมูลผู้ใช้งานทั้งหมด
    @GetMapping
    public ResponseEntity<List<com.example.itborrow.dto.response.UserResponseDto>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users.stream().map(mapper::toResponse).toList());
    }

    // Read: ดึงข้อมูลผู้ใช้งานตาม ID
    @GetMapping("/{id}")
    public ResponseEntity<com.example.itborrow.dto.response.UserResponseDto> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(mapper.toResponse(user));
    }

    // Create: สร้างผู้ใช้งานใหม่
    @PostMapping
    public ResponseEntity<com.example.itborrow.dto.response.UserResponseDto> createUser(
            @jakarta.validation.Valid @RequestBody com.example.itborrow.dto.request.RegistrationDto user) {
        User createdUser = accounts.register(user);
        return new ResponseEntity<>(mapper.toResponse(createdUser), HttpStatus.CREATED);
    }
}