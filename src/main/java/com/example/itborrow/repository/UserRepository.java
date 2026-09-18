package com.example.itborrow.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.itborrow.domain.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

}