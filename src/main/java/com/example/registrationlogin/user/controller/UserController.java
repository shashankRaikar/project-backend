package com.example.registrationlogin.user.controller;

import com.example.registrationlogin.dto.ApiResponse;
import com.example.registrationlogin.dto.RegistrationRequest;
import com.example.registrationlogin.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/reg")
    
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegistrationRequest registrationRequest) {
        ApiResponse response = userService.registerUser(registrationRequest);
        if (response.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
