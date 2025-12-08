package com.gemstore.backend.dtos;


import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String displayName;
    private String username;
    private String email;
    private String avatarUrl;
    private String provider;
    private boolean emailVerified;
    private String role;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
    private String timezone;
    private String locale;
}
