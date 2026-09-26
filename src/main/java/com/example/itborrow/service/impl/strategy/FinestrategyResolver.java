package com.example.itborrow.service.impl.strategy;

import com.example.itborrow.domain.enums.Role;
import com.example.itborrow.service.FineStrategyService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FinestrategyResolver {

    private final List<FineStrategyService> strategies;

    public FinestrategyResolver(List<FineStrategyService> strategies) {
        this.strategies = strategies;
    }

    public FineStrategyService resolve(Role role) {
        return strategies.stream()
                .filter(s -> s.supports(role))
                .findFirst()
                // ถ้าไม่มี strategy ตรงกับ role ไหนเลย ใช้ Standard เป็นค่า default กันระบบพัง
                .orElseGet(() -> strategies.stream()
                        .filter(s -> s.supports(Role.STAFF))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("ไม่พบ FineStrategyService เริ่มต้น")));
    }
}