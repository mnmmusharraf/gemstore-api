package com.gemstore.backend.services.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JWTService - Unit Tests")
class JWTServiceTest {

    private JWTService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JWTService();
        // Create a valid Base64-encoded 256-bit secret
        String secret = Base64.getEncoder().encodeToString(
                "my-super-secret-key-for-testing-gemstore-1234567890".getBytes()
        );
        ReflectionTestUtils.setField(jwtService, "secretKeyBase64", secret);
        ReflectionTestUtils.setField(jwtService, "ttlSeconds", 3600L);
    }

    @Nested
    @DisplayName("generateToken()")
    class GenerateToken {

        @Test
        @DisplayName("TC-JWT-001: Should generate token with correct claims")
        void shouldGenerateTokenWithClaims() {
            String token = jwtService.generateToken(1L, "admin", "ADMIN");

            assertThat(token).isNotBlank();
            assertThat(jwtService.extractUsername(token)).isEqualTo("admin");
            assertThat(jwtService.extractUserId(token)).isEqualTo(1L);
            assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("TC-JWT-002: Should throw when userId is null")
        void shouldThrowForNullUserId() {
            assertThatThrownBy(() -> jwtService.generateToken(null, "admin", "ADMIN"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("userId cannot be null");
        }

        @Test
        @DisplayName("TC-JWT-003: Should generate unique tokens for different users")
        void shouldGenerateUniqueTokens() {
            String token1 = jwtService.generateToken(1L, "user1", "USER");
            String token2 = jwtService.generateToken(2L, "user2", "USER");

            assertThat(token1).isNotEqualTo(token2);
        }
    }

    @Nested
    @DisplayName("extractUsername()")
    class ExtractUsername {

        @Test
        @DisplayName("TC-JWT-004: Should extract username correctly")
        void shouldExtractUsername() {
            String token = jwtService.generateToken(1L, "testuser", "USER");

            assertThat(jwtService.extractUsername(token)).isEqualTo("testuser");
        }
    }

    @Nested
    @DisplayName("extractUserId()")
    class ExtractUserId {

        @Test
        @DisplayName("TC-JWT-005: Should extract userId correctly")
        void shouldExtractUserId() {
            String token = jwtService.generateToken(42L, "testuser", "USER");

            assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
        }

        @Test
        @DisplayName("TC-JWT-006: Should handle large userId")
        void shouldHandleLargeUserId() {
            String token = jwtService.generateToken(999999L, "testuser", "USER");

            assertThat(jwtService.extractUserId(token)).isEqualTo(999999L);
        }
    }

    @Nested
    @DisplayName("extractRole()")
    class ExtractRole {

        @Test
        @DisplayName("TC-JWT-007: Should extract USER role")
        void shouldExtractUserRole() {
            String token = jwtService.generateToken(1L, "testuser", "USER");

            assertThat(jwtService.extractRole(token)).isEqualTo("USER");
        }

        @Test
        @DisplayName("TC-JWT-008: Should extract ADMIN role")
        void shouldExtractAdminRole() {
            String token = jwtService.generateToken(1L, "admin", "ADMIN");

            assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
        }
    }

    @Nested
    @DisplayName("isValid()")
    class IsValid {

        @Test
        @DisplayName("TC-JWT-009: Should validate correct token")
        void shouldValidateCorrectToken() {
            String token = jwtService.generateToken(1L, "testuser", "USER");

            assertThat(jwtService.isValid(token, "testuser")).isTrue();
        }

        @Test
        @DisplayName("TC-JWT-010: Should reject wrong username")
        void shouldRejectWrongUsername() {
            String token = jwtService.generateToken(1L, "testuser", "USER");

            assertThat(jwtService.isValid(token, "hacker")).isFalse();
        }

        @Test
        @DisplayName("TC-JWT-011: Should reject tampered token")
        void shouldRejectTamperedToken() {
            String token = jwtService.generateToken(1L, "testuser", "USER");

            assertThat(jwtService.isValid(token + "tampered", "testuser")).isFalse();
        }

        @Test
        @DisplayName("TC-JWT-012: Should reject expired token")
        void shouldRejectExpiredToken() {
            // Set TTL to negative so token is already expired
            ReflectionTestUtils.setField(jwtService, "ttlSeconds", -1L);
            String token = jwtService.generateToken(1L, "testuser", "USER");

            assertThat(jwtService.isValid(token, "testuser")).isFalse();
        }

        @Test
        @DisplayName("TC-JWT-013: Should reject completely invalid token")
        void shouldRejectInvalidToken() {
            assertThat(jwtService.isValid("not.a.token", "testuser")).isFalse();
        }
    }
}