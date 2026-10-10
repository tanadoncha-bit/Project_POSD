package com.example.itborrow.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AvatarRepository {
    private final JdbcTemplate jdbc;

    public AvatarRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public String path(Long id) {
        var paths =
                jdbc.query(
                        "SELECT avatar_path FROM user_profiles WHERE user_id=?",
                        (rs, n) -> rs.getString(1),
                        id);
        return paths.isEmpty() ? null : paths.get(0);
    }

    public void lock(Long id) {
        jdbc.queryForObject("SELECT id FROM users WHERE id=? FOR UPDATE", Long.class, id);
    }

    public void setPath(Long id, String path) {
        if (jdbc.update("UPDATE user_profiles SET avatar_path=? WHERE user_id=?", path, id) == 0)
            jdbc.update(
                    "INSERT INTO user_profiles(user_id,full_name,avatar_path) SELECT id,username,?"
                            + " FROM users WHERE id=?",
                    path,
                    id);
    }
}
