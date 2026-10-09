package com.example.itborrow.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EquipmentRepairRepository {
    private final JdbcTemplate jdbc;

    public EquipmentRepairRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void record(Long id, String actor, String note) {
        jdbc.update(
                "INSERT INTO equipment_repairs(equipment_id,actor_username,note) VALUES (?,?,?)",
                id,
                actor,
                note);
    }
}
