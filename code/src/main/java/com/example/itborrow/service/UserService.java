package com.example.itborrow.service;

import com.example.itborrow.domain.entity.User;

import java.util.List;

public interface UserService {
    List<User> getAllUsers();

    User getUserById(Long id);
}
