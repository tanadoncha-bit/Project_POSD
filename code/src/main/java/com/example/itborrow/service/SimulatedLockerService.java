package com.example.itborrow.service;

import com.example.itborrow.common.event.BorrowWorkflowEvent;
import com.example.itborrow.domain.enums.BorrowStatus;
import com.example.itborrow.repository.BorrowRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.security.access.AccessDeniedException;
import java.time.LocalDate;
import java.util.Map;

/** Application contract; persistence and orchestration reside in its implementation. */
public interface SimulatedLockerService {
    Map<String, Object> access(Long id);
    void open(Long id, String pin);
}
