package com.example.itborrow.service.impl;

import com.example.itborrow.service.*;
import com.example.itborrow.repository.*;
import com.example.itborrow.mapper.BorrowRequestMapper;
import com.example.itborrow.domain.enums.EquipmentStatus;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class BorrowerPageQueryImpl implements BorrowerPageQuery {
    private final com.example.itborrow.config.PaginationProperties pagination;

    private final BorrowRequestRepository requests;
    private final EquipmentRepository assets;
    private final BorrowRequestMapper mapper;
    private final CurrentUser current;

    public BorrowerPageQueryImpl(BorrowRequestRepository requests, EquipmentRepository assets,
            BorrowRequestMapper mapper, CurrentUser current, com.example.itborrow.config.PaginationProperties pagination) {
        this.pagination=pagination;
        this.requests = requests;
        this.assets = assets;
        this.mapper = mapper;
        this.current = current;
    }

    private java.util.List<com.example.itborrow.domain.enums.BorrowStatus> states(String status) {
        var all = com.example.itborrow.domain.enums.BorrowStatus.values();
        if ("ALL".equals(status))
            return java.util.List.of(all);
        if ("ACTIVE".equals(status))
            return java.util.List.of(com.example.itborrow.domain.enums.BorrowStatus.APPROVED,
                    com.example.itborrow.domain.enums.BorrowStatus.BORROWED,
                    com.example.itborrow.domain.enums.BorrowStatus.OVERDUE);
        try {
            return java.util.List.of(com.example.itborrow.domain.enums.BorrowStatus.valueOf(status));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown request status.");
        }
    }

    public Map<String, Object> history(int page, String status, String keyword) {
        Map<String, Object> result = new HashMap<>();
        var owner = current.require().getId();
        var results = requests.searchUserRequests(owner, states(status), keyword.trim(),
                PageRequest.of(Math.max(0, page), pagination.getRequestSize(), Sort.by("id").descending())).map(mapper::toResponseDto);
        result.put("requests", results.getContent());
        result.put("requestPage", results);
        result.put("requestStatus", status);
        result.put("keyword", keyword);
        var counts = new java.util.HashMap<String, Long>();
        var statusCounts = new java.util.EnumMap<com.example.itborrow.domain.enums.BorrowStatus, Long>(
                com.example.itborrow.domain.enums.BorrowStatus.class);
        requests.summarizeUserStatuses(owner).forEach(row -> statusCounts.put(row.getStatus(), row.getTotal()));
        for (var filter : java.util.List.of("ALL", "ACTIVE", "PENDING", "RETURNED", "CANCELLED"))
            counts.put(filter, states(filter).stream().mapToLong(state -> statusCounts.getOrDefault(state, 0L)).sum());
        result.put("requestCounts", counts);
        result.put("overdueCount",
                statusCounts.getOrDefault(com.example.itborrow.domain.enums.BorrowStatus.OVERDUE, 0L));
        return result;
    }

    public Map<String, Object> profile(int activePage, int historyPage) {
        Map<String, Object> result = new HashMap<>();
        var owner = current.require().getId();
        var active = requests
                .searchUserRequests(owner,
                        java.util.List.of(com.example.itborrow.domain.enums.BorrowStatus.BORROWED,
                                com.example.itborrow.domain.enums.BorrowStatus.OVERDUE),
                        "", PageRequest.of(Math.max(0, activePage), pagination.getProfileSize(), Sort.by("id").descending()))
                .map(mapper::toResponseDto);
        var history = requests
                .searchUserRequests(owner,
                        java.util.List.of(com.example.itborrow.domain.enums.BorrowStatus.RETURNED,
                                com.example.itborrow.domain.enums.BorrowStatus.CANCELLED),
                        "", PageRequest.of(Math.max(0, historyPage), pagination.getProfileSize(), Sort.by("id").descending()))
                .map(mapper::toResponseDto);
        result.put("activeLoans", active.getContent());
        result.put("borrowHistory", history.getContent());
        result.put("activeLoanPage", active);
        result.put("borrowHistoryPage", history);
        return result;
    }

    public List<com.example.itborrow.domain.entity.Equipment> availableEquipment(Long equipmentId,
            List<Long> equipmentIds) {
        if (equipmentIds != null && equipmentIds.size() > 100)
            throw new IllegalArgumentException("Select at most 100 equipment items.");
        var selected = equipmentIds != null ? equipmentIds : equipmentId != null ? List.of(equipmentId) : null;
        return selected != null ? assets.findByIdInAndStatus(selected, EquipmentStatus.AVAILABLE)
                : assets.searchInventory("", EquipmentStatus.AVAILABLE, PageRequest.of(0, pagination.getCatalogSize(), Sort.by("name", "id")))
                        .getContent();
    }
}
