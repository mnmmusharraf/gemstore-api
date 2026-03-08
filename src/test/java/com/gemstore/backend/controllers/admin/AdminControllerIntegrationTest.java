package com.gemstore.backend.controllers.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gemstore.backend.entities.user.User;
import com.gemstore.backend.repositories.user.UserRepository;
import com.gemstore.backend.services.auth.JWTService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JWTService jwtService;

    private String adminToken;
    private String userToken;
    private User targetUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        // Create admin user
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
        adminToken = jwtService.generateToken(admin.getId(), admin.getUsername(), admin.getRole());

        // Create regular user
        User regularUser = User.builder()
                .displayName("Regular User")
                .email("user@example.com")
                .username("regularuser")
                .passwordHash(passwordEncoder.encode("userpass"))
                .provider("LOCAL")
                .role("USER")
                .status("ACTIVE")
                .build();
        regularUser = userRepository.save(regularUser);
        userToken = jwtService.generateToken(regularUser.getId(), regularUser.getUsername(), regularUser.getRole());

        // Create target user for status/role updates
        targetUser = User.builder()
                .displayName("Target User")
                .email("target@example.com")
                .username("targetuser")
                .passwordHash(passwordEncoder.encode("targetpass"))
                .provider("LOCAL")
                .role("USER")
                .status("ACTIVE")
                .build();
        targetUser = userRepository.save(targetUser);
    }

    // ==================== UPDATE STATUS ====================

    @Test
    void updateStatus_asAdmin_returnsOk() throws Exception {
        mockMvc.perform(patch("/api/admin/users/{id}/status/{status}", targetUser.getId(), "SUSPENDED")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User status updated to SUSPENDED"))
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));

        // Verify in database
        User updated = userRepository.findById(targetUser.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("SUSPENDED");
    }

    @Test
    void updateStatus_toBanned_returnsOk() throws Exception {
        mockMvc.perform(patch("/api/admin/users/{id}/status/{status}", targetUser.getId(), "BANNED")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("BANNED"));
    }

    @Test
    void updateStatus_toLocked_returnsOk() throws Exception {
        mockMvc.perform(patch("/api/admin/users/{id}/status/{status}", targetUser.getId(), "LOCKED")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("LOCKED"));
    }

    @Test
    void updateStatus_asRegularUser_returnsForbidden() throws Exception {
        mockMvc.perform(patch("/api/admin/users/{id}/status/{status}", targetUser.getId(), "SUSPENDED")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateStatus_withoutAuth_returnsUnauthorized() throws Exception {
        mockMvc.perform(patch("/api/admin/users/{id}/status/{status}", targetUser.getId(), "SUSPENDED"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateStatus_nonExistentUser_returns4xx() throws Exception {
        mockMvc.perform(patch("/api/admin/users/{id}/status/{status}", 99999L, "SUSPENDED")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().is4xxClientError());
    }

    // ==================== UPDATE ROLE ====================

    @Test
    void updateRole_asAdmin_returnsOk() throws Exception {
        mockMvc.perform(patch("/api/admin/users/{id}/role/{role}", targetUser.getId(), "ADMIN")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User role updated to ADMIN"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));

        // Verify in database
        User updated = userRepository.findById(targetUser.getId()).orElseThrow();
        assertThat(updated.getRole()).isEqualTo("ADMIN");
    }

    @Test
    void updateRole_demoteToUser_returnsOk() throws Exception {
        // First promote
        targetUser.setRole("ADMIN");
        userRepository.save(targetUser);

        mockMvc.perform(patch("/api/admin/users/{id}/role/{role}", targetUser.getId(), "USER")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    void updateRole_asRegularUser_returnsForbidden() throws Exception {
        mockMvc.perform(patch("/api/admin/users/{id}/role/{role}", targetUser.getId(), "ADMIN")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateRole_withoutAuth_returnsUnauthorized() throws Exception {
        mockMvc.perform(patch("/api/admin/users/{id}/role/{role}", targetUser.getId(), "ADMIN"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateRole_invalidRole_returns4xx() throws Exception {
        mockMvc.perform(patch("/api/admin/users/{id}/role/{role}", targetUser.getId(), "SUPERADMIN")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void updateRole_nonExistentUser_returns4xx() throws Exception {
        mockMvc.perform(patch("/api/admin/users/{id}/role/{role}", 99999L, "ADMIN")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().is4xxClientError());
    }
}