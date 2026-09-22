package com.example.registrationlogin.user.service;

import com.example.registrationlogin.dto.ApiResponse;
import com.example.registrationlogin.dto.RegistrationRequest;
import com.example.registrationlogin.entity.User;
import com.example.registrationlogin.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public ApiResponse registerUser(RegistrationRequest request) {
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername().trim())) {
            return new ApiResponse(false, "Username already exists. Please choose a different username.");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail().trim())) {
            return new ApiResponse(false, "Email is already registered. Please use another email.");
        }

        // Hash password using BCrypt
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // Create and save user
        User user = new User(
                request.getUsername().trim(),
                hashedPassword,
                request.getEmail().trim(),
                request.getPhoneNumber().trim()
        );

        userRepository.save(user);

        return new ApiResponse(true, "User registered successfully! You can now log in.");
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
