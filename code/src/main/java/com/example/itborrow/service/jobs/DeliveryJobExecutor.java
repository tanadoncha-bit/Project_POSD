package com.example.itborrow.service.jobs;

public interface DeliveryJobExecutor {
    boolean emailAvailable();

    void process();
}
