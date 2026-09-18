package com.example.itborrow.service;

import java.util.List;
import com.example.itborrow.domain.entity.User;

public interface UserService {
    List<User> getAllUsers();
    User getUserById(Long id);
    User createUser(User user);

}
