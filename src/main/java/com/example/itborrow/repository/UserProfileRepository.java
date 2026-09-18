package com.example.itborrow.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.itborrow.domain.entity.UserProfile;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
}
