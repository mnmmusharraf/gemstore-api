package com.gemstore.backend.dtos.message;

import com.gemstore.backend.enums.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageRequestDto {

    @NotNull(message = "Receiver ID is required")
    private Long receiverId;

    @NotBlank(message = "Message content is required")
    @Size(max = 5000, message = "Message too long (max 5000 characters)")
    private String content;

    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    private String attachmentUrl;

    private Long listingId;  // For sharing gem listings
}