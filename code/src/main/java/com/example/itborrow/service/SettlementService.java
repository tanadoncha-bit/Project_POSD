package com.example.itborrow.service;

import java.math.BigDecimal;
import java.util.Map;

public interface SettlementService {
    Map<String, Object> settlement(Long id);

    void settle(Long id, String reference, BigDecimal expectedAmount);
}
