package com.example.itborrow.service.impl;

import com.example.itborrow.config.PaginationProperties;
import com.example.itborrow.domain.entity.Equipment;
import com.example.itborrow.domain.enums.BorrowStatus;
import com.example.itborrow.domain.enums.EquipmentStatus;
import com.example.itborrow.mapper.BorrowRequestMapper;
import com.example.itborrow.repository.*;
import com.example.itborrow.service.*;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class BorrowerPageQueryImpl implements BorrowerPageQuery {
    private final PaginationProperties pagination;

    private final BorrowRequestRepository requests;
    private final EquipmentRepository assets;
    private final BorrowRequestMapper mapper;
    private final CurrentUser current;

    public BorrowerPageQueryImpl(
            BorrowRequestRepository requests,
            EquipmentRepository assets,
            BorrowRequestMapper mapper,
            CurrentUser current,
            PaginationProperties pagination) {
        this.pagination = pagination;
        this.requests = requests;
        this.assets = assets;
        this.mapper = mapper;
        this.current = current;
    }

    private List<BorrowStatus> states(String status) {
        var all = BorrowStatus.values();
        if ("ALL".equals(status)) return List.of(all);
        if ("ACTIVE".equals(status))
            return List.of(BorrowStatus.APPROVED, BorrowStatus.BORROWED, BorrowStatus.OVERDUE);
        try {
            return List.of(BorrowStatus.valueOf(status));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown request status.");
        }
    }

    public Map<String, Object> history(int page, String status, String keyword) {
        Map<String, Object> result = new HashMap<>();
        var owner = current.require().getId();
        var results =
                requests.searchUserRequests(
                                owner,
                                states(status),
                                keyword.trim(),
                                PageRequest.of(
                                        Math.max(0, page),
                                        pagination.getRequestSize(),
                                        Sort.by("id").descending()))
                        .map(mapper::toResponseDto);
        result.put("requests", results.getContent());
        result.put("requestPage", results);
        result.put("requestStatus", status);
        result.put("keyword", keyword);
        var counts = new HashMap<String, Long>();
        var statusCounts = new EnumMap<BorrowStatus, Long>(BorrowStatus.class);
        requests.summarizeUserStatuses(owner)
                .forEach(row -> statusCounts.put(row.getStatus(), row.getTotal()));
        for (var filter : List.of("ALL", "ACTIVE", "PENDING", "RETURNED", "CANCELLED"))
            counts.put(
                    filter,
                    states(filter).stream()
                            .mapToLong(state -> statusCounts.getOrDefault(state, 0L))
                            .sum());
        result.put("requestCounts", counts);
        result.put("overdueCount", statusCounts.getOrDefault(BorrowStatus.OVERDUE, 0L));
        return result;
    }

    public Map<String, Object> profile(int activePage, int historyPage) {
        Map<String, Object> result = new HashMap<>();
        var owner = current.require().getId();
        var active =
                requests.searchUserRequests(
                                owner,
                                List.of(BorrowStatus.BORROWED, BorrowStatus.OVERDUE),
                                "",
                                PageRequest.of(
                                        Math.max(0, activePage),
                                        pagination.getProfileSize(),
                                        Sort.by("id").descending()))
                        .map(mapper::toResponseDto);
        var history =
                requests.searchUserRequests(
                                owner,
                                List.of(BorrowStatus.RETURNED, BorrowStatus.CANCELLED),
                                "",
                                PageRequest.of(
                                        Math.max(0, historyPage),
                                        pagination.getProfileSize(),
                                        Sort.by("id").descending()))
                        .map(mapper::toResponseDto);
        result.put("activeLoans", active.getContent());
        result.put("borrowHistory", history.getContent());
        result.put("activeLoanPage", active);
        result.put("borrowHistoryPage", history);
        return result;
    }

    public List<Equipment> availableEquipment(Long equipmentId, List<Long> equipmentIds) {
        if (equipmentIds != null && equipmentIds.size() > 100)
            throw new IllegalArgumentException("Select at most 100 equipment items.");
        var selected =
                equipmentIds != null
                        ? equipmentIds
                        : equipmentId != null ? List.of(equipmentId) : null;
        return selected != null
                ? assets.findByIdInAndStatus(selected, EquipmentStatus.AVAILABLE)
                : assets.searchInventory(
                                "",
                                EquipmentStatus.AVAILABLE,
                                PageRequest.of(
                                        0, pagination.getCatalogSize(), Sort.by("name", "id")))
                        .getContent();
    }
}
