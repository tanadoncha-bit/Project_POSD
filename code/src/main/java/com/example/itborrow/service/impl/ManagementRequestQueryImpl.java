package com.example.itborrow.service.impl;

import com.example.itborrow.service.*;

import com.example.itborrow.repository.BorrowRequestRepository;
import com.example.itborrow.mapper.BorrowRequestMapper;
import com.example.itborrow.domain.enums.BorrowStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;

@Service
public class ManagementRequestQueryImpl implements ManagementRequestQuery {
    private final com.example.itborrow.config.PaginationProperties pagination;

    private final BorrowRequestRepository requests;
    private final BorrowRequestMapper mapper;
    private final CurrentUser current;

    public ManagementRequestQueryImpl(BorrowRequestRepository requests, BorrowRequestMapper mapper, CurrentUser current, com.example.itborrow.config.PaginationProperties pagination) {
        this.pagination=pagination;
        this.requests = requests;
        this.mapper = mapper;
        this.current = current;
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> load(int page, String status) {
        current.requireOperator();
        var paging = PageRequest.of(Math.max(0, page), pagination.getManagementSize(), Sort.by("id").descending());
        var result = ("ALL".equals(status) ? requests.findAll(paging)
                : requests.findByStatus(BorrowStatus.valueOf(status), paging)).map(mapper::toResponseDto);
        var queue = new java.util.ArrayList<com.example.itborrow.dto.response.BorrowResponseDto>();
        for (var state : java.util.List.of(BorrowStatus.OVERDUE, BorrowStatus.PENDING, BorrowStatus.APPROVED)) {
            if (queue.size() >= 6)
                break;
            queue.addAll(requests.findByStatus(state, PageRequest.of(0, 6 - queue.size(), Sort.by("id").descending()))
                    .map(mapper::toResponseDto).getContent());
        }
        return java.util.Map.of("page", com.example.itborrow.dto.response.PageResponse.from(result), "queue", queue, "pending", requests.countByStatus(BorrowStatus.PENDING),
                "overdue", requests.countByStatus(BorrowStatus.OVERDUE));
    }
}
