package com.gemstore.backend.dtos.message;

import com.gemstore.backend.enums.MessageStatus;
import com.gemstore.backend.enums.MessageType;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponseDto {

    private Long id;

    // Sender info
    private Long senderId;
    private String senderUsername;
    private String senderDisplayName;
    private String senderAvatarUrl;

    // Receiver info
    private Long receiverId;
    private String receiverUsername;
    private String receiverDisplayName;
    private String receiverAvatarUrl;

    // Message content
    private String content;
    private MessageType messageType;
    private String attachmentUrl;
    private Long listingId;

    // Listing preview (for LISTING type messages)
    private ListingPreviewDto listingPreview;

    // Status
    private MessageStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    // Context
    private Boolean isOwnMessage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListingPreviewDto {
        private Long id;
        private String title;
        private String imageUrl;
        private Double price;
        private String currency;
        private String gemType;
        private Double caratWeight;
    }
}