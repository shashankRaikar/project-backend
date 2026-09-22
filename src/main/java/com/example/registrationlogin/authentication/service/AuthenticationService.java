package com.example.registrationlogin.authentication.service;

import com.example.registrationlogin.authentication.repository.JwtTokenRepository;
import com.example.registrationlogin.dto.LoginRequest;
import com.example.registrationlogin.entity.JwtToken;
import com.example.registrationlogin.entity.User;
import com.example.registrationlogin.security.JwtService;
import com.example.registrationlogin.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final JwtTokenRepository jwtTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Autowired
    public AuthenticationService(UserRepository userRepository,
                                 JwtTokenRepository jwtTokenRepository,
                                 PasswordEncoder passwordEncoder,
                                 JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtTokenRepository = jwtTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public static class LoginResult {
        private final boolean success;
        private final String message;
        private final String token;
        private final User user;

        public LoginResult(boolean success, String message, String token, User user) {
            this.success = success;
            this.message = message;
            this.token = token;
            this.user = user;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getToken() {
            return token;
        }

        public User getUser() {
            return user;
        }
    }

    @Transactional
    public LoginResult authenticate(LoginRequest request) {
        Optional<User> userOptional = userRepository.findByUsername(request.getUsername().trim());

        if (userOptional.isEmpty()) {
            return new LoginResult(false, "Invalid username or password", null, null);
        }

        User user = userOptional.get();

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return new LoginResult(false, "Invalid username or password", null, null);
        }

        // Generate JWT
        String token = jwtService.generateToken(user.getUsername());
        LocalDateTime creationTime = LocalDateTime.now();
        LocalDateTime expiryTime = creationTime.plusHours(1);

        // Remove old tokens for this user to keep table clean
        jwtTokenRepository.deleteByUserId(user.getId());

        // Save new JWT in database
        JwtToken jwtToken = new JwtToken(user.getId(), token, creationTime, expiryTime);
        jwtTokenRepository.save(jwtToken);

        return new LoginResult(true, "Login successful", token, user);
    }

    @Transactional
    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            jwtTokenRepository.deleteByToken(token);
        }
    }

    public boolean validateTokenInDatabase(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        Optional<JwtToken> tokenOpt = jwtTokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            return false;
        }

        JwtToken jwtToken = tokenOpt.get();
        // Check if database recorded expiry time has passed
        return jwtToken.getExpiryTime().isAfter(LocalDateTime.now());
    }
}
