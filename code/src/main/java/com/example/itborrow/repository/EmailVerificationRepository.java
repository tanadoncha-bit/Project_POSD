package com.example.itborrow.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Repository
public class EmailVerificationRepository {
    private final JdbcTemplate jdbc;

    public EmailVerificationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean verified(long id, String email) {
        return jdbc.queryForObject(
                        "SELECT count(*) FROM email_verifications WHERE user_id=? AND email=? AND"
                                + " verified_at IS NOT NULL",
                        Long.class,
                        id,
                        email)
                > 0;
    }

    public void lockUser(long id) {
        jdbc.queryForObject("SELECT id FROM users WHERE id=? FOR UPDATE", Long.class, id);
    }

    public boolean recentlyRequested(long id, Instant since) {
        return jdbc.queryForObject(
                        "SELECT count(*) FROM email_verifications WHERE user_id=? AND"
                                + " requested_at>?",
                        Long.class,
                        id,
                        OffsetDateTime.ofInstant(since, ZoneOffset.UTC))
                > 0;
    }

    public void issue(long id, String email, String hash, Instant expires, Instant now) {
        jdbc.update("DELETE FROM email_verifications WHERE user_id=?", id);
        jdbc.update(
                "INSERT INTO email_verifications(user_id,email,token_hash,expires_at,requested_at)"
                        + " VALUES (?,?,?,?,?)",
                id,
                email,
                hash,
                OffsetDateTime.ofInstant(expires, ZoneOffset.UTC),
                OffsetDateTime.ofInstant(now, ZoneOffset.UTC));
    }

    public boolean valid(String hash) {
        return jdbc.queryForObject(
                        "SELECT count(*) FROM email_verifications v JOIN users u ON u.id=v.user_id"
                                + " AND u.email=v.email WHERE v.token_hash=? AND"
                                + " v.expires_at>CURRENT_TIMESTAMP AND v.verified_at IS NULL",
                        Long.class,
                        hash)
                == 1;
    }

    public boolean confirm(String hash, Long userId) {
        String sql =
                "UPDATE email_verifications SET verified_at=CURRENT_TIMESTAMP,token_hash=NULL WHERE"
                    + " token_hash=? AND expires_at>CURRENT_TIMESTAMP AND verified_at IS NULL AND"
                    + " email=(SELECT email FROM users WHERE users.id=email_verifications.user_id)";
        return (userId == null
                        ? jdbc.update(sql, hash)
                        : jdbc.update(sql + " AND user_id=?", hash, userId))
                == 1;
    }

    public void markVerified(long id, String email) {
        jdbc.update("DELETE FROM email_verifications WHERE user_id=?", id);
        jdbc.update(
                "INSERT INTO email_verifications(user_id,email,requested_at,verified_at) VALUES"
                        + " (?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",
                id,
                email);
    }
}
