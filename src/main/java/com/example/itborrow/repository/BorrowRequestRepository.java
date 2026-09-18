package com.example.itborrow.repository;

import com.example.itborrow.domain.entity.BorrowRequest;
import com.example.itborrow.domain.enums.BorrowStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;


public interface BorrowRequestRepository extends JpaRepository<BorrowRequest, Long> {
    Page<BorrowRequest> findByUserId(Long userId, Pageable pageable);
    Page<BorrowRequest> findByStatus(BorrowStatus status, Pageable pageable);
    List<BorrowRequest> findByStatusAndDueDateBefore(BorrowStatus status, LocalDate date);
}