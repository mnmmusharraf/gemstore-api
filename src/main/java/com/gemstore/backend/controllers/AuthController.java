package com.gemstore.backend.controllers;



import com.gemstore.backend.dtos.auth.AuthResponse;
import com.gemstore.backend.dtos.auth.LoginRequest;
import com.gemstore.backend.dtos.auth.RegisterUserRequest;
import com.gemstore.backend.dtos.user.UserResponse;
import com.gemstore.backend.mappers.user.UserMapper;
import com.gemstore.backend.security.UserPrincipal;
import com.gemstore.backend.services.auth.AuthService;
import com.gemstore.backend.services.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Authentication endpoints: register, login, current user.
 *
 * NOTE: Ensure these routes are permitted in SecurityConfig:
 *   .requestMatchers("/api/auth/**").permitAll()
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final UserMapper userMapper;

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    /**
     * Register a local (username/password) user.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        logger.info("[Controller] Register endpoint called");
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Login with username or email + password.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        System.out.println(response);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns the authenticated user's profile (self view).
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        var user = userService.getById(principal.getId());
        return ResponseEntity.ok(userMapper.toUserResponse(user));
    }
}
