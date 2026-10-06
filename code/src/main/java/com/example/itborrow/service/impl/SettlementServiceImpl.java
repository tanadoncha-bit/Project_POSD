package com.example.itborrow.service.impl;
import com.example.itborrow.repository.*;
import com.example.itborrow.domain.enums.*;
import com.example.itborrow.exception.*;
import com.example.itborrow.common.event.BorrowWorkflowEvent;
import com.example.itborrow.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import java.math.BigDecimal;
import java.util.Map;
@Service
public class SettlementServiceImpl implements SettlementService {
    private final BorrowRequestRepository requests;
    private final ReturnRecordRepository returns;
    private final CurrentUser current;
    private final SettlementRepository payments;
    private final ApplicationEventPublisher events;
    public SettlementServiceImpl(BorrowRequestRepository requests, ReturnRecordRepository returns, CurrentUser current, SettlementRepository payments, ApplicationEventPublisher events) {
        this.requests=requests;
        this.returns=returns;
        this.current=current;
        this.payments=payments;
        this.events=events;
    }
    private String required(String value) {
        if (value == null || value.isBlank() || value.length() > 500)
            throw new IllegalArgumentException("Provide a reason/reference of at most 500 characters.");
        return value.trim();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> settlement(Long id) {
        var request = requests.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Request", id));
        current.requireOwnerOrOperator(request);
        var record = returns.findByBorrowRequestId(id).orElseThrow(() -> ResourceNotFoundException.of("Return", id));
        var recordedPayments = payments.findByRequest(id);
        return Map.of("borrowRequestId", id, "username", request.getUser().getUsername(), "fineAmount",
                record.getFineAmount(), "damageAmount", record.getDamageAmount(), "total",
                record.getFineAmount().add(record.getDamageAmount()), "finalized",
                request.getStatus() == BorrowStatus.RETURNED, "paid", !recordedPayments.isEmpty(), "payments", recordedPayments);
    }

    @Transactional
    public void settle(Long id, String reference, BigDecimal expectedAmount) {
        var request = requests.findLockedById(id).orElseThrow(() -> ResourceNotFoundException.of("Request", id));
        current.requireIndependentOperator(request);
        if (request.getStatus() != BorrowStatus.RETURNED)
            throw new InvalidBorrowStateException("Complete all returns before settlement.");
        var record = returns.findByBorrowRequestId(id).orElseThrow(() -> ResourceNotFoundException.of("Return", id));
        var total = record.getFineAmount().add(record.getDamageAmount());
        if (total.signum() <= 0 || expectedAmount == null || total.compareTo(expectedAmount) != 0)
            throw new IllegalArgumentException("Verify the outstanding amount before recording payment.");
        if (payments.existsByRequest(id))
            throw new InvalidBorrowStateException("Payment is already recorded.");
        payments.record(id,total,required(reference),current.require().getUsername());
        events.publishEvent(new BorrowWorkflowEvent(id, "PAYMENT_RECORDED"));
    }


}
