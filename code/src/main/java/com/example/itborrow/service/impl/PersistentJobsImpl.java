package com.example.itborrow.service.impl;

import com.example.itborrow.repository.DeliveryJobRepository;
import com.example.itborrow.service.*;
import com.example.itborrow.service.jobs.DeliveryJobExecutor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class PersistentJobsImpl implements PersistentJobs {
    private final DeliveryJobRepository jobs;
    private final DeliveryJobExecutor processor;

    public PersistentJobsImpl(DeliveryJobRepository jobs, DeliveryJobExecutor processor) {
        this.jobs = jobs;
        this.processor = processor;
    }

    public boolean mailConfigured() {
        return processor.emailAvailable();
    }

    @Transactional
    public void email(String recipient, String subject, String body) {
        jobs.enqueue("EMAIL", recipient, subject, body);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanup(String path) {
        jobs.enqueue("STORAGE_DELETE", "", "", path);
    }

    public void process() {
        processor.process();
    }
}
