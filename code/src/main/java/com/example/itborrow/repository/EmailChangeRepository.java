package com.example.itborrow.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

@Repository
public class EmailChangeRepository {
    private final JdbcTemplate jdbc;

    public EmailChangeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean recent(long id, Instant since) {
        return jdbc.queryForObject(
                        "SELECT count(*) FROM email_changes WHERE user_id=? AND requested_at>?",
                        Long.class,
                        id,
                        OffsetDateTime.ofInstant(since, ZoneOffset.UTC))
                > 0;
    }

    public void issue(
            long id, String oldEmail, String newEmail, long version, String hash, Instant now) {
        jdbc.update("DELETE FROM email_changes WHERE user_id=?", id);
        jdbc.update(
                "INSERT INTO"
                    + " email_changes(user_id,old_email,new_email,security_version,token_hash,expires_at,requested_at)"
                    + " VALUES (?,?,?,?,?,?,?)",
                id,
                oldEmail,
                newEmail,
                version,
                hash,
                OffsetDateTime.ofInstant(now.plusSeconds(1800), ZoneOffset.UTC),
                OffsetDateTime.ofInstant(now, ZoneOffset.UTC));
    }

    public Optional<Map<String, Object>> find(String hash) {
        return jdbc
                .queryForList(
                        "SELECT * FROM email_changes WHERE token_hash=? AND"
                                + " expires_at>CURRENT_TIMESTAMP",
                        hash)
                .stream()
                .findFirst();
    }

    public Optional<Map<String, Object>> lock(long id, String hash) {
        return jdbc
                .queryForList(
                        "SELECT * FROM email_changes WHERE user_id=? AND token_hash=? AND"
                                + " expires_at>CURRENT_TIMESTAMP FOR UPDATE",
                        id,
                        hash)
                .stream()
                .findFirst();
    }

    public void consume(long id) {
        jdbc.update("DELETE FROM email_changes WHERE user_id=?", id);
    }
}
