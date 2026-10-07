package com.example.itborrow.service;

import com.example.itborrow.domain.entity.*;
import com.example.itborrow.domain.enums.Role;
import com.example.itborrow.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import java.util.UUID;

/** Application contract; persistence and orchestration reside in its implementation. */
public interface GoogleAccountService {
    boolean linked(String username);
    User signIn(String subject,String email,boolean verified,String fullName,String linkingUsername);
}
