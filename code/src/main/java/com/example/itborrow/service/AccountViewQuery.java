package com.example.itborrow.service;

import java.util.Map;

public interface AccountViewQuery {
    Map<String, Object> account(String username, boolean includeProfile);
}
