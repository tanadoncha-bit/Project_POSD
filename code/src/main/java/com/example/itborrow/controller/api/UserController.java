package com.example.itborrow.controller.api;

import com.example.itborrow.domain.entity.User;
import com.example.itborrow.dto.request.RegistrationDto;
import com.example.itborrow.dto.response.UserResponseDto;
import com.example.itborrow.mapper.UserMapper;
import com.example.itborrow.service.AccountService;
import com.example.itborrow.service.UserService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserMapper mapper;
    private final UserService userService;
    private final AccountService accounts;

    public UserController(UserService userService, AccountService accounts, UserMapper mapper) {
        this.mapper = mapper;
        this.userService = userService;
        this.accounts = accounts;
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users.stream().map(mapper::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(mapper.toResponse(user));
    }

    @PostMapping
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody RegistrationDto user) {
        User createdUser = accounts.register(user);
        return new ResponseEntity<>(mapper.toResponse(createdUser), HttpStatus.CREATED);
    }
}
