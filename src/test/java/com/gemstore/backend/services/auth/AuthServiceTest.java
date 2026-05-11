package com.gemstore.backend.services.auth;

import com.gemstore.backend.dtos.auth.AuthResponse;
import com.gemstore.backend.dtos.auth.LoginRequest;
import com.gemstore.backend.dtos.auth.RegisterUserRequest;
import com.gemstore.backend.dtos.user.UserResponse;
import com.gemstore.backend.entities.user.User;
import com.gemstore.backend.mappers.user.UserMapper;
import com.gemstore.backend.repositories.user.UserRepository;
import com.gemstore.backend.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - Unit Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JWTService jwtService;

    // FIX: AuthService requires this, and register() calls it
    @Mock private EmailVerificationOtpService otpService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private UserResponse testUserResponse;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@gemstore.com");
        testUser.setDisplayName("Test User");
        testUser.setRole("USER");
        testUser.setStatus("ACTIVE");
        testUser.setPasswordHash("$2a$12$hashedpassword");

        testUserResponse = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .email("test@gemstore.com")
                .displayName("Test User")
                .role("USER")
                .build();
    }

    @Nested
    @DisplayName("register()")
    class Register {

        private RegisterUserRequest validRequest;

        @BeforeEach
        void setUp() {
            validRequest = new RegisterUserRequest();
            validRequest.setUsername("newuser");
            validRequest.setEmail("new@gemstore.com");
            validRequest.setPassword("SecurePass123");
            validRequest.setDisplayName("New User");
        }

        @Test
        @DisplayName("TC-AUTH-001: Should register user with valid data")
        void shouldRegisterWithValidData() {
            User mappedUser = new User();
            mappedUser.setRole("USER");

            when(userRepository.existsByEmailIgnoreCase("new@gemstore.com")).thenReturn(false);
            when(userRepository.existsByUsernameIgnoreCase("newuser")).thenReturn(false);
            when(userMapper.toEntity(validRequest)).thenReturn(mappedUser);
            when(passwordEncoder.encode("SecurePass123")).thenReturn("$2a$12$encodedhash");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });

            // FIX: register() calls otpService
            doNothing().when(otpService).generateAndSendOtp(any(User.class));

            when(jwtService.generateToken(1L, "newuser", "USER")).thenReturn("jwt-token-123");

            UserResponse newUserResponse = UserResponse.builder()
                    .id(1L)
                    .username("newuser")
                    .email("new@gemstore.com")
                    .displayName("New User")
                    .role("USER")
                    .build();
            when(userMapper.toUserResponse(any(User.class))).thenReturn(newUserResponse);

            AuthResponse response = authService.register(validRequest);

            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("jwt-token-123");
            assertThat(response.getTokenType()).isEqualTo("Bearer");
            assertThat(response.isNewUser()).isTrue();
            assertThat(response.getUser().getUsername()).isEqualTo("newuser");

            verify(userRepository).save(any(User.class));
            verify(passwordEncoder).encode("SecurePass123");
            verify(otpService).generateAndSendOtp(any(User.class));
            verify(jwtService).generateToken(1L, "newuser", "USER");
        }

        @Test
        @DisplayName("TC-AUTH-002: Should reject duplicate email")
        void shouldRejectDuplicateEmail() {
            when(userRepository.existsByEmailIgnoreCase("new@gemstore.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(validRequest))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Email already used");

            verify(userRepository, never()).save(any());
            verify(otpService, never()).generateAndSendOtp(any());
        }

        @Test
        @DisplayName("TC-AUTH-003: Should reject duplicate username")
        void shouldRejectDuplicateUsername() {
            when(userRepository.existsByEmailIgnoreCase("new@gemstore.com")).thenReturn(false);
            when(userRepository.existsByUsernameIgnoreCase("newuser")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(validRequest))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Username already taken");

            verify(userRepository, never()).save(any());
            verify(otpService, never()).generateAndSendOtp(any());
        }

        @Test
        @DisplayName("TC-AUTH-004: Should trim and lowercase email")
        void shouldNormalizeEmail() {
            validRequest.setEmail("  NEW@GemStore.COM  ");

            User mappedUser = new User();
            mappedUser.setRole("USER");

            when(userRepository.existsByEmailIgnoreCase("new@gemstore.com")).thenReturn(false);
            when(userRepository.existsByUsernameIgnoreCase("newuser")).thenReturn(false);
            when(userMapper.toEntity(validRequest)).thenReturn(mappedUser);
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hash");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });

            // FIX: register() calls otpService
            doNothing().when(otpService).generateAndSendOtp(any(User.class));

            when(jwtService.generateToken(anyLong(), anyString(), anyString())).thenReturn("token");
            when(userMapper.toUserResponse(any(User.class))).thenReturn(testUserResponse);

            authService.register(validRequest);

            verify(userRepository).existsByEmailIgnoreCase("new@gemstore.com");
            verify(otpService).generateAndSendOtp(any(User.class));
        }

        @Test
        @DisplayName("TC-AUTH-005: Should set default role to USER when null")
        void shouldSetDefaultRole() {
            User userWithoutRole = new User();
            userWithoutRole.setRole(null);

            when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
            when(userRepository.existsByUsernameIgnoreCase(anyString())).thenReturn(false);
            when(userMapper.toEntity(validRequest)).thenReturn(userWithoutRole);
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hash");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });

            // FIX: register() calls otpService
            doNothing().when(otpService).generateAndSendOtp(any(User.class));

            when(jwtService.generateToken(anyLong(), anyString(), anyString())).thenReturn("token");
            when(userMapper.toUserResponse(any(User.class))).thenReturn(testUserResponse);

            authService.register(validRequest);

            assertThat(userWithoutRole.getRole()).isEqualTo("USER");
            verify(otpService).generateAndSendOtp(any(User.class));
        }

        @Test
        @DisplayName("TC-AUTH-006: Should encode password before saving")
        void shouldEncodePassword() {
            User mappedUser = new User();
            mappedUser.setRole("USER");

            when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
            when(userRepository.existsByUsernameIgnoreCase(anyString())).thenReturn(false);
            when(userMapper.toEntity(validRequest)).thenReturn(mappedUser);
            when(passwordEncoder.encode("SecurePass123")).thenReturn("$2a$12$encodedhash");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });

            // FIX: register() calls otpService
            doNothing().when(otpService).generateAndSendOtp(any(User.class));

            when(jwtService.generateToken(anyLong(), anyString(), anyString())).thenReturn("token");
            when(userMapper.toUserResponse(any(User.class))).thenReturn(testUserResponse);

            authService.register(validRequest);

            verify(passwordEncoder).encode("SecurePass123");
            verify(otpService).generateAndSendOtp(any(User.class));
            assertThat(mappedUser.getPasswordHash()).isEqualTo("$2a$12$encodedhash");
        }
    }

    @Nested
    @DisplayName("login()")
    class Login {

        private LoginRequest validLoginRequest;

        @BeforeEach
        void setUp() {
            validLoginRequest = new LoginRequest();
            validLoginRequest.setIdentifier("testuser");
            validLoginRequest.setPassword("correct-password");
        }

        @Test
        @DisplayName("TC-AUTH-007: Should login with valid credentials")
        void shouldLoginWithValidCredentials() {
            CustomUserDetails userDetails = new CustomUserDetails(1L, "testuser", "USER");

            Authentication authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);

            when(jwtService.generateToken(1L, "testuser", "USER")).thenReturn("login-jwt-token");

            // FIX: AuthService.login uses findActiveById()
            when(userRepository.findActiveById(1L)).thenReturn(Optional.of(testUser));

            when(userMapper.toUserResponse(testUser)).thenReturn(testUserResponse);

            AuthResponse response = authService.login(validLoginRequest);

            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("login-jwt-token");
            assertThat(response.getTokenType()).isEqualTo("Bearer");
            assertThat(response.isNewUser()).isFalse();
            assertThat(response.getUser().getUsername()).isEqualTo("testuser");

            verify(authenticationManager).authenticate(
                    argThat(auth ->
                            auth.getPrincipal().equals("testuser") &&
                                    auth.getCredentials().equals("correct-password")
                    )
            );
        }

        @Test
        @DisplayName("TC-AUTH-008: Should reject wrong password")
        void shouldRejectWrongPassword() {
            validLoginRequest.setPassword("wrong-password");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(validLoginRequest))
                    .isInstanceOf(BadCredentialsException.class);

            verify(jwtService, never()).generateToken(anyLong(), anyString(), anyString());
        }

        @Test
        @DisplayName("TC-AUTH-009: Should reject non-existent user")
        void shouldRejectNonExistentUser() {
            validLoginRequest.setIdentifier("ghostuser");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(validLoginRequest))
                    .isInstanceOf(BadCredentialsException.class);

            verify(jwtService, never()).generateToken(anyLong(), anyString(), anyString());
        }

        @Test
        @DisplayName("TC-AUTH-010: Should extract ADMIN role correctly (remove ROLE_ prefix)")
        void shouldExtractRoleCorrectly() {
            CustomUserDetails adminDetails = new CustomUserDetails(2L, "adminuser", "ADMIN");

            Authentication authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn(adminDetails);
            when(authenticationManager.authenticate(any())).thenReturn(authentication);

            User adminUser = new User();
            adminUser.setId(2L);
            adminUser.setUsername("adminuser");
            adminUser.setRole("ADMIN");

            // FIX: AuthService.login uses findActiveById()
            when(userRepository.findActiveById(2L)).thenReturn(Optional.of(adminUser));

            when(jwtService.generateToken(2L, "adminuser", "ADMIN")).thenReturn("admin-token");
            when(userMapper.toUserResponse(adminUser)).thenReturn(testUserResponse);

            validLoginRequest.setIdentifier("adminuser");

            AuthResponse response = authService.login(validLoginRequest);

            verify(jwtService).generateToken(2L, "adminuser", "ADMIN");
            assertThat(response.getToken()).isEqualTo("admin-token");
        }

        @Test
        @DisplayName("TC-AUTH-011: Should login with email as identifier")
        void shouldLoginWithEmail() {
            validLoginRequest.setIdentifier("test@gemstore.com");

            CustomUserDetails userDetails = new CustomUserDetails(1L, "testuser", "USER");

            Authentication authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(authenticationManager.authenticate(any())).thenReturn(authentication);

            when(jwtService.generateToken(1L, "testuser", "USER")).thenReturn("email-login-token");

            // FIX: AuthService.login uses findActiveById()
            when(userRepository.findActiveById(1L)).thenReturn(Optional.of(testUser));

            when(userMapper.toUserResponse(testUser)).thenReturn(testUserResponse);

            AuthResponse response = authService.login(validLoginRequest);

            assertThat(response.getToken()).isEqualTo("email-login-token");
            verify(authenticationManager).authenticate(
                    argThat(auth -> auth.getPrincipal().equals("test@gemstore.com"))
            );
        }
    }
}