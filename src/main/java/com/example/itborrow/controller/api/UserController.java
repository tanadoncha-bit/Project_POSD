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

    private final UserService userService;
    private final com.example.itborrow.service.AccountService accounts;

    public UserController(UserService userService, com.example.itborrow.service.AccountService accounts) {
        this.userService = userService;
        this.accounts = accounts;
    }

    // Read: ดึงข้อมูลผู้ใช้งานทั้งหมด
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    // Read: ดึงข้อมูลผู้ใช้งานตาม ID
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    // Create: สร้างผู้ใช้งานใหม่
    @PostMapping
    public ResponseEntity<User> createUser(@jakarta.validation.Valid @RequestBody com.example.itborrow.dto.request.RegistrationDto user) {
        User createdUser = accounts.register(user);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }
}