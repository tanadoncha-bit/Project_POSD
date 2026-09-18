package com.example.itborrow.repository;

import com.example.itborrow.domain.entity.BorrowItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BorrowItemRepository extends JpaRepository<BorrowItem, Long> {
    List<BorrowItem> findByEquipmentId(Long equipmentId);
    List<BorrowItem> findByBorrowRequestId(Long borrowRequestId);
}
