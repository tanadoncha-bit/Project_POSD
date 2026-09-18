package com.example.itborrow.domain.state;

import com.example.itborrow.domain.enums.BorrowStatus;
import org.springframework.stereotype.Component;

@Component
public class BorrowStateResolver {

    private final PendingState pendingState;
    private final ApprovedState approvedState;
    private final BorrowedState borrowedState;
    private final ReturnedState returnedState;
    private final OverdueState overdueState;

    public BorrowStateResolver(PendingState pendingState, ApprovedState approvedState,
            BorrowedState borrowedState, ReturnedState returnedState,
            OverdueState overdueState) {
        this.pendingState = pendingState;
        this.approvedState = approvedState;
        this.borrowedState = borrowedState;
        this.returnedState = returnedState;
        this.overdueState = overdueState;
    }

    public BorrowState resolve(BorrowStatus status) {
        switch (status) {
            case PENDING:
                return pendingState;

            case APPROVED:
                return approvedState;

            case BORROWED:
                return borrowedState;

            case RETURNED:
                return returnedState;

            case OVERDUE:
                return overdueState;

            default:
                return null;
        }
    }
}