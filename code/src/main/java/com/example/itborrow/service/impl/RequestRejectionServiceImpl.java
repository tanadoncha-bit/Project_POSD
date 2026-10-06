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
public class RequestRejectionServiceImpl implements RequestRejectionService {
    private final BorrowRequestRepository requests;
    private final CurrentUser current;
    private final ApplicationEventPublisher events;
    public RequestRejectionServiceImpl(BorrowRequestRepository requests, CurrentUser current, ApplicationEventPublisher events) {
        this.requests=requests;
        this.current=current;
        this.events=events;
    }
    private String required(String value) {
        if (value == null || value.isBlank() || value.length() > 500)
            throw new IllegalArgumentException("Provide a reason/reference of at most 500 characters.");
        return value.trim();
    }

    @Transactional
    public void reject(Long id, String reason) {
        var request = requests.findLockedById(id).orElseThrow(() -> ResourceNotFoundException.of("Request", id));
        current.requireIndependentOperator(request);
        if (request.getStatus() != BorrowStatus.PENDING)
            throw new InvalidBorrowStateException("Only pending requests can be rejected.");
        request.setRejectionReason(required(reason));
        request.setStatus(BorrowStatus.CANCELLED);
        events.publishEvent(new BorrowWorkflowEvent(id, "REJECTED"));
    }


}
