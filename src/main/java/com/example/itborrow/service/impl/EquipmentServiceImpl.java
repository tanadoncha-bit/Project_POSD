package com.example.itborrow.service.impl;
import com.example.itborrow.domain.entity.Equipment;
import com.example.itborrow.domain.enums.EquipmentStatus;
import com.example.itborrow.exception.*;
import com.example.itborrow.repository.*;
import com.example.itborrow.service.EquipmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
@Service
public class EquipmentServiceImpl implements EquipmentService {
    private final EquipmentRepository equipmentRepository;
    private final EquipmentCategoryRepository categories;
    private final BorrowItemRepository borrowItems;
    private final org.springframework.context.ApplicationEventPublisher events;
    public EquipmentServiceImpl(EquipmentRepository equipmentRepository, EquipmentCategoryRepository categories, org.springframework.context.ApplicationEventPublisher events, BorrowItemRepository borrowItems) {
        this.equipmentRepository=equipmentRepository; this.categories=categories; this.events=events; this.borrowItems=borrowItems;
    }
    public Page<Equipment> searchInventory(String keyword, EquipmentStatus status, Pageable pageable) {
        return equipmentRepository.searchInventory(keyword.trim(),status,PageRequest.of(pageable.getPageNumber(),Math.min(100,pageable.getPageSize()),pageable.getSort()));
    }
    public java.util.Map<String,Long> inventorySummary() {
        return java.util.Map.of("available",equipmentRepository.countByStatus(EquipmentStatus.AVAILABLE),"inUse",equipmentRepository.countByStatus(EquipmentStatus.IN_USE),"total",equipmentRepository.count());
    }
    public Page<Equipment> getAllEquipments(Pageable pageable) { return equipmentRepository.findAll(pageable); }
    public Page<Equipment> searchEquipments(String keyword, Pageable pageable) { return keyword == null || keyword.isBlank() ? equipmentRepository.findAll(pageable) : equipmentRepository.findByNameContainingIgnoreCaseOrAssetCodeContainingIgnoreCase(keyword.trim(), keyword.trim(), pageable); }
    public Equipment getEquipmentById(Long id) { return equipmentRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Equipment",id)); }
    private void validateDetails(Equipment equipment) {
        if (equipment.getStorageSlot() != null) {
            String slot = equipment.getStorageSlot().trim().toUpperCase(java.util.Locale.ROOT);
            if (slot.length() > 100) throw new IllegalArgumentException("Storage slot must be at most 100 characters.");
            equipment.setStorageSlot(slot.isEmpty() ? null : slot);
        }
        String url = equipment.getImageUrl();
        if (url != null && !url.isBlank() && (url.length() > 1000 || !(url.startsWith("https://") || url.startsWith("/images/"))))
            throw new IllegalArgumentException("Use an HTTPS image URL or a local /images/ path.");
        if (equipment.getSpecifications() != null && equipment.getSpecifications().length() > 10000)
            throw new IllegalArgumentException("Specifications must be at most 10000 characters.");
    }
    private void validateName(String name) {
        if (name == null || name.isBlank() || name.length()>150) throw new IllegalArgumentException("Invalid equipment name.");
    }
    private void validatePrice(java.math.BigDecimal price) {
        if (price == null || price.signum() < 0 || price.compareTo(new java.math.BigDecimal("9999999999.99")) > 0 || price.stripTrailingZeros().scale() > 2)
            throw new IllegalArgumentException("Enter a non-negative purchase price with at most two decimal places.");
    }
    private void validateSlot(String slot, Long excludedId) {
        if (slot != null && equipmentRepository.countSlotAssignments(slot, excludedId) > 0)
            throw new InvalidBorrowStateException("This storage slot is already assigned to another asset. Choose a different slot or clear the old assignment first.");
    }
    @Transactional public Equipment createEquipment(Equipment equipment) {
        validateName(equipment.getName());
        validateDetails(equipment);
        if (equipment.getId()!=null || equipment.getAssetCode()==null || equipment.getAssetCode().isBlank() || equipment.getAssetCode().length()>30)
            throw new IllegalArgumentException("Invalid asset code/id.");
        if (equipment.getCategoryId()==null || !categories.existsById(equipment.getCategoryId())) throw new IllegalArgumentException("Select a category.");
        if (equipment.getStatus()!=null && equipment.getStatus()!=EquipmentStatus.AVAILABLE) throw new IllegalArgumentException("New equipment must be available.");
        validateSlot(equipment.getStorageSlot(), null);
        validatePrice(equipment.getPurchasePrice());
        if (equipment.getImageUrl() != null && equipment.getImageUrl().startsWith("/images/equipment/")) throw new IllegalArgumentException("Upload equipment photos through the image endpoint.");
        equipment.setStatus(EquipmentStatus.AVAILABLE);
        return equipmentRepository.save(equipment);
    }
    @Transactional public Equipment updateEquipment(Long id, Equipment details) {
        boolean slotProvided = details.getStorageSlot() != null;
        validateName(details.getName());
        validateDetails(details);
        var equipment=equipmentRepository.findLockedById(id).orElseThrow(() -> ResourceNotFoundException.of("Equipment",id));
        if (details.getCategoryId() != null) {
            if (!categories.existsById(details.getCategoryId())) throw new IllegalArgumentException("Select a valid category.");
            equipment.setCategoryId(details.getCategoryId());
        }
        validateSlot(details.getStorageSlot(), id);
        if (slotProvided) equipment.setStorageSlot(details.getStorageSlot());
        if (details.getImageUrl() != null && (equipment.getImageUrl() == null || !equipment.getImageUrl().startsWith("/images/equipment/")) && !details.getImageUrl().startsWith("/images/equipment/")) equipment.setImageUrl(details.getImageUrl());
        if (details.getSpecifications() != null) equipment.setSpecifications(details.getSpecifications());
        if (details.getStatus()==null) throw new IllegalArgumentException("Select a status.");
        if ((equipment.getStatus()==EquipmentStatus.IN_USE || details.getStatus()==EquipmentStatus.IN_USE) && equipment.getStatus()!=details.getStatus())
            throw new InvalidBorrowStateException("Use pickup/return to change an in-use asset.");
        if (equipment.getStatus()==EquipmentStatus.MAINTENANCE && details.getStatus()==EquipmentStatus.AVAILABLE) throw new InvalidBorrowStateException("Use Complete repair and record the repair details first.");
        if (details.getPurchasePrice() != null) {
            validatePrice(details.getPurchasePrice()); equipment.setPurchasePrice(details.getPurchasePrice());
        }
        equipment.setName(details.getName()); equipment.setStatus(details.getStatus());
        return equipmentRepository.save(equipment);
    }
    @Transactional(readOnly=true) public boolean canDeleteEquipment(Long id) {
        var equipment=getEquipmentById(id);
        return equipment.getStatus()!=EquipmentStatus.IN_USE && !borrowItems.existsByEquipmentId(id);
    }
    @Transactional public void deleteEquipment(Long id) {
        var equipment=equipmentRepository.findLockedById(id).orElseThrow(() -> ResourceNotFoundException.of("Equipment",id));
        if (equipment.getStatus()==EquipmentStatus.IN_USE) throw new InvalidBorrowStateException("Cannot delete an asset in use.");
        if (borrowItems.existsByEquipmentId(id)) {
            throw new InvalidBorrowStateException("Cannot delete equipment linked to borrowing requests, including cancelled or returned requests. Its history must be kept. To retire this asset, use Edit equipment and set its status to Disposed.");
        }
        equipmentRepository.delete(equipment);
        events.publishEvent(new com.example.itborrow.common.event.EquipmentDeleted(equipment.getImageUrl()));
    }
}
