package com.gemstore.backend.controllers.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gemstore.backend.dtos.auth.LoginRequest;
import com.gemstore.backend.dtos.auth.RegisterUserRequest;
import com.gemstore.backend.entities.user.User;
import com.gemstore.backend.repositories.user.EmailVerificationOtpRepository;
import com.gemstore.backend.repositories.user.UserRepository;
import com.gemstore.backend.services.auth.EmailVerificationOtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EmailVerificationOtpRepository otpRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    // ✅ Mock the OTP service so it doesn't try to send real emails during tests
    @MockBean
    private EmailVerificationOtpService otpService;

    @BeforeEach
    void setUp() {
        // Tell the mock to do nothing when an email is supposed to be sent
        doNothing().when(otpService).generateAndSendOtp(any());

        otpRepository.deleteAll(); // Prevent foreign key constraint violation
        userRepository.deleteAll();
    }

    // ==================== REGISTER ====================

    @Test
    void register_withValidData_returnsCreatedWithToken() throws Exception {
        RegisterUserRequest request = new RegisterUserRequest();
        request.setDisplayName("Test User");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setUsername("testuser");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.newUser").value(true))
                .andExpect(jsonPath("$.user.username").value("testuser"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.user.displayName").value("Test User"));
    }

    @Test
    void register_withDuplicateEmail_returns4xx() throws Exception {
        // Seed an existing user
        User existing = User.builder()
                .displayName("Existing")
                .email("test@example.com")
                .username("existing")
                .passwordHash(passwordEncoder.encode("password123"))
                .provider("LOCAL")
                .role("USER")
                .status("ACTIVE")
                .build();
        userRepository.save(existing);

        RegisterUserRequest request = new RegisterUserRequest();
        request.setDisplayName("New User");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setUsername("newuser");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void register_withDuplicateUsername_returns4xx() throws Exception {
        User existing = User.builder()
                .displayName("Existing")
                .email("existing@example.com")
                .username("testuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .provider("LOCAL")
                .role("USER")
                .status("ACTIVE")
                .build();
        userRepository.save(existing);

        RegisterUserRequest request = new RegisterUserRequest();
        request.setDisplayName("New User");
        request.setEmail("new@example.com");
        request.setPassword("password123");
        request.setUsername("testuser");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void register_withBlankEmail_returnsBadRequest() throws Exception {
        RegisterUserRequest request = new RegisterUserRequest();
        request.setDisplayName("Test User");
        request.setEmail("");
        request.setPassword("password123");
        request.setUsername("testuser");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_withShortPassword_returnsBadRequest() throws Exception {
        RegisterUserRequest request = new RegisterUserRequest();
        request.setDisplayName("Test User");
        request.setEmail("test@example.com");
        request.setPassword("short");
        request.setUsername("testuser");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_withBlankUsername_returnsBadRequest() throws Exception {
        RegisterUserRequest request = new RegisterUserRequest();
        request.setDisplayName("Test User");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setUsername("");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== LOGIN ====================

    @Test
    void login_withValidCredentials_returnsOkWithToken() throws Exception {
        // Seed user
        User user = User.builder()
                .displayName("Test User")
                .email("test@example.com")
                .username("testuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .provider("LOCAL")
                .role("USER")
                .status("ACTIVE")
                .build();
        userRepository.save(user);

        LoginRequest request = new LoginRequest();
        request.setIdentifier("testuser");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.newUser").value(false))
                .andExpect(jsonPath("$.user.username").value("testuser"));
    }

    @Test
    void login_withEmail_returnsOkWithToken() throws Exception {
        User user = User.builder()
                .displayName("Test User")
                .email("test@example.com")
                .username("testuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .provider("LOCAL")
                .role("USER")
                .status("ACTIVE")
                .build();
        userRepository.save(user);

        LoginRequest request = new LoginRequest();
        request.setIdentifier("test@example.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("test@example.com"));
    }

    @Test
    void login_withWrongPassword_returnsUnauthorized() throws Exception {
        User user = User.builder()
                .displayName("Test User")
                .email("test@example.com")
                .username("testuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .provider("LOCAL")
                .role("USER")
                .status("ACTIVE")
                .build();
        userRepository.save(user);

        LoginRequest request = new LoginRequest();
        request.setIdentifier("testuser");
        request.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withNonexistentUser_returnsUnauthorized() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("nobody");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withBlankFields_returnsBadRequest() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("");
        request.setPassword("");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== ME ====================

    @Test
    void me_withValidToken_returnsCurrentUser() throws Exception {
        // Register a user to get a valid token
        RegisterUserRequest registerReq = new RegisterUserRequest();
        registerReq.setDisplayName("Test User");
        registerReq.setEmail("test@example.com");
        registerReq.setPassword("password123");
        registerReq.setUsername("testuser");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void me_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withInvalidToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized());
    }
}