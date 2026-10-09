package com.example.itborrow.service;

import org.springframework.data.domain.*;

import java.util.Map;

public interface ManagementRequestQuery {
    Map<String, Object> load(int page, String status);
}
