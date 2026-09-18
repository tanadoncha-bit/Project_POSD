package com.example.itborrow.repository;

import com.example.itborrow.domain.entity.BorrowItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BorrowItemRepository extends JpaRepository<BorrowItem, Long> {
    List<BorrowItem> findByEquipmentId(Long equipmentId);
    List<BorrowItem> findByBorrowRequestId(Long borrowRequestId);
}
