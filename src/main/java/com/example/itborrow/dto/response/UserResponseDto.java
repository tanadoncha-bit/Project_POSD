package com.example.itborrow.dto.response;

public class UserResponseDto {
    private Long id;
    private String username;
    private String email;
    // ไม่ใส่รหัสผ่านในนี้ เพื่อความปลอดภัย

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}