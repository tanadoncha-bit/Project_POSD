package com.example.itborrow.service;

import com.example.itborrow.repository.DeliveryJobRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Application contract; persistence and orchestration reside in its implementation. */
public interface DeliveryOperations {
    Map<String, Object> summary();
    List<Map<String, Object>> failed();
    void retry(long id);
}
