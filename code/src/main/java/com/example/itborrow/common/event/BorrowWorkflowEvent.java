package com.example.itborrow.common.event;
/** Persisted by a before-commit listener so failed workflows leave no audit entry. */
public record BorrowWorkflowEvent(Long requestId, String action) {}
