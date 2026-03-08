package com.gemstore.backend.services.auth;

import com.gemstore.backend.entities.user.User;
import com.gemstore.backend.repositories.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MyUserDetailsService - Unit Tests")
class MyUserDetailsServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks
    private MyUserDetailsService myUserDetailsService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = new User();
        activeUser.setId(1L);
        activeUser.setUsername("testuser");
        activeUser.setEmail("test@gemstore.com");
        activeUser.setPasswordHash("$2a$12$hashedpassword");
        activeUser.setRole("USER");
        activeUser.setStatus("ACTIVE");
    }

    @Nested
    @DisplayName("loadUserByUsername()")
    class LoadUserByUsername {

        @Test
        @DisplayName("TC-UDS-001: Should load user by email")
        void shouldLoadByEmail() {
            when(userRepository.findByEmailIgnoreCase("test@gemstore.com"))
                    .thenReturn(Optional.of(activeUser));

            UserDetails result = myUserDetailsService.loadUserByUsername("test@gemstore.com");

            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("testuser");
            assertThat(result.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("TC-UDS-002: Should load user by username when email not found")
        void shouldLoadByUsername() {
            when(userRepository.findByEmailIgnoreCase("testuser")).thenReturn(Optional.empty());
            when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(activeUser));

            UserDetails result = myUserDetailsService.loadUserByUsername("testuser");

            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("TC-UDS-003: Should throw when user not found")
        void shouldThrowWhenNotFound() {
            when(userRepository.findByEmailIgnoreCase("ghost@test.com")).thenReturn(Optional.empty());
            when(userRepository.findByUsernameIgnoreCase("ghost@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> myUserDetailsService.loadUserByUsername("ghost@test.com"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("TC-UDS-004: Should throw when account is soft deleted")
        void shouldThrowWhenSoftDeleted() {
            activeUser.setDeletedAt(Instant.now());

            when(userRepository.findByEmailIgnoreCase("test@gemstore.com"))
                    .thenReturn(Optional.of(activeUser));

            assertThatThrownBy(() -> myUserDetailsService.loadUserByUsername("test@gemstore.com"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("deleted");
        }

        @Test
        @DisplayName("TC-UDS-005: Should throw when account is locked")
        void shouldThrowWhenLocked() {
            activeUser.setLockedUntil(Instant.now().plusSeconds(3600));

            when(userRepository.findByEmailIgnoreCase("test@gemstore.com"))
                    .thenReturn(Optional.of(activeUser));

            assertThatThrownBy(() -> myUserDetailsService.loadUserByUsername("test@gemstore.com"))
                    .isInstanceOf(LockedException.class)
                    .hasMessageContaining("locked");
        }

        @Test
        @DisplayName("TC-UDS-006: Should throw when account is not active")
        void shouldThrowWhenNotActive() {
            activeUser.setStatus("BANNED");

            when(userRepository.findByEmailIgnoreCase("test@gemstore.com"))
                    .thenReturn(Optional.of(activeUser));

            assertThatThrownBy(() -> myUserDetailsService.loadUserByUsername("test@gemstore.com"))
                    .isInstanceOf(DisabledException.class)
                    .hasMessageContaining("not active");
        }

        @Test
        @DisplayName("TC-UDS-007: Should include correct role authority")
        void shouldIncludeCorrectAuthority() {
            activeUser.setRole("ADMIN");

            when(userRepository.findByEmailIgnoreCase("test@gemstore.com"))
                    .thenReturn(Optional.of(activeUser));

            UserDetails result = myUserDetailsService.loadUserByUsername("test@gemstore.com");

            assertThat(result.getAuthorities())
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        }
    }
}