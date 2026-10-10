package com.example.itborrow.service.impl;

import com.example.itborrow.domain.entity.User;
import com.example.itborrow.exception.ResourceNotFoundException;
import com.example.itborrow.repository.UserRepository;
import com.example.itborrow.service.UserService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User getUserById(Long id) {
        return userRepository
                .findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }
}
