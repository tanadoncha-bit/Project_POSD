package com.example.itborrow.config;

import com.example.itborrow.service.BorrowRequestService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;

@ConditionalOnProperty(name = "app.overdue.enabled", havingValue = "true", matchIfMissing = true)
@Configuration
public class OverdueScheduler {
    private final BorrowRequestService service;

    public OverdueScheduler(BorrowRequestService service) {
        this.service = service;
    }

    @Scheduled(
            fixedDelayString = "${borrow.overdue.interval-ms:60000}",
            initialDelayString = "${borrow.overdue.initial-delay-ms:60000}")
    public void markOverdue() {
        service.checkAndMarkOverdue();
    }
}
