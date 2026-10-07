package com.example.itborrow.service;

import com.example.itborrow.repository.BorrowRequestRepository;
import com.example.itborrow.mapper.BorrowRequestMapper;
import com.example.itborrow.domain.enums.BorrowStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;

/** Application contract; persistence and orchestration reside in its implementation. */
public interface ManagementRequestQuery {
    java.util.Map<String, Object> load(int page, String status);
}
