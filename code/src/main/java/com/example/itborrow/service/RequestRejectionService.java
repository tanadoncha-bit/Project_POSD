package com.example.itborrow.service;
import java.math.BigDecimal;
import java.util.Map;
public interface RequestRejectionService {
    void reject(Long id,String reason);
}
