package com.example.itborrow.service.strategy;

import com.example.itborrow.domain.enums.Role;
import com.example.itborrow.service.FineStrategyService;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FineStrategyResolver {

    private final List<FineStrategyService> strategies;

    public FineStrategyResolver(List<FineStrategyService> strategies) {
        this.strategies = List.copyOf(strategies);
        for (Role role : Role.values()) {
            if (this.strategies.stream().filter(strategy -> strategy.supports(role)).count() > 1)
                throw new IllegalStateException("Duplicate fine strategy for role: " + role);
        }
        if (this.strategies.stream().noneMatch(strategy -> strategy.supports(Role.USER)))
            throw new IllegalStateException("A default USER fine strategy is required.");
    }

    public FineStrategyService resolve(Role role) {
        if (role == null) throw new IllegalArgumentException("User role is required.");
        return strategies.stream()
                .filter(s -> s.supports(role))
                .findFirst()
                .orElseGet(
                        () ->
                                strategies.stream()
                                        .filter(s -> s.supports(Role.USER))
                                        .findFirst()
                                        .orElseThrow(
                                                () ->
                                                        new IllegalStateException(
                                                                "ไม่พบ FineStrategyService"
                                                                        + " เริ่มต้น")));
    }
}
