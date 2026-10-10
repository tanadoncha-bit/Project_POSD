package com.example.itborrow.service.impl;

import com.example.itborrow.config.PaginationProperties;
import com.example.itborrow.domain.enums.BorrowStatus;
import com.example.itborrow.dto.response.BorrowResponseDto;
import com.example.itborrow.dto.response.PageResponse;
import com.example.itborrow.mapper.BorrowRequestMapper;
import com.example.itborrow.repository.BorrowRequestRepository;
import com.example.itborrow.service.*;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ManagementRequestQueryImpl implements ManagementRequestQuery {
    private final PaginationProperties pagination;

    private final BorrowRequestRepository requests;
    private final BorrowRequestMapper mapper;
    private final CurrentUser current;

    public ManagementRequestQueryImpl(
            BorrowRequestRepository requests,
            BorrowRequestMapper mapper,
            CurrentUser current,
            PaginationProperties pagination) {
        this.pagination = pagination;
        this.requests = requests;
        this.mapper = mapper;
        this.current = current;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> load(int page, String status) {
        current.requireOperator();
        var paging =
                PageRequest.of(
                        Math.max(0, page),
                        pagination.getManagementSize(),
                        Sort.by("id").descending());
        var result =
                ("ALL".equals(status)
                                ? requests.findAll(paging)
                                : requests.findByStatus(BorrowStatus.valueOf(status), paging))
                        .map(mapper::toResponseDto);
        var queue = new ArrayList<BorrowResponseDto>();
        for (var state :
                List.of(BorrowStatus.OVERDUE, BorrowStatus.PENDING, BorrowStatus.APPROVED)) {
            if (queue.size() >= 6) break;
            queue.addAll(
                    requests.findByStatus(
                                    state,
                                    PageRequest.of(0, 6 - queue.size(), Sort.by("id").descending()))
                            .map(mapper::toResponseDto)
                            .getContent());
        }
        return Map.of(
                "page",
                PageResponse.from(result),
                "queue",
                queue,
                "pending",
                requests.countByStatus(BorrowStatus.PENDING),
                "overdue",
                requests.countByStatus(BorrowStatus.OVERDUE));
    }
}
