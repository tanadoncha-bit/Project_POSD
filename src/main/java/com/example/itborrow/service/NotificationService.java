package com.example.itborrow.service;

import com.example.itborrow.domain.entity.User;

public interface NotificationService {
    void send(User user, String message);
}