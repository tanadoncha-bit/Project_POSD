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
    List<BorrowRequest> findOverdueForUpdate(
            @org.springframework.data.repository.query.Param("status") BorrowStatus status,
            @org.springframework.data.repository.query.Param("date") LocalDate date);

    @org.springframework.data.jpa.repository.Query("select count(distinct b.id) from BorrowRequest b join b.items i where i.equipment.id = :equipmentId and i.returnedOn is null and b.id <> :requestId and b.status in :states and b.borrowDate <= :end and b.dueDate >= :start")
    long countConflicts(Long equipmentId, Long requestId, LocalDate start, LocalDate end,
            java.util.List<BorrowStatus> states);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select b from BorrowRequest b where b.status in :states and b.dueDate < :today order by b.id")
    List<BorrowRequest> findExpiredForUpdate(java.util.List<BorrowStatus> states, LocalDate today);

    Page<BorrowRequest> findByUserId(Long userId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("select b from BorrowRequest b where b.user.id = :userId and b.status in :states and (:keyword = '' or exists (select i.id from BorrowItem i where i.borrowRequest = b and (lower(coalesce(i.snapshotName, i.equipment.name)) like lower(concat('%', :keyword, '%')) or lower(coalesce(i.snapshotAssetCode, i.equipment.assetCode)) like lower(concat('%', :keyword, '%')))))")
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "user", "returnRecord" })
    Page<BorrowRequest> searchUserRequests(Long userId, java.util.List<BorrowStatus> states, String keyword,
            Pageable pageable);

    interface StatusCount {
        BorrowStatus getStatus();

        long getTotal();
    }

    @org.springframework.data.jpa.repository.Query("select b.status as status, count(b) as total from BorrowRequest b where b.user.id=:userId group by b.status")
    List<StatusCount> summarizeUserStatuses(Long userId);

    long countByUserIdAndStatusIn(Long userId, java.util.List<BorrowStatus> states);

    long countByStatus(BorrowStatus status);

    Page<BorrowRequest> findByStatus(BorrowStatus status, Pageable pageable);

    List<BorrowRequest> findByStatusAndDueDateBefore(BorrowStatus status, LocalDate date);
}