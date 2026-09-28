package com.example.itborrow.repository;

import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;

/** Metadata lives in user_profiles; binary reads exist only for the legacy migration. */
@Repository
public class AvatarRepository {
    private final JdbcTemplate jdbc;
    public AvatarRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public String path(Long id) {
        var paths=jdbc.query("SELECT avatar_path FROM user_profiles WHERE user_id=?",(rs,n)->rs.getString(1),id);
        return paths.isEmpty()?null:paths.get(0);
    }
    public boolean hasLegacy(Long id) { return jdbc.queryForObject("SELECT COUNT(*) FROM user_avatars WHERE user_id=?",Integer.class,id)>0; }
    public byte[] legacy(Long id) {
        var rows=jdbc.query("SELECT image_data FROM user_avatars WHERE user_id=?",(rs,n)->rs.getBytes(1),id);
        return rows.isEmpty()?null:rows.get(0);
    }
    public void lock(Long id) { jdbc.queryForObject("SELECT id FROM users WHERE id=? FOR UPDATE",Long.class,id); }
    public void setPath(Long id,String path) {
        if(jdbc.update("UPDATE user_profiles SET avatar_path=? WHERE user_id=?",path,id)==0)
            jdbc.update("INSERT INTO user_profiles(user_id,full_name,avatar_path) SELECT id,username,? FROM users WHERE id=?",path,id);
    }
    public java.util.List<Long> legacyIds() {
        return jdbc.queryForList("SELECT a.user_id FROM user_avatars a LEFT JOIN user_profiles p ON a.user_id=p.user_id WHERE p.avatar_path IS NULL ORDER BY a.user_id",Long.class);
    }
}
