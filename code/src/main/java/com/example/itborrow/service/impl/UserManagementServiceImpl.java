package com.example.itborrow.service.impl;

import com.example.itborrow.service.*;

import com.example.itborrow.domain.enums.Role;
import com.example.itborrow.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

@Service
public class UserManagementServiceImpl implements UserManagementService {
    private final com.example.itborrow.config.PaginationProperties pagination;

    private final UserRepository users;
    private final CurrentUser current;
    private final com.example.itborrow.repository.RoleAuditRepository audit;

    public UserManagementServiceImpl(UserRepository users, CurrentUser current, com.example.itborrow.repository.RoleAuditRepository audit, com.example.itborrow.config.PaginationProperties pagination) {
        this.pagination=pagination;
        this.users = users;
        this.current = current;
        this.audit = audit;
    }

    public void requireAdmin() {
        if (current.require().getRole() != Role.ADMIN)
            throw new AccessDeniedException("Administrator access required.");
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<com.example.itborrow.domain.entity.User> findAccounts(int page,
            String keyword) {
        requireAdmin();
        return users.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(keyword.trim(), keyword.trim(),
                org.springframework.data.domain.PageRequest.of(Math.max(0, page), pagination.getUserSize(),
                        org.springframework.data.domain.Sort.by("username", "id")));
    }

    @Transactional
    public Role changeRole(Long id, Role role) {
        audit.lockManagement();
        requireAdmin();
        var actor = current.require();
        var target = users.findById(id).orElseThrow(() -> new IllegalArgumentException("Account not found."));
        if (role == null)
            throw new IllegalArgumentException("Select a role.");
        var previous = target.getRole();
        if (previous == role)
            return actor.getRole();
        if (previous == Role.ADMIN && role != Role.ADMIN && users.countByRole(Role.ADMIN) <= 1)
            throw new IllegalArgumentException(
                    "Cannot remove the last administrator. Assign another administrator first.");
        target.setRole(role);
        users.saveAndFlush(target);
        audit.record(actor.getUsername(), target.getUsername(), previous.name(), role.name());
        return actor.getRole();
    }

    @Transactional(readOnly = true)
    public java.util.List<java.util.Map<String, Object>> history() {
        requireAdmin();
        return audit.recent();
    }
}
