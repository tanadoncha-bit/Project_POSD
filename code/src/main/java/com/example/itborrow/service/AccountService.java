package com.example.itborrow.service;

import com.example.itborrow.domain.entity.*;
import com.example.itborrow.domain.enums.Role;
import com.example.itborrow.dto.request.*;
import com.example.itborrow.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application contract; persistence and orchestration reside in its implementation. */
public interface AccountService {
    User register(RegistrationDto dto);
    void update(ProfileUpdateDto dto);
    User changePassword(PasswordChangeDto dto);
    boolean requiresLoginSetup();
    User setupLogin(LoginSetupDto dto);
}
