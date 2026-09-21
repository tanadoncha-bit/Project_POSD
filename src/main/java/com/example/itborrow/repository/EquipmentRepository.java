package com.example.itborrow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.itborrow.domain.entity.Equipment;

@Repository 
public interface EquipmentRepository extends JpaRepository<Equipment,Long> {
    
}