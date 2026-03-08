package com.gemstore.backend.controllers.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gemstore.backend.dtos.user.UpdateProfileRequest;
import com.gemstore.backend.entities.user.User;
import com.gemstore.backend.repositories.user.UserRepository;
import com.gemstore.backend.services.auth.JWTService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JWTService jwtService;

    private User testUser;
    private String testToken;
    private User otherUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testUser = User.builder()
                .displayName("Test User")
                .email("test@example.com")
                .username("testuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .provider("LOCAL")
                .role("USER")
                .status("ACTIVE")
                .bio("Hello world")
                .website("https://example.com")
                .build();
        testUser = userRepository.save(testUser);
        testToken = jwtService.generateToken(testUser.getId(), testUser.getUsername(), testUser.getRole());

        otherUser = User.builder()
                .displayName("Other User")
                .email("other@example.com")
                .username("otheruser")
                .passwordHash(passwordEncoder.encode("password123"))
                .provider("LOCAL")
                .role("USER")
                .status("ACTIVE")
                .build();
        otherUser = userRepository.save(otherUser);
    }

    // ==================== LIST ALL ====================

    @Test
    void listAll_authenticated_returnsAllUsers() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void listAll_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== GET BY ID ====================

    @Test
    void getById_existingUser_returnsUser() throws Exception {
        mockMvc.perform(get("/api/users/{id}", testUser.getId())
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId()))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void getById_nonExistentUser_returns4xx() throws Exception {
        mockMvc.perform(get("/api/users/{id}", 99999L)
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(status().is4xxClientError());
    }

    // ==================== GET CURRENT USER (/me) ====================

    @Test
    void getCurrentUser_authenticated_returnsCurrentUser() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId()))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void getCurrentUser_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== PATCH PROFILE ====================

    @Test
    void patchProfile_validData_returnsUpdatedUser() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDisplayName("Updated Name");
        request.setBio("Updated bio");

        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + testToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Updated Name"))
                .andExpect(jsonPath("$.bio").value("Updated bio"));

        // Verify DB
        User updated = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updated.getDisplayName()).isEqualTo("Updated Name");
        assertThat(updated.getBio()).isEqualTo("Updated bio");
    }

    @Test
    void patchProfile_partialUpdate_onlyUpdatesProvidedFields() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setWebsite("https://newsite.com");
        // bio is NOT set, should remain unchanged

        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + testToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.website").value("https://newsite.com"));

        User updated = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updated.getBio()).isEqualTo("Hello world"); // unchanged
    }

    @Test
    void patchProfile_unauthenticated_returnsUnauthorized() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDisplayName("Hacked");

        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ==================== PUT PROFILE ====================

    @Test
    void updateProfile_validData_returnsUpdatedUser() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDisplayName("Full Update");
        request.setBio("Full bio update");
        request.setWebsite("https://full.com");
        request.setTimezone("Asia/Colombo");
        request.setLocale("en-LK");
        request.setPrivateProfile(true);

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + testToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Full Update"))
                .andExpect(jsonPath("$.bio").value("Full bio update"))
                .andExpect(jsonPath("$.website").value("https://full.com"))
                .andExpect(jsonPath("$.timezone").value("Asia/Colombo"))
                .andExpect(jsonPath("$.privateProfile").value(true));
    }

    // ==================== UPLOAD AVATAR ====================

    @Test
    void uploadAvatar_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/users/me/avatar"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== DELETE AVATAR ====================

    @Test
    void deleteAvatar_authenticated_returnsNoContent() throws Exception {
        // Set an avatar first
        testUser.setAvatarUrl("https://example.com/avatar.png");
        userRepository.save(testUser);

        mockMvc.perform(delete("/api/users/me/avatar")
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isNoContent());

        User updated = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updated.getAvatarUrl()).isNull();
    }

    @Test
    void deleteAvatar_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/users/me/avatar"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== DELETE SELF ====================

    @Test
    void deleteSelf_authenticated_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/users/me")
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isNoContent());

        User deleted = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
        assertThat(deleted.getStatus()).isEqualTo("DELETED");
    }

    @Test
    void deleteSelf_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== DELETE BY ID (ADMIN) ====================

    @Test
    void deleteById_asAdmin_returnsNoContent() throws Exception {
        // Create admin
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

        mockMvc.perform(delete("/api/users/{id}", otherUser.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(otherUser.getId())).isEmpty();
    }

    @Test
    void deleteById_nonExistentUser_returns4xx() throws Exception {
        User admin = User.builder()
                .displayName("Admin")
                .email("admin2@example.com")
                .username("admin2")
                .passwordHash(passwordEncoder.encode("adminpass"))
                .provider("LOCAL")
                .role("ADMIN")
                .status("ACTIVE")
                .build();
        admin = userRepository.save(admin);
        String adminToken = jwtService.generateToken(admin.getId(), admin.getUsername(), admin.getRole());

        mockMvc.perform(delete("/api/users/{id}", 99999L)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().is4xxClientError());
    }
}