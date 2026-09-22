package com.example.registrationlogin.authentication.controller;

import com.example.registrationlogin.authentication.service.AuthenticationService;
import com.example.registrationlogin.dto.ApiResponse;
import com.example.registrationlogin.dto.LoginRequest;
import com.example.registrationlogin.entity.User;
import com.example.registrationlogin.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final JwtService jwtService;

    @Value("${jwt.cookie-name:authToken}")
    private String cookieName;

    @Autowired
    public AuthenticationController(AuthenticationService authenticationService,
                                    JwtService jwtService) {
        this.authenticationService = authenticationService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest loginRequest,
                                             HttpServletResponse response) {
        AuthenticationService.LoginResult result = authenticationService.authenticate(loginRequest);

        if (!result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, result.getMessage()));
        }

        // Create HttpOnly cookie with 1 hour expiry (3600 seconds)
        ResponseCookie authCookie = ResponseCookie.from(cookieName, result.getToken())
                .httpOnly(true)
                .secure(false) // Set to false for localhost HTTP
                .path("/")
                .maxAge(3600)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, authCookie.toString());

        Map<String, Object> userData = new HashMap<>();
        userData.put("username", result.getUser().getUsername());
        userData.put("email", result.getUser().getEmail());
        userData.put("phoneNumber", result.getUser().getPhoneNumber());

        return ResponseEntity.ok(new ApiResponse(true, "Login successful", userData));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(HttpServletRequest request, HttpServletResponse response) {
        String token = jwtService.extractTokenFromCookie(request, cookieName);

        if (token != null) {
            authenticationService.logout(token);
        }

        // Clear the cookie by setting maxAge to 0
        ResponseCookie clearCookie = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());
        SecurityContextHolder.clearContext();

        return ResponseEntity.ok(new ApiResponse(true, "Logged out successfully"));
    }

    @GetMapping("/home")
    public ResponseEntity<?> getHome() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof User)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, "Unauthorized access. Please log in."));
        }

        User user = (User) authentication.getPrincipal();

        Map<String, Object> data = new HashMap<>();
        data.put("message", "Welcome to the Home Page!");
        data.put("username", user.getUsername());
        data.put("email", user.getEmail());
        data.put("phoneNumber", user.getPhoneNumber());

        return ResponseEntity.ok(new ApiResponse(true, "Welcome to the Home Page!", data));
    }
}
