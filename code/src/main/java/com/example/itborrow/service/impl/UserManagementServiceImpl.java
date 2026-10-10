package com.example.itborrow.service.impl;

import com.example.itborrow.config.PaginationProperties;
import com.example.itborrow.domain.entity.User;
import com.example.itborrow.domain.enums.Role;
import com.example.itborrow.repository.RoleAuditRepository;
import com.example.itborrow.repository.UserRepository;
import com.example.itborrow.service.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class UserManagementServiceImpl implements UserManagementService {
    private final PaginationProperties pagination;

    private final UserRepository users;
    private final CurrentUser current;
    private final RoleAuditRepository audit;

    public UserManagementServiceImpl(
            UserRepository users,
            CurrentUser current,
            RoleAuditRepository audit,
            PaginationProperties pagination) {
        this.pagination = pagination;
        this.users = users;
        this.current = current;
        this.audit = audit;
    }

    public void requireAdmin() {
        if (current.require().getRole() != Role.ADMIN)
            throw new AccessDeniedException("Administrator access required.");
    }

    @Transactional(readOnly = true)
    public Page<User> findAccounts(int page, String keyword) {
        requireAdmin();
        return users.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                keyword.trim(),
                keyword.trim(),
                PageRequest.of(
                        Math.max(0, page), pagination.getUserSize(), Sort.by("username", "id")));
    }

    @Transactional
    public Role changeRole(Long id, Role role) {
        audit.lockManagement();
        requireAdmin();
        var actor = current.require();
        var target =
                users.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Account not found."));
        if (role == null) throw new IllegalArgumentException("Select a role.");
        var previous = target.getRole();
        if (previous == role) return actor.getRole();
        if (previous == Role.ADMIN && role != Role.ADMIN && users.countByRole(Role.ADMIN) <= 1)
            throw new IllegalArgumentException(
                    "Cannot remove the last administrator. Assign another administrator first.");
        target.setRole(role);
        users.saveAndFlush(target);
        audit.record(actor.getUsername(), target.getUsername(), previous.name(), role.name());
        return actor.getRole();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> history() {
        requireAdmin();
        return audit.recent();
    }
}
