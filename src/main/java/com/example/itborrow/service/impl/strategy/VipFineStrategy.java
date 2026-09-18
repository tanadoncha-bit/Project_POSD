package com.example.itborrow.service.impl.strategy;

import com.example.itborrow.domain.entity.BorrowRequest;
import com.example.itborrow.domain.enums.Role;
import com.example.itborrow.service.FineStrategyService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class VipFineStrategy implements FineStrategyService {

    private static final int GRACE_PERIOD_DAYS = 2;
    private static final BigDecimal FINE_PER_DAY = BigDecimal.valueOf(30);

    @Override
    public BigDecimal calculate(BorrowRequest request, LocalDate actualReturnDate) {
        long overdueDays = ChronoUnit.DAYS.between(request.getDueDate(), actualReturnDate);
        long chargeableDays = overdueDays - GRACE_PERIOD_DAYS;
        if (chargeableDays <= 0) {
            return BigDecimal.ZERO;
        }
        return FINE_PER_DAY.multiply(BigDecimal.valueOf(chargeableDays));
    }

    @Override
    public boolean supports(Role role) {
        return role == Role.VIP;
    }
}