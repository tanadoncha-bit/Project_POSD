package com.example.itborrow.service.impl;

import com.example.itborrow.domain.entity.User;
import com.example.itborrow.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceimpl implements NotificationService {

    private static final Logger log =
            LoggerFactory.getLogger(NotificationServiceimpl.class);

    @Override
    public void send(User user, String message) {
        log.info(
                "[NOTIFICATION] ถึง {} ({}): {}",
                user.getUsername(),
                user.getEmail(),
                message
        );
    }
}