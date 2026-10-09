package com.example.itborrow.service;

import java.util.List;
import java.util.Map;

public interface DeliveryOperations {
    Map<String, Object> summary();

    List<Map<String, Object>> failed();

    void retry(long id);
}
