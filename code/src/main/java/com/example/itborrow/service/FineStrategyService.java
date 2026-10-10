package com.example.itborrow.service;

import com.example.itborrow.domain.entity.BorrowRequest;
import com.example.itborrow.domain.enums.Role;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface FineStrategyService {
    BigDecimal calculate(BorrowRequest request, LocalDate actualReturnDate);

    boolean supports(Role role);
}
