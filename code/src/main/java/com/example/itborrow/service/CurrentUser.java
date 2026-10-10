package com.example.itborrow.service;

import com.example.itborrow.domain.entity.*;

public interface CurrentUser {
    User require();

    boolean isOperator(User user);

    void requireOperator();

    void requireIndependentOperator(BorrowRequest request);

    void requireOwnerOrOperator(BorrowRequest request);
}
