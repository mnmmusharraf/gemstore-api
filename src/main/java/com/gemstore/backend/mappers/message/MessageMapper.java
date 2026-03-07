package com.gemstore.backend.mappers.message;

import com.gemstore.backend.dtos.message.ConversationDto;
import com.gemstore.backend.dtos.message.MessageEventDto;
import com.gemstore.backend.dtos.message.MessageRequestDto;
import com.gemstore.backend.dtos.message.MessageResponseDto;
import com.gemstore.backend.entities.message.Message;
import com.gemstore.backend.entities.user.User;
import com.gemstore.backend.entities.message.enums.MessageStatus;
import org.mapstruct.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MessageMapper converts between Message entity and various DTOs.
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = {LocalDateTime.class, MessageStatus.class}
)
public interface MessageMapper {

    /* ===================== Entity -> MessageResponseDto ===================== */

    @Mapping(target = "id", source = "message.id")
    @Mapping(target = "senderId", source = "message.sender.id")
    @Mapping(target = "senderUsername", source = "message.sender.username")
    @Mapping(target = "senderDisplayName", source = "message.sender.displayName")
    @Mapping(target = "senderAvatarUrl", source = "message.sender.avatarUrl")
    @Mapping(target = "receiverId", source = "message.receiver.id")
    @Mapping(target = "receiverUsername", source = "message.receiver.username")
    @Mapping(target = "receiverDisplayName", source = "message.receiver.displayName")
    @Mapping(target = "receiverAvatarUrl", source = "message.receiver.avatarUrl")
    @Mapping(target = "content", source = "message.content")
    @Mapping(target = "messageType", source = "message.messageType")
    @Mapping(target = "attachmentUrl", source = "message.attachmentUrl")
    @Mapping(target = "listingId", source = "message.listingId")
    @Mapping(target = "status", source = "message.status")
    @Mapping(target = "createdAt", source = "message.createdAt")
    @Mapping(target = "readAt", source = "message.readAt")
    @Mapping(target = "isOwnMessage", expression = "java(message.getSender().getId().equals(currentUserId))")
    MessageResponseDto toResponseDto(Message message, @Context Long currentUserId);

    /* ===================== Entity List -> Response List ===================== */

    default List<MessageResponseDto> toResponseDtoList(List<Message> messages, Long currentUserId) {
        if (messages == null) {
            return List.of();
        }
        return messages.stream()
                .map(msg -> toResponseDto(msg, currentUserId))
                .toList();
    }

    /* ===================== Entity -> MessageEventDto (for Kafka) ===================== */

    @Mapping(target = "eventType", constant = "NEW_MESSAGE")
    @Mapping(target = "messageId", source = "message.id")
    @Mapping(target = "senderId", source = "message.sender.id")
    @Mapping(target = "receiverId", source = "message.receiver.id")
    @Mapping(target = "senderUsername", source = "message.sender.username")
    @Mapping(target = "senderAvatarUrl", source = "message.sender.avatarUrl")
    @Mapping(target = "content", source = "message.content")
    @Mapping(target = "messageType", source = "message.messageType")
    @Mapping(target = "attachmentUrl", source = "message.attachmentUrl")
    @Mapping(target = "listingId", source = "message.listingId")
    @Mapping(target = "status", source = "message.status")
    @Mapping(target = "timestamp", expression = "java(LocalDateTime.now())")
    @Mapping(target = "isTyping", ignore = true)
    MessageEventDto toEventDto(Message message);

    /* ===================== Request -> Entity ===================== */

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sender", ignore = true)  // Set in service
    @Mapping(target = "receiver", ignore = true)  // Set in service
    @Mapping(target = "content", source = "content")
    @Mapping(target = "messageType", source = "messageType", defaultValue = "TEXT")
    @Mapping(target = "attachmentUrl", source = "attachmentUrl")
    @Mapping(target = "listingId", source = "listingId")
    @Mapping(target = "status", constant = "SENT")
    @Mapping(target = "readAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "isDeleted", constant = "false")
    Message toEntity(MessageRequestDto request);

    /* ===================== Message -> ConversationDto ===================== */

    default ConversationDto toConversationDto(Message lastMessage, Long currentUserId, Integer unreadCount) {
        if (lastMessage == null) {
            return null;
        }

        // Determine conversation partner
        User partner = lastMessage.getSender().getId().equals(currentUserId)
                ? lastMessage.getReceiver()
                : lastMessage.getSender();

        return ConversationDto.builder()
                .partnerId(partner.getId())
                .partnerUsername(partner.getUsername())
                .partnerDisplayName(partner.getDisplayName())
                .partnerAvatarUrl(partner.getAvatarUrl())
                .partnerIsOnline(false) // TODO: Implement online status
                .lastMessage(toResponseDto(lastMessage, currentUserId))
                .unreadCount(unreadCount != null ? unreadCount : 0)
                .lastMessageAt(lastMessage.getCreatedAt())
                .build();
    }

    /* ===================== Helper: Create Read Receipt Event ===================== */

    default MessageEventDto createReadReceiptEvent(Long conversationPartnerId, Long currentUserId) {
        return MessageEventDto.builder()
                .eventType("READ_RECEIPT")
                .senderId(conversationPartnerId)  // Original sender gets notified
                .receiverId(currentUserId)
                .status(MessageStatus.READ)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /* ===================== Helper: Create Typing Event ===================== */

    default MessageEventDto createTypingEvent(Long senderId, Long receiverId, String username, Boolean isTyping) {
        return MessageEventDto.builder()
                .eventType("TYPING")
                .senderId(senderId)
                .receiverId(receiverId)
                .senderUsername(username)
                .isTyping(isTyping)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /* ===================== Helper: Create Status Update Event ===================== */

    default MessageEventDto createStatusUpdateEvent(Message message, MessageStatus newStatus) {
        return MessageEventDto.builder()
                .eventType("STATUS_UPDATE")
                .messageId(message.getId())
                .senderId(message.getSender().getId())
                .receiverId(message.getReceiver().getId())
                .status(newStatus)
                .timestamp(LocalDateTime.now())
                .build();
    }
}