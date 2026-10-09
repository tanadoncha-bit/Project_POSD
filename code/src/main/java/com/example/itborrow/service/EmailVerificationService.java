package com.example.itborrow.service;

import java.time.*;
import java.util.*;

public interface EmailVerificationService {
    Map<String, Boolean> status();

    void request();

    void confirm(String token);

    boolean validLink(String token);

    void confirmLink(String token);
}
