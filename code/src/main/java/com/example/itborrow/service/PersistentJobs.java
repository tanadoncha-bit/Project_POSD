package com.example.itborrow.service;

import org.springframework.transaction.annotation.*;

public interface PersistentJobs {
    boolean mailConfigured();

    void email(String recipient, String subject, String body);

    void cleanup(String path);

    void process();
}
