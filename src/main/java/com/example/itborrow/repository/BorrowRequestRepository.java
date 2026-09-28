package com.example.itborrow.repository;

import com.example.itborrow.domain.entity.BorrowRequest;
import com.example.itborrow.domain.enums.BorrowStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;


public interface BorrowRequestRepository extends JpaRepository<BorrowRequest, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select b from BorrowRequest b where b.id = :id")
    java.util.Optional<BorrowRequest> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select b from BorrowRequest b where b.status = :status and b.dueDate < :date order by b.id")
    List<BorrowRequest> findOverdueForUpdate(@org.springframework.data.repository.query.Param("status") BorrowStatus status, @org.springframework.data.repository.query.Param("date") LocalDate date);
    @org.springframework.data.jpa.repository.Query("select count(distinct b.id) from BorrowRequest b join b.items i where i.equipment.id = :equipmentId and b.id <> :requestId and b.status in :states and b.borrowDate <= :end and b.dueDate >= :start")
    long countConflicts(Long equipmentId, Long requestId, LocalDate start, LocalDate end, java.util.List<BorrowStatus> states);
    Page<BorrowRequest> findByUserId(Long userId, Pageable pageable);
    long countByStatus(BorrowStatus status);
    Page<BorrowRequest> findByStatus(BorrowStatus status, Pageable pageable);
    List<BorrowRequest> findByStatusAndDueDateBefore(BorrowStatus status, LocalDate date);
}