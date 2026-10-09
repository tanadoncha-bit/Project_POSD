package com.example.itborrow.repository;

import com.example.itborrow.domain.entity.BorrowRequest;
import com.example.itborrow.domain.enums.BorrowStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BorrowRequestRepository extends JpaRepository<BorrowRequest, Long> {
    @Query(
            "select count(b.id) from BorrowRequest b join b.items i where i.equipment.id ="
                + " :equipmentId and b.status = :status and b.id <> :requestId")
    long countReservations(Long equipmentId, Long requestId, BorrowStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BorrowRequest b where b.id = :id")
    Optional<BorrowRequest> findLockedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "select b from BorrowRequest b where b.status = :status and b.dueDate < :date order by"
                    + " b.id")
    List<BorrowRequest> findOverdueForUpdate(
            @Param("status") BorrowStatus status, @Param("date") LocalDate date);

    @Query(
            "select count(distinct b.id) from BorrowRequest b join b.items i where i.equipment.id ="
                + " :equipmentId and i.returnedOn is null and b.id <> :requestId and b.status in"
                + " :states and b.borrowDate <= :end and b.dueDate >= :start")
    long countConflicts(
            Long equipmentId,
            Long requestId,
            LocalDate start,
            LocalDate end,
            List<BorrowStatus> states);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "select b from BorrowRequest b where b.status in :states and b.dueDate < :today order"
                    + " by b.id")
    List<BorrowRequest> findExpiredForUpdate(List<BorrowStatus> states, LocalDate today);

    Page<BorrowRequest> findByUserId(Long userId, Pageable pageable);

    @Query(
            "select b from BorrowRequest b where b.user.id = :userId and b.status in :states and"
                + " (:keyword = '' or exists (select i.id from BorrowItem i where i.borrowRequest ="
                + " b and (lower(coalesce(i.snapshotName, i.equipment.name)) like lower(concat('%',"
                + " :keyword, '%')) or lower(coalesce(i.snapshotAssetCode, i.equipment.assetCode))"
                + " like lower(concat('%', :keyword, '%')))))")
    @EntityGraph(attributePaths = {"user", "returnRecord"})
    Page<BorrowRequest> searchUserRequests(
            Long userId, List<BorrowStatus> states, String keyword, Pageable pageable);

    interface StatusCount {
        BorrowStatus getStatus();

        long getTotal();
    }

    @Query(
            "select b.status as status, count(b) as total from BorrowRequest b where"
                    + " b.user.id=:userId group by b.status")
    List<StatusCount> summarizeUserStatuses(Long userId);

    long countByUserIdAndStatusIn(Long userId, List<BorrowStatus> states);

    long countByStatus(BorrowStatus status);

    Page<BorrowRequest> findByStatus(BorrowStatus status, Pageable pageable);

    List<BorrowRequest> findByStatusAndDueDateBefore(BorrowStatus status, LocalDate date);
}
