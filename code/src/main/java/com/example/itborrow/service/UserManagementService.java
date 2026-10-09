package com.example.itborrow.service;

import com.example.itborrow.domain.entity.User;
import com.example.itborrow.domain.enums.Role;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface UserManagementService {
    void requireAdmin();

    Page<User> findAccounts(int page, String keyword);

    Role changeRole(Long id, Role role);

    List<Map<String, Object>> history();
}
