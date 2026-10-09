package com.example.itborrow.service.impl;

import com.example.itborrow.domain.enums.Role;
import com.example.itborrow.repository.DeliveryJobRepository;
import com.example.itborrow.service.*;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DeliveryOperationsImpl implements DeliveryOperations {
    private final DeliveryJobRepository jobs;
    private final CurrentUser current;

    public DeliveryOperationsImpl(DeliveryJobRepository jobs, CurrentUser current) {
        this.jobs = jobs;
        this.current = current;
    }

    private void requireAdmin() {
        if (current.require().getRole() != Role.ADMIN)
            throw new AccessDeniedException("Administrator access required");
    }

    public Map<String, Object> summary() {
        requireAdmin();
        return jobs.summary();
    }

    public List<Map<String, Object>> failed() {
        requireAdmin();
        return jobs.failed();
    }

    public void retry(long id) {
        requireAdmin();
        if (!jobs.retryFailed(id))
            throw new IllegalArgumentException("Job is not available for manual retry.");
    }
}
