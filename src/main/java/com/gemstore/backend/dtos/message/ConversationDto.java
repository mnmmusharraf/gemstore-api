package com.gemstore.backend.dtos.message;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDto {

    // Partner info (the other user in conversation)
    private Long partnerId;
    private String partnerUsername;
    private String partnerDisplayName;
    private String partnerAvatarUrl;
    private Boolean partnerIsOnline;
    private LocalDateTime partnerLastSeenAt;

    // Last message
    private MessageResponseDto lastMessage;

    // Conversation metadata
    private Integer unreadCount;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
}