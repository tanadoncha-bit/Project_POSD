<<<<<<< HEAD
// import jakarta.persistence.*;
=======
package com.example.itborrow.domain.entity;

import jakarta.persistence.*;
>>>>>>> alicha_673380431_7_Sec3

// @Entity
// @Table(name = "users")
// public class User {

//     @Id
//     @GeneratedValue(strategy = GenerationType.IDENTITY)
//     private Long id;

//     @Column(nullable = false)
//     private String username;

//     @Column(nullable = false, unique = true)
//     private String email;

//     @Enumerated(EnumType.STRING)
//     @Column(nullable = false)
//     private Role role;

//     public User() {
//     }

//     // Constructor
//     public User(Long id, String username, String email, Role role) {
//         this.id = id;
//         this.username = username;
//         this.email = email;
//         this.role = role;
//     }

//     // Constructor
//     public User(String username, String email, Role role) {
//         this.username = username;
//         this.email = email;
//         this.role = role;
//     }

//     // Getters and Setters
//     public Long getId() {
//         return id;
//     }

//     public void setId(Long id) {
//         this.id = id;
//     }

//     public String getUsername() {
//         return username;
//     }

//     public void setUsername(String username) {
//         this.username = username;
//     }

//     public String getEmail() {
//         return email;
//     }

//     public void setEmail(String email) {
//         this.email = email;
//     }

//     public Role getRole() {
//         return role;
//     }

//     public void setRole(Role role) {
//         this.role = role;
//     }
// }