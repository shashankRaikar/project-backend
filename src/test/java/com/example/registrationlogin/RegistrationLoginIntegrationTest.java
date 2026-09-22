package com.example.registrationlogin;

import com.example.registrationlogin.authentication.repository.JwtTokenRepository;
import com.example.registrationlogin.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class RegistrationLoginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenRepository jwtTokenRepository;

    @BeforeEach
    void setUp() {
        jwtTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Complete Authentication Flow: Register -> Duplicate Check -> Bad Login -> Valid Login -> Protected Home -> Logout")
    void testCompleteAuthenticationFlow() throws Exception {
        // 1. User Registration: POST /api/reg
        String regJson = """
                {
                    "username": "john_doe",
                    "password": "SecurePassword123!",
                    "email": "john.doe@example.com",
                    "phoneNumber": "9876543210"
                }
                """;

        mockMvc.perform(post("/api/reg")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(regJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("successfully")));

        // Verify user in repository
        assertTrue(userRepository.existsByUsername("john_doe"));
        assertNotEquals("SecurePassword123!", userRepository.findByUsername("john_doe").get().getPassword(),
                "Password must be hashed with BCrypt and not stored in plain text");

        // 2. Duplicate Registration Attempt
        mockMvc.perform(post("/api/reg")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(regJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("already exists")));

        // 3. Login with wrong password
        String wrongLoginJson = """
                {
                    "username": "john_doe",
                    "password": "WrongPassword"
                }
                """;

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wrongLoginJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));

        // 4. Login with correct credentials
        String correctLoginJson = """
                {
                    "username": "john_doe",
                    "password": "SecurePassword123!"
                }
                """;

        MvcResult loginResult = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(correctLoginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(cookie().exists("authToken"))
                .andExpect(cookie().httpOnly("authToken", true))
                .andExpect(cookie().maxAge("authToken", 3600))
                .andReturn();

        // Extract cookie
        jakarta.servlet.http.Cookie authCookie = loginResult.getResponse().getCookie("authToken");
        assertNotNull(authCookie, "authToken cookie must be present");
        String tokenValue = authCookie.getValue();

        // Verify token saved in DB
        assertTrue(jwtTokenRepository.existsByToken(tokenValue), "JWT must be saved in jwt_token table");

        // 5. Unauthenticated access to /api/home without cookie
        mockMvc.perform(get("/api/home"))
                .andExpect(status().isUnauthorized());

        // 6. Authenticated access to /api/home with cookie
        mockMvc.perform(get("/api/home")
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Welcome to the Home Page!")))
                .andExpect(jsonPath("$.data.username", is("john_doe")))
                .andExpect(jsonPath("$.data.email", is("john.doe@example.com")))
                .andExpect(jsonPath("$.data.phoneNumber", is("9876543210")));

        // 7. Logout: POST /api/logout
        mockMvc.perform(post("/api/logout")
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(cookie().maxAge("authToken", 0));

        // Verify token deleted from database
        assertFalse(jwtTokenRepository.existsByToken(tokenValue), "Token must be invalidated in DB after logout");

        // 8. Access to /api/home after logout with old cookie
        mockMvc.perform(get("/api/home")
                        .cookie(authCookie))
                .andExpect(status().isUnauthorized());
    }
}
