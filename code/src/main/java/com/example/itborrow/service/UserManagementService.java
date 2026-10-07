package com.example.itborrow.service;

import com.example.itborrow.domain.enums.Role;
import com.example.itborrow.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

/** Application contract; persistence and orchestration reside in its implementation. */
public interface UserManagementService {
    void requireAdmin();
    org.springframework.data.domain.Page<com.example.itborrow.domain.entity.User> findAccounts(int page,
            String keyword);
    Role changeRole(Long id, Role role);
    java.util.List<java.util.Map<String, Object>> history();
}
