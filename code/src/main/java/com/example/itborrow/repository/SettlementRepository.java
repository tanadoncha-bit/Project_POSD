package com.example.itborrow.repository;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import java.math.BigDecimal;
import java.util.*;
@Repository
public class SettlementRepository {
    private final JdbcTemplate jdbc;
    public SettlementRepository(JdbcTemplate jdbc) {this.jdbc=jdbc;}
    public List<Map<String,Object>> findByRequest(Long id) {return jdbc.queryForList("SELECT amount,reference,paid_at,actor_username FROM settlements WHERE request_id=?",id);}
    public boolean existsByRequest(Long id) {return jdbc.queryForObject("SELECT count(*) FROM settlements WHERE request_id=?",Long.class,id)>0;}
    public void record(Long id,BigDecimal amount,String reference,String actor) {jdbc.update("INSERT INTO settlements(request_id,amount,reference,actor_username) VALUES (?,?,?,?)",id,amount,reference,actor);}
}
