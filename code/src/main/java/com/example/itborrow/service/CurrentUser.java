package com.example.itborrow.service;

import com.example.itborrow.domain.entity.*;
import com.example.itborrow.domain.enums.Role;
import com.example.itborrow.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** Application contract; persistence and orchestration reside in its implementation. */
public interface CurrentUser {
    User require();
    boolean isOperator(User user);
    void requireOperator();
    void requireIndependentOperator(BorrowRequest request);
    void requireOwnerOrOperator(BorrowRequest request);
}
