package com.example.itborrow.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.itborrow.domain.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    org.springframework.data.domain.Page<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String username, String email, org.springframework.data.domain.Pageable pageable);

    java.util.Optional<User> findByUsername(String username);

    java.util.Optional<User> findByEmailIgnoreCase(String email);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from User u where u.id=:id")
    java.util.Optional<User> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);

    long countByRole(com.example.itborrow.domain.enums.Role role);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update User u set u.password = :encoded where u.id = :id and u.password = :old")
    int upgradePassword(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("old") String old,
            @org.springframework.data.repository.query.Param("encoded") String encoded);

}