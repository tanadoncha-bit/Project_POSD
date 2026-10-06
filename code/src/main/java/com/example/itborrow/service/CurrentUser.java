package com.example.itborrow.service;

import com.example.itborrow.domain.entity.*;
import com.example.itborrow.domain.enums.Role;
import com.example.itborrow.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUser {
    private final UserRepository users;

    public CurrentUser(UserRepository users) {
        this.users = users;
    }

    public User require() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)
            throw new AccessDeniedException("Please sign in.");
        return users.findByUsername(auth.getName()).orElseThrow(() -> new AccessDeniedException("Account not found."));
    }

    public boolean isOperator(User user) {
        return user.getRole() == Role.ADMIN || user.getRole() == Role.STAFF;
    }

    public void requireOperator() {
        if (!isOperator(require()))
            throw new AccessDeniedException("Operator access required.");
    }

    public void requireIndependentOperator(BorrowRequest request) {
        var actor = require();
        if (!isOperator(actor) || actor.getId().equals(request.getUser().getId()))
            throw new AccessDeniedException("Another staff member must handle this request.");
    }

    public void requireOwnerOrOperator(BorrowRequest request) {
        var user = require();
        if (!isOperator(user) && !user.getId().equals(request.getUser().getId()))
            throw new AccessDeniedException("This request belongs to another user.");
    }
}
