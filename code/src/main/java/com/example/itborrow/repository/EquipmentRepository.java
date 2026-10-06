package com.example.itborrow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.itborrow.domain.entity.Equipment;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    boolean existsByCategoryId(Long categoryId);

    @org.springframework.data.jpa.repository.Query("select count(e) from Equipment e where lower(trim(e.storageSlot)) = lower(:slot) and (:excludedId is null or e.id <> :excludedId)")
    long countSlotAssignments(String slot, Long excludedId);

    @org.springframework.data.jpa.repository.Query("select e from Equipment e where (:status is null or e.status=:status) and (:keyword='' or lower(e.name) like lower(concat('%',:keyword,'%')) or lower(e.assetCode) like lower(concat('%',:keyword,'%')))")
    org.springframework.data.domain.Page<Equipment> searchInventory(String keyword,
            com.example.itborrow.domain.enums.EquipmentStatus status,
            org.springframework.data.domain.Pageable pageable);

    long countByStatus(com.example.itborrow.domain.enums.EquipmentStatus status);

    java.util.List<Equipment> findByIdInAndStatus(java.util.Collection<Long> ids,
            com.example.itborrow.domain.enums.EquipmentStatus status);

    org.springframework.data.domain.Page<Equipment> findByNameContainingIgnoreCaseOrAssetCodeContainingIgnoreCase(
            String name, String code, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update Equipment e set e.status = :next where e.id = :id and e.status = :expected")
    int transition(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("expected") com.example.itborrow.domain.enums.EquipmentStatus expected,
            @org.springframework.data.repository.query.Param("next") com.example.itborrow.domain.enums.EquipmentStatus next);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from Equipment e where e.id = :id")
    java.util.Optional<Equipment> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);

}