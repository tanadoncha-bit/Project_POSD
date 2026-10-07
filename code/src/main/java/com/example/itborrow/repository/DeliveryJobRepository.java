package com.example.itborrow.repository;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class DeliveryJobRepository {
    private final JdbcTemplate jdbc;
    private final Clock clock;
    public DeliveryJobRepository(JdbcTemplate jdbc, Clock clock) { this.jdbc = jdbc; this.clock = clock; }

    public void enqueue(String kind, String recipient, String subject, String payload) {
        jdbc.update("INSERT INTO delivery_jobs(kind,recipient,subject,payload,delivery_key) VALUES (?,?,?,?,?)",
                kind, recipient, subject, payload, UUID.randomUUID().toString());
    }

    // One lease at a time prevents later jobs expiring while an earlier network call runs.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Map<String, Object> claim(boolean emailAvailable) {
        var rows = jdbc.queryForList("SELECT * FROM delivery_jobs WHERE completed=false AND attempts<10 "
                + "AND next_attempt<=CURRENT_TIMESTAMP AND (lease_until IS NULL OR lease_until<=CURRENT_TIMESTAMP) "
                + "AND (kind<>'EMAIL' OR ?=true) ORDER BY CASE WHEN kind='EMAIL' "
                + "AND subject='Verify your LeadIT email' THEN 0 ELSE 1 END, id LIMIT 1 FOR UPDATE SKIP LOCKED",
                emailAvailable);
        if (rows.isEmpty()) return Map.of();
        var row = rows.get(0);
        String lease = UUID.randomUUID().toString();
        jdbc.update("UPDATE delivery_jobs SET lease_token=?,lease_until=?,attempts=attempts+1 WHERE id=?",
                lease, java.time.OffsetDateTime.ofInstant(clock.instant().plusSeconds(120), java.time.ZoneOffset.UTC), row.get("id"));
        row.put("lease_token", lease);
        row.put("attempts", ((Number) row.get("attempts")).intValue() + 1);
        return row;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean complete(long id, String lease) {
        return jdbc.update("UPDATE delivery_jobs SET completed=true,payload='',last_error=NULL,"
                + "lease_token=NULL,lease_until=NULL WHERE id=? AND lease_token=?", id, lease) == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void retry(long id, String lease, int attempt, RuntimeException error) {
        long delay = Math.min(300, 5L << Math.min(attempt - 1, 6));
        jdbc.update("UPDATE delivery_jobs SET next_attempt=?,last_error=?,lease_token=NULL,lease_until=NULL "
                + "WHERE id=? AND lease_token=?", java.time.OffsetDateTime.ofInstant(clock.instant().plusSeconds(delay), java.time.ZoneOffset.UTC),
                error.getClass().getSimpleName(), id, lease);
    }

    public Map<String, Object> summary() {
        return jdbc.queryForMap("SELECT COUNT(*) FILTER (WHERE completed=false AND attempts<10) AS pending,"
                + "COUNT(*) FILTER (WHERE completed=false AND attempts>=10) AS failed FROM delivery_jobs");
    }

    public List<Map<String, Object>> failed() {
        return jdbc.queryForList("SELECT id,kind,attempts,last_error,next_attempt FROM delivery_jobs "
                + "WHERE completed=false AND attempts>=10 ORDER BY id DESC LIMIT 50");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean retryFailed(long id) {
        return jdbc.update("UPDATE delivery_jobs SET attempts=0,next_attempt=?,lease_token=NULL,lease_until=NULL "
                + "WHERE id=? AND completed=false AND attempts>=10 AND (lease_until IS NULL OR lease_until<=?)",
                java.time.OffsetDateTime.ofInstant(clock.instant(), java.time.ZoneOffset.UTC), id, java.time.OffsetDateTime.ofInstant(clock.instant(), java.time.ZoneOffset.UTC)) == 1;
    }
}
