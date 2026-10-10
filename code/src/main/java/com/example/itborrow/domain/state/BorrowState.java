package com.example.itborrow.domain.state;

import com.example.itborrow.domain.entity.BorrowRequest;
import com.example.itborrow.domain.enums.BorrowStatus;

import java.util.Set;

public interface BorrowState {
    Set<BorrowStatus> supports();

    void approve(BorrowRequest request);

    void pickUp(BorrowRequest request);

    void returnEquipment(BorrowRequest request);

    void cancel(BorrowRequest request);

    void markOverdue(BorrowRequest request);
}
