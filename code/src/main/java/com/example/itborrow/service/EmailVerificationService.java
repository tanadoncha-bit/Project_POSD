package com.example.itborrow.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

/** Application contract; persistence and orchestration reside in its implementation. */
public interface EmailVerificationService {
    Map<String, Boolean> status();
    void request();
    void confirm(String token);
    boolean validLink(String token);
    void confirmLink(String token);
}
