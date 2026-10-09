package com.example.itborrow.service.jobs;

import java.util.Map;

public interface DeliveryJobHandler {
    String kind();

    default boolean available() {
        return true;
    }

    void execute(Map<String, Object> job);
}
