package com.example.itborrow.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.itborrow.domain.entity.Equipment;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    
}