package com.example.itborrow.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.itborrow.domain.entity.EquipmentCategory;


public interface EquipmentCategoryRepository extends JpaRepository<EquipmentCategory, Long> {

}
