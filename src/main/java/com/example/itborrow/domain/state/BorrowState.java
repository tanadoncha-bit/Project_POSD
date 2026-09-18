package com.example.itborrow.domain.state;

import com.example.itborrow.domain.entity.BorrowRequest;

public interface BorrowState {
    void approve(BorrowRequest request);
    void pickUp(BorrowRequest request);
    void returnEquipment(BorrowRequest request);
    void cancel(BorrowRequest request);
    void markOverdue(BorrowRequest request);
}