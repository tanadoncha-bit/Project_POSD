package com.example.itborrow.service.impl;

import com.example.itborrow.service.*;

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

@Service
public class SimulatedLockerServiceImpl implements SimulatedLockerService {
    private final java.time.Clock clock;
    private final com.example.itborrow.repository.LockerAccessRepository access;
    private final BorrowRequestRepository requests;
    private final CurrentUser current;
    private final java.security.SecureRandom random = new java.security.SecureRandom();

    public SimulatedLockerServiceImpl(com.example.itborrow.repository.LockerAccessRepository access, BorrowRequestRepository requests, CurrentUser current, java.time.Clock clock) {
        this.access = access;
        this.clock = clock;
        this.requests = requests;
        this.current = current;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void workflow(BorrowWorkflowEvent event) {
        if (event.action().equals("APPROVED")) {
            var request = requests.findById(event.requestId()).orElseThrow();
            issue(request);
        } else if (java.util.Set.of("PICKED_UP", "CANCELLED", "EXPIRED", "REJECTED", "RETURNED")
                .contains(event.action())) {
            access.revoke(event.requestId());
        }
    }

    private void issue(com.example.itborrow.domain.entity.BorrowRequest request) {
        String slots = request.getItems().stream().map(item -> {
            String slot = item.getEquipment().getStorageSlot();
            return item.getSnapshotName() + ": " + (slot == null ? "Ask staff for storage location" : slot);
        }).collect(java.util.stream.Collectors.joining("; "));
        access.issue(
                request.getId(), String.format(java.util.Locale.ROOT, "%06d", random.nextInt(1000000)), slots);
    }

    private com.example.itborrow.domain.entity.BorrowRequest owned(Long id) {
        var request = requests.findLockedById(id).orElseThrow(() -> new IllegalArgumentException("Request not found."));
        if (!request.getUser().getId().equals(current.require().getId()))
            throw new AccessDeniedException("Only the borrower can access this PIN.");
        if (request.getStatus() != BorrowStatus.APPROVED || LocalDate.now(clock).isAfter(request.getDueDate()))
            throw new IllegalArgumentException(
                    "Locker access is only available for an approved request before pickup.");
        return request;
    }

    @Transactional
    public Map<String, Object> access(Long id) {
        var request = owned(id);
        var rows = access.find(id);
        if (rows.isEmpty()) {
            issue(request);
            rows = access.find(id);
        }
        var row = rows.get(0);
        return Map.of("pin", row.get("pin") == null ? "" : row.get("pin"), "slots", row.get("slots"), "opened",
                row.get("opened"),
                "availableFrom", request.getBorrowDate().toString(), "expiresOn", request.getDueDate().toString(),
                "simulation", true);
    }

    @Transactional
    public void open(Long id, String pin) {
        var request = owned(id);
        if (LocalDate.now(clock).isBefore(request.getBorrowDate()))
            throw new IllegalArgumentException("Pickup period has not started.");
        if (pin == null || !pin.matches("[0-9]{6}"))
            throw new IllegalArgumentException("Enter the 6-digit PIN.");
        if (!access.open(id, pin))
            throw new IllegalArgumentException("Incorrect PIN or this simulated locker has already been opened.");
    }
}
