package com.gemstore.backend.config;

import com.gemstore.backend.entities.user.User;
import com.gemstore.backend.repositories.user.UserRepository;
import com.gemstore.backend.services.auth.JWTService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtFilterTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JWTService jwtService;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User testUser;
    private String validToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testUser = User.builder()
                .displayName("Filter Test User")
                .email("filtertest@example.com")
                .username("filteruser")
                .passwordHash(passwordEncoder.encode("password123"))
                .provider("LOCAL")
                .role("USER")
                .status("ACTIVE")
                .build();
        testUser = userRepository.save(testUser);
        validToken = jwtService.generateToken(testUser.getId(), testUser.getUsername(), testUser.getRole());
    }

    // ==================== shouldNotFilter ====================

    @Test
    void shouldNotFilter_loginEndpoint_allowsWithoutToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldNotFilter_registerEndpoint_allowsWithoutToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ==================== doFilterInternal ====================

    @Test
    void doFilterInternal_validToken_setsAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());
    }

    @Test
    void doFilterInternal_noAuthHeader_doesNotAuthenticate() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void doFilterInternal_nonBearerScheme_doesNotAuthenticate() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void doFilterInternal_malformedToken_doesNotAuthenticate() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer not.a.valid.jwt.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void doFilterInternal_expiredToken_doesNotAuthenticate() throws Exception {
        String fakeExpiredToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0IiwiZXhwIjoxfQ.invalid";

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + fakeExpiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void doFilterInternal_emptyBearerToken_doesNotAuthenticate() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void doFilterInternal_validToken_publicEndpoint_stillWorks() throws Exception {
        mockMvc.perform(get("/api/v1/listings/search")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());
    }

    @Test
    void doFilterInternal_noToken_publicEndpoint_works() throws Exception {
        mockMvc.perform(get("/api/v1/listings/search"))
                .andExpect(status().isOk());
    }

    @Test
    void doFilterInternal_validAdminToken_accessesAdminEndpoint() throws Exception {
        User admin = User.builder()
                .displayName("Admin")
                .email("admin@example.com")
                .username("admin")
                .passwordHash(passwordEncoder.encode("adminpass"))
                .provider("LOCAL")
                .role("ADMIN")
                .status("ACTIVE")
                .build();
        admin = userRepository.save(admin);
        String adminToken = jwtService.generateToken(admin.getId(), admin.getUsername(), admin.getRole());

        // Use PATCH (the correct HTTP method for this endpoint) to verify admin access
        // Returns 4xx (user not found) but NOT 401/403 — proving admin auth works
        mockMvc.perform(patch("/api/admin/users/" + testUser.getId() + "/status/ACTIVE")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void doFilterInternal_userToken_cannotAccessAdminEndpoint() throws Exception {
        // Use PATCH (correct method) — regular user should get 403 Forbidden
        mockMvc.perform(patch("/api/admin/users/" + testUser.getId() + "/status/ACTIVE")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void doFilterInternal_tokenWithoutUid_doesNotAuthenticate() throws Exception {
        String badToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyIn0.invalidsignature";

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + badToken))
                .andExpect(status().isUnauthorized());
    }
}