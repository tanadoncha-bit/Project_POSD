package com.example.itborrow.repository;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Optional;
@Repository
public class ExternalIdentityRepository {
    private final JdbcTemplate jdbc;
    public ExternalIdentityRepository(JdbcTemplate jdbc) {this.jdbc=jdbc;}
    public Optional<Long> userId(String provider,String subject) {
        return jdbc.query("SELECT user_id FROM external_identities WHERE provider=? AND subject=?",(rs,n)->rs.getLong(1),provider,subject).stream().findFirst();
    }
    public void link(String provider,String subject,Long userId) {jdbc.update("INSERT INTO external_identities(provider,subject,user_id) VALUES (?,?,?)",provider,subject,userId);}
    public void markEmailVerified(Long id,String email) {
        jdbc.update("DELETE FROM email_verifications WHERE user_id=?",id);
        jdbc.update("INSERT INTO email_verifications(user_id,email,requested_at,verified_at) VALUES (?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",id,email);
    }
}
