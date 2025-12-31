package com.gemstore.backend.dtos.auth;


import com.gemstore.backend.dtos.user.UserResponse;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String token;           // JWT or session token
    private UserResponse user;      // Sanitized user info
    private String tokenType;       // "Bearer"
    private long expiresIn;         // seconds (optional)
    private boolean newUser;        // if first-time social login
}