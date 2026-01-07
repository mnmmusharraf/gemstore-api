package com. gemstore.backend. dtos.user;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryResponse {
    private Long id;
    private String username;
    private String displayName;
    private String avatarUrl;
    private Boolean isFollowing;
}