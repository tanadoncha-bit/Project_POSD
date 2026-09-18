package com.example.itborrow.repository;

import com.example.itborrow.domain.entity.ReturnRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface ReturnRecordRepository extends JpaRepository<ReturnRecord, Long> {
    Optional<ReturnRecord> findByBorrowRequestId(Long borrowRequestId);
}