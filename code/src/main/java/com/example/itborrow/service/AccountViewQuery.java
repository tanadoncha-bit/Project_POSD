package com.example.itborrow.service;

import java.util.*;
import org.springframework.data.domain.*;
import com.example.itborrow.domain.entity.*;

public interface AccountViewQuery {
    Map<String, Object> account(String username, boolean includeProfile);
}
