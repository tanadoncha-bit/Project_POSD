package com.example.itborrow.repository;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LockerAccessRepository {
    private final JdbcTemplate jdbc;
    public LockerAccessRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public void revoke(long id) { jdbc.update("UPDATE locker_access SET pin=NULL WHERE request_id=?",id); }
    public void issue(long id, String pin, String slots) { jdbc.update("INSERT INTO locker_access(request_id,pin,slots,opened) VALUES (?,?,?,false)",id,pin,slots); }
    public List<Map<String,Object>> find(long id) { return jdbc.queryForList("SELECT pin,slots,opened FROM locker_access WHERE request_id=?",id); }
    public boolean open(long id, String pin) { return jdbc.update("UPDATE locker_access SET opened=true,pin=NULL WHERE request_id=? AND pin=? AND opened=false",id,pin)==1; }
}
