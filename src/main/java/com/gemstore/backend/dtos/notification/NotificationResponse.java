package com.gemstore.backend. dtos.notification;

import lombok.*;

import java.time. Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private String type;
    private String message;
    private Boolean isRead;
    private Instant createdAt;
    private ActorDto actor;
    private ListingDto listing;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActorDto {
        private Long id;
        private String username;
        private String displayName;
        private String avatarUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListingDto {
        private Long id;
        private String title;
        private String primaryImageUrl;
    }
}