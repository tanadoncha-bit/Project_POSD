package com.example.itborrow.service;

import com.example.itborrow.domain.entity.*;
import com.example.itborrow.domain.entity.Equipment;

import org.springframework.data.domain.*;

import java.util.*;

public interface BorrowerPageQuery {
    Map<String, Object> history(int page, String status, String keyword);

    Map<String, Object> profile(int activePage, int historyPage);

    List<Equipment> availableEquipment(Long equipmentId, List<Long> equipmentIds);
}
