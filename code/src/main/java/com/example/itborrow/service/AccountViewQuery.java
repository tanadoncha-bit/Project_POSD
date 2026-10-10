package com.example.itborrow.service;

import com.example.itborrow.domain.entity.*;

import org.springframework.data.domain.*;

import java.util.*;

public interface AccountViewQuery {
    Map<String, Object> account(String username, boolean includeProfile);
}
