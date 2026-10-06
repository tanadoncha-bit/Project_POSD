package com.example.itborrow.common.event;

import com.example.itborrow.domain.entity.BorrowRequest;
import org.springframework.context.ApplicationEvent;

public class OverdueEvent extends ApplicationEvent {

    private final BorrowRequest borrowRequest;

    public OverdueEvent(Object source, BorrowRequest borrowRequest) {
        super(source);
        this.borrowRequest = borrowRequest;
    }

    public BorrowRequest getBorrowRequest() {
        return borrowRequest;
    }
}