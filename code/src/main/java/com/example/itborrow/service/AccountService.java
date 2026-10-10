package com.example.itborrow.service;

import com.example.itborrow.domain.entity.*;
import com.example.itborrow.dto.request.*;
import com.example.itborrow.repository.*;

public interface AccountService {
    User register(RegistrationDto dto);

    void update(ProfileUpdateDto dto);

    User changePassword(PasswordChangeDto dto);

    boolean requiresLoginSetup();

    User setupLogin(LoginSetupDto dto);
}
