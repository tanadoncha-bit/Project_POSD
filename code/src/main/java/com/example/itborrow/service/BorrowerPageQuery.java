package com.example.itborrow.service;

import java.util.*;
import org.springframework.data.domain.*;
import com.example.itborrow.domain.entity.*;

public interface BorrowerPageQuery {
    Map<String, Object> history(int page, String status, String keyword);
    Map<String, Object> profile(int activePage, int historyPage);
    List<com.example.itborrow.domain.entity.Equipment> availableEquipment(Long equipmentId,
            List<Long> equipmentIds);
}
