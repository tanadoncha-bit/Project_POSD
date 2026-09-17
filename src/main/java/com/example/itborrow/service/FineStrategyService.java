package com.example.itborrow.service;

import com.example.itborrow.domain.entity.BorrowRequest;

import java.math.BigDecimal;

public interface FineStrategyService {
    BigDecimal calculate(BorrowRequest request, java.time.LocalDate actualReturnDate);
    boolean supports(com.example.itborrow.domain.enums.Role role);
}