package com.example.itborrow.service.jobs;
/** Execution port for durable background jobs. */
public interface DeliveryJobExecutor {
    boolean emailAvailable();
    void process();
}
