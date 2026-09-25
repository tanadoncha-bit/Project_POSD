package com.example.itborrow.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "department")
    private String  ;

    // Default Constructor
    public UserProfile() {
    }

    // Constructor 
    public UserProfile(Long id, String fullName, String phone, String department) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.department = department;
    }

    // Constructor 
    public UserProfile(String fullName, String phone, String department) {
        this.fullName = fullName;
        this.phone = phone;
        this.department = department;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}