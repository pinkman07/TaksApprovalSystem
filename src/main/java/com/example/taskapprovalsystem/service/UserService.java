package com.example.taskapprovalsystem.service;

import com.example.taskapprovalsystem.dto.UserDTO;
import com.example.taskapprovalsystem.entity.User;
import com.example.taskapprovalsystem.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(UserDTO userDTO) {
        log.info("Creating user with email: {}", userDTO.getEmail());
        try {
            User user = new User();
            user.setName(userDTO.getName());
            user.setEmail(userDTO.getEmail());
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));

            log.debug("User object created: {}", user);
            User savedUser = userRepository.save(user);
            log.info("User successfully saved with ID: {}", savedUser.getId());
            return savedUser;
        } catch (Exception e) {
            log.error("Error while creating user: {}", userDTO.getEmail(), e);
            throw e;
        }
    }

    public List<User> getAllUsers() {
        log.info("Fetching all users from the database");
        try {
            List<User> users = userRepository.findAll();
            log.info("Total users fetched: {}", users.size());
            return users;
        } catch (Exception e) {
            log.error("Error while fetching users", e);
            throw e;
        }
    }
}
