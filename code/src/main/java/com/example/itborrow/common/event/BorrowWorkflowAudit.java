package com.example.itborrow.common.event;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class BorrowWorkflowAudit {
    private final JdbcTemplate jdbc;

    public BorrowWorkflowAudit(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void record(BorrowWorkflowEvent event) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String actor = authentication != null && authentication.isAuthenticated()
                ? authentication.getName() : "SYSTEM";
        jdbc.update(
                "INSERT INTO borrow_workflow_audit(request_id,actor_username,action,changed_at) VALUES (?,?,?,CURRENT_TIMESTAMP)",
                event.requestId(), actor, event.action());
    }
}
