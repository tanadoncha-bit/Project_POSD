package com.example.itborrow.service.impl;

import org.springframework.stereotype.Service;

import com.example.itborrow.domain.entity.User;
import com.example.itborrow.repository.UserRepository;
import com.example.itborrow.service.UserService;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    @Override
    public User createUser(User user) {
        // สามารถเพิ่ม Logic ตรวจสอบอีเมลซ้ำตรงนี้ได้ก่อนบันทึก
        return userRepository.save(user);
    }

    @Override
    public User updateUser(Long id, User userDetails) {
        User user = getUserById(id);
        // user.setName(userDetails.getName());
        // user.setEmail(userDetails.getEmail());
        // อัปเดตฟิลด์อื่นๆ ของ User ตามที่มีใน Entity
        return userRepository.save(user);
    }

    @Override
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}