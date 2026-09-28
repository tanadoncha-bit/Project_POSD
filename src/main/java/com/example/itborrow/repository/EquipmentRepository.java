package com.example.itborrow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.itborrow.domain.entity.Equipment;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment,Long> {
    org.springframework.data.domain.Page<Equipment> findByNameContainingIgnoreCaseOrAssetCodeContainingIgnoreCase(String name, String code, org.springframework.data.domain.Pageable pageable);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update Equipment e set e.status = :next where e.id = :id and e.status = :expected")
    int transition(@org.springframework.data.repository.query.Param("id") Long id,
        @org.springframework.data.repository.query.Param("expected") com.example.itborrow.domain.enums.EquipmentStatus expected,
        @org.springframework.data.repository.query.Param("next") com.example.itborrow.domain.enums.EquipmentStatus next);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from Equipment e where e.id = :id")
    java.util.Optional<Equipment> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);

    
}