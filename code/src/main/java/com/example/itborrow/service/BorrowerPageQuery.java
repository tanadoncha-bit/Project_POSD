package com.example.itborrow.service;

import java.util.Map;
import java.util.List;
import com.example.itborrow.domain.entity.Equipment;

public interface BorrowerPageQuery {
    Map<String, Object> history(int page, String status, String keyword);

    Map<String, Object> profile(int activePage, int historyPage);

    List<Equipment> availableEquipment(Long equipmentId, List<Long> equipmentIds);
}
