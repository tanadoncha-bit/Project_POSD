package com.example.itborrow.domain.state;

import com.example.itborrow.domain.entity.BorrowRequest;

public interface BorrowState {
    java.util.Set<com.example.itborrow.domain.enums.BorrowStatus> supports();
    void approve(BorrowRequest request);

    void pickUp(BorrowRequest request);

    void returnEquipment(BorrowRequest request);

    void cancel(BorrowRequest request);

    void markOverdue(BorrowRequest request);
}