package com.example.itborrow.service;

import com.example.itborrow.dto.request.EmailChangeDto;
import com.example.itborrow.repository.*;

public interface EmailChangeService {
    void request(EmailChangeDto dto);

    boolean valid(String token);

    void confirm(String token);
}
