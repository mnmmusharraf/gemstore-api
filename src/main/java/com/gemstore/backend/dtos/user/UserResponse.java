package com.gemstore.backend.dtos.user;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

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

    // New profile fields
    private String website;
    private String bio;
    private LocalDate dateOfBirth;

    @JsonProperty("privateProfile")
    private Boolean privateProfile;

    // Social stats
    private Integer postsCount;
    private Integer followersCount;
    private Integer followingCount;
}
