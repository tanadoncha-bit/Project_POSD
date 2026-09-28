package com.example.itborrow.config;
import com.example.itborrow.service.BorrowRequestService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;
@Configuration
@EnableScheduling
public class OverdueScheduler {
    private final BorrowRequestService service;
    public OverdueScheduler(BorrowRequestService service) { this.service=service; }
    @Scheduled(fixedDelayString="${borrow.overdue.interval-ms:60000}", initialDelayString="${borrow.overdue.initial-delay-ms:60000}")
    public void markOverdue() { service.checkAndMarkOverdue(); }
}
