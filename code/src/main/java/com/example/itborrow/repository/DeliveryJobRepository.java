package com.example.itborrow.repository;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
@Repository
public class DeliveryJobRepository {
    private final JdbcTemplate jdbc;
    public DeliveryJobRepository(JdbcTemplate jdbc) {this.jdbc=jdbc;}
    public void enqueue(String kind,String recipient,String subject,String payload) {jdbc.update("INSERT INTO delivery_jobs(kind,recipient,subject,payload) VALUES (?,?,?,?)",kind,recipient,subject,payload);}
    public List<Map<String,Object>> lockPending(boolean emailAvailable) {return jdbc.queryForList("SELECT * FROM delivery_jobs WHERE completed=false AND attempts<10 AND next_attempt<=CURRENT_TIMESTAMP AND (kind<>'EMAIL' OR ?=true) ORDER BY id LIMIT 5 FOR UPDATE SKIP LOCKED",emailAvailable);}
    public void complete(long id) {jdbc.update("UPDATE delivery_jobs SET completed=true,payload='',last_error=NULL WHERE id=?",id);}
    public void retry(long id,RuntimeException error) {jdbc.update("UPDATE delivery_jobs SET attempts=attempts+1,next_attempt=?,last_error=? WHERE id=?",java.sql.Timestamp.from(java.time.Instant.now().plusSeconds(60)),error.getClass().getSimpleName(),id);}
}
