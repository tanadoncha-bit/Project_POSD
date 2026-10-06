package com.example.itborrow.repository;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
@Repository
public class EquipmentImageReferenceRepository {
    private final JdbcTemplate jdbc;
    public EquipmentImageReferenceRepository(JdbcTemplate jdbc) {this.jdbc=jdbc;}
    public boolean isReferenced(String path) {
        String url="/images/equipment/"+path;
        return jdbc.queryForObject("SELECT count(*) FROM borrow_items WHERE snapshot_image_url=?",Long.class,url)>0
            || jdbc.queryForObject("SELECT count(*) FROM equipment WHERE image_url=?",Long.class,url)>0;
    }
}
