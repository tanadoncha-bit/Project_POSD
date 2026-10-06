package com.example.itborrow.service.impl.strategy;

import com.example.itborrow.domain.entity.BorrowRequest;
import com.example.itborrow.domain.enums.Role;
import com.example.itborrow.service.FineStrategyService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class StandardFineStrategy implements FineStrategyService {

    private final com.example.itborrow.config.FeePolicyProperties fees;
    public StandardFineStrategy(com.example.itborrow.config.FeePolicyProperties fees) {this.fees=fees;}

    @Override
    public BigDecimal calculate(BorrowRequest request, LocalDate actualReturnDate) {
        long overdueDays = ChronoUnit.DAYS.between(request.getDueDate(), actualReturnDate) - fees.getStandardGraceDays();
        if (overdueDays <= 0) {
            return BigDecimal.ZERO;
        }
        return fees.getStandardDailyFine().multiply(BigDecimal.valueOf(overdueDays));
    }

    @Override
    public boolean supports(Role role) {
        return role == Role.USER;
    }
}