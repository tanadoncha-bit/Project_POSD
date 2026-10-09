package com.example.itborrow.service;

import com.example.itborrow.domain.entity.*;
import com.example.itborrow.repository.*;

import org.springframework.security.oauth2.core.*;

public interface GoogleAccountService {
    boolean linked(String username);

    User signIn(
            String subject,
            String email,
            boolean verified,
            String fullName,
            String linkingUsername);
}
