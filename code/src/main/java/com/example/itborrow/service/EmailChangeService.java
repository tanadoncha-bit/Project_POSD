package com.example.itborrow.service;

import com.example.itborrow.dto.request.EmailChangeDto;
import com.example.itborrow.repository.*;
import java.time.Clock;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application contract; persistence and orchestration reside in its implementation. */
public interface EmailChangeService {
    void request(EmailChangeDto dto);
    boolean valid(String token);
    void confirm(String token);
}
