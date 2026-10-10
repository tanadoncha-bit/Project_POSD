package com.example.itborrow.service;

import java.util.Map;

public interface SimulatedLockerService {
    Map<String, Object> access(Long id);

    void open(Long id, String pin);
}
