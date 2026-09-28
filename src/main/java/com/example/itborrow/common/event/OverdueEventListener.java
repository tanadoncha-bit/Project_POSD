package com.example.itborrow.common.event;

import com.example.itborrow.service.NotificationService;
import org.springframework.stereotype.Component;

@Component
public class OverdueEventListener {

    private final NotificationService notificationService;

    public OverdueEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @org.springframework.transaction.event.TransactionalEventListener(phase = org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT)
    public void handleOverdueEvent(OverdueEvent event) {
        var request = event.getBorrowRequest();

        String message = String.format(
                "คำขอยืม #%d เกินกำหนดคืนแล้ว (กำหนดคืน: %s) กรุณานำอุปกรณ์มาคืนโดยเร็ว",
                request.getId(),
                request.getDueDate()
        );

        notificationService.send(request.getUser(), message);
    }
}
