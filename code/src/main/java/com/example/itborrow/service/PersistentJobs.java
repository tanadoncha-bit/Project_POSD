package com.example.itborrow.service;

import com.example.itborrow.repository.DeliveryJobRepository;
import com.example.itborrow.service.jobs.DeliveryJobProcessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Application contract; persistence and orchestration reside in its implementation. */
public interface PersistentJobs {
    boolean mailConfigured();
    void email(String recipient,String subject,String body);
    void cleanup(String path);
    void process();
}
