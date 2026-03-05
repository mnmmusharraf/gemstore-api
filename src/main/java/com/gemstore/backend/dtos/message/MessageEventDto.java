package com.gemstore.backend.dtos.message;

import com.gemstore.backend.entities.message.enums.MessageStatus;
import com.gemstore.backend.entities.message.enums.MessageType;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageEventDto implements Serializable {

    private static final long serialVersionUID = 1L;

    // Event type: NEW_MESSAGE, STATUS_UPDATE, TYPING, READ_RECEIPT, DELIVERED
    private String eventType;

    // Message info
    private Long messageId;
    private Long senderId;
    private Long receiverId;
    private String senderUsername;
    private String senderAvatarUrl;
    private String content;
    private MessageType messageType;
    private String attachmentUrl;
    private Long listingId;
    private MessageStatus status;
    private LocalDateTime timestamp;

    // For typing indicators
    private Boolean isTyping;
}