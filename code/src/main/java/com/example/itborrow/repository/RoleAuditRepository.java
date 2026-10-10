package com.example.itborrow.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class RoleAuditRepository {
    private final JdbcTemplate jdbc;

    public RoleAuditRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void lockManagement() {
        jdbc.queryForObject(
                "SELECT id FROM role_management_lock WHERE id=1 FOR UPDATE", Long.class);
    }

    public void record(String actor, String target, String oldRole, String newRole) {
        jdbc.update(
                "INSERT INTO"
                        + " role_audit(actor_username,target_username,old_role,new_role,changed_at)"
                        + " VALUES (?,?,?,?,CURRENT_TIMESTAMP)",
                actor,
                target,
                oldRole,
                newRole);
    }

    public List<Map<String, Object>> recent() {
        return jdbc.queryForList(
                "SELECT actor_username,target_username,old_role,new_role,changed_at FROM role_audit"
                        + " ORDER BY id DESC LIMIT 10");
    }
}
