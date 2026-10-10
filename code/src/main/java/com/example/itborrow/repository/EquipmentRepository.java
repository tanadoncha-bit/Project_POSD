package com.example.itborrow.repository;

import com.example.itborrow.domain.entity.Equipment;
import com.example.itborrow.domain.enums.EquipmentStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    boolean existsByCategoryId(Long categoryId);

    @Query(
            "select count(e) from Equipment e where lower(trim(e.storageSlot)) = lower(:slot) and"
                    + " (:excludedId is null or e.id <> :excludedId)")
    long countSlotAssignments(String slot, Long excludedId);

    @Query(
            "select e from Equipment e where (:status is null or e.status=:status) and (:keyword=''"
                    + " or lower(e.name) like lower(concat('%',:keyword,'%')) or lower(e.assetCode)"
                    + " like lower(concat('%',:keyword,'%')))")
    Page<Equipment> searchInventory(String keyword, EquipmentStatus status, Pageable pageable);

    long countByStatus(EquipmentStatus status);

    List<Equipment> findByIdInAndStatus(Collection<Long> ids, EquipmentStatus status);

    Page<Equipment> findByNameContainingIgnoreCaseOrAssetCodeContainingIgnoreCase(
            String name, String code, Pageable pageable);

    @Modifying
    @Query("update Equipment e set e.status = :next where e.id = :id and e.status = :expected")
    int transition(
            @Param("id") Long id,
            @Param("expected") EquipmentStatus expected,
            @Param("next") EquipmentStatus next);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Equipment e where e.id = :id")
    Optional<Equipment> findLockedById(@Param("id") Long id);
}
