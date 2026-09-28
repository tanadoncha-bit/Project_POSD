package com.example.itborrow.service;

import com.example.itborrow.domain.enums.Role;
import com.example.itborrow.repository.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

@Service
public class UserManagementService {
    private final UserRepository users;
    private final CurrentUser current;
    private final JdbcTemplate jdbc;
    public UserManagementService(UserRepository users, CurrentUser current, JdbcTemplate jdbc) {
        this.users=users; this.current=current; this.jdbc=jdbc;
    }
    public void requireAdmin() {
        if (current.require().getRole()!=Role.ADMIN) throw new AccessDeniedException("Administrator access required.");
    }
    @Transactional
    public Role changeRole(Long id, Role role) {
        // Serialize all role changes, including simultaneous demotions by different admins.
        jdbc.queryForObject("SELECT id FROM role_management_lock WHERE id=1 FOR UPDATE", Long.class);
        requireAdmin();
        var actor=current.require();
        var target=users.findById(id).orElseThrow(() -> new IllegalArgumentException("Account not found."));
        if (role==null) throw new IllegalArgumentException("Select a role.");
        var previous=target.getRole();
        if (previous==role) return actor.getRole();
        if (previous==Role.ADMIN && role!=Role.ADMIN && users.countByRole(Role.ADMIN)<=1)
            throw new IllegalArgumentException("Cannot remove the last administrator. Assign another administrator first.");
        target.setRole(role); users.saveAndFlush(target);
        jdbc.update("INSERT INTO role_audit(actor_username,target_username,old_role,new_role,changed_at) VALUES (?,?,?,?,CURRENT_TIMESTAMP)",
            actor.getUsername(),target.getUsername(),previous.name(),role.name());
        return actor.getRole();
    }
    @Transactional(readOnly=true)
    public java.util.List<java.util.Map<String,Object>> history() {
        requireAdmin();
        return jdbc.queryForList("SELECT actor_username,target_username,old_role,new_role,changed_at FROM role_audit ORDER BY id DESC LIMIT 100");
    }
}
