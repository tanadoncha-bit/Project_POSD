package com.example.itborrow.config;

import com.example.itborrow.service.PersistentJobs;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;

@Component
@ConditionalOnProperty(name = "app.jobs.enabled", havingValue = "true", matchIfMissing = true)
public class DeliveryScheduler {
    private final PersistentJobs jobs;

    public DeliveryScheduler(PersistentJobs jobs) {
        this.jobs = jobs;
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void run() {
        jobs.process();
    }
}
