package com.gemstore.backend.services.message;

import com.gemstore.backend.dtos.message.*;
import com.gemstore.backend.entities.message.Message;
import com.gemstore.backend.entities.user.User;
import com.gemstore.backend.enums.MessageStatus;
import com.gemstore.backend.exceptions.BadRequestException;
import com.gemstore.backend.exceptions.ResourceNotFoundException;
import com.gemstore.backend.exceptions.UnauthorizedException;
import com.gemstore.backend.mappers.message.MessageMapper;
import com.gemstore.backend.repositories.message.MessageRepository;
import com.gemstore.backend.repositories.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final KafkaMessageProducer kafkaProducer;
    private final MessageMapper messageMapper;

    /**
     * Send a message
     */
    @Transactional
    public MessageResponseDto sendMessage(Long senderId, MessageRequestDto request) {

        if (senderId.equals(request.getReceiverId())) {
            throw new BadRequestException("Cannot send message to yourself");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));

        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found"));

        Message message = messageMapper.toEntity(request);
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setStatus(MessageStatus.SENT);

        message = messageRepository.save(message);

        log.info("Message saved: id={}, from={} to={}",
                message.getId(), senderId, request.getReceiverId());

        MessageResponseDto response = messageMapper.toResponseDto(message, senderId);

        MessageEventDto event = messageMapper.toEventDto(message);
        kafkaProducer.sendMessage(event);

        return response;
    }

    /**
     * Get single message by ID
     */
    @Transactional(readOnly = true)
    public MessageResponseDto getMessageById(Long messageId, Long currentUserId) {

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        if (!message.getSender().getId().equals(currentUserId) &&
                !message.getReceiver().getId().equals(currentUserId)) {
            throw new UnauthorizedException("You don't have access to this message");
        }

        return messageMapper.toResponseDto(message, currentUserId);
    }

    /**
     * Get conversation between two users
     */
    @Transactional(readOnly = true)
    public Page<MessageResponseDto> getConversation(Long currentUserId,
                                                    Long otherUserId,
                                                    Pageable pageable) {

        Page<Message> messages =
                messageRepository.findConversation(currentUserId, otherUserId, pageable);

        return messages.map(msg -> messageMapper.toResponseDto(msg, currentUserId));
    }

    /**
     * Search messages
     */
    @Transactional(readOnly = true)
    public Page<MessageResponseDto> searchMessages(Long userId,
                                                   String query,
                                                   Long partnerId,
                                                   Pageable pageable) {

        if (query == null || query.trim().isEmpty()) {
            throw new BadRequestException("Search query cannot be empty");
        }

        Page<Message> messages;

        if (partnerId != null) {
            messages = messageRepository.searchInConversation(
                    userId, partnerId, query.trim(), pageable);
        } else {
            messages = messageRepository.searchAllMessages(
                    userId, query.trim(), pageable);
        }

        return messages.map(msg -> messageMapper.toResponseDto(msg, userId));
    }

    /**
     * Get all conversations for user (last message + unread count)
     */
    @Transactional(readOnly = true)
    public List<ConversationDto> getUserConversations(Long userId) {

        List<Message> lastMessages =
                messageRepository.findUserConversations(userId);

        return lastMessages.stream()
                .map(msg -> {

                    User partner = msg.getSender().getId().equals(userId)
                            ? msg.getReceiver()
                            : msg.getSender();

                    Integer unreadCount =
                            messageRepository.countUnreadFromUser(userId, partner.getId());

                    return messageMapper.toConversationDto(msg, userId, unreadCount);
                })
                .collect(Collectors.toList());
    }

    /**
     * Mark messages as read
     */
    @Transactional
    public void markAsRead(Long currentUserId, Long otherUserId) {

        LocalDateTime now = LocalDateTime.now();

        int updated = messageRepository.markMessagesAsRead(
                currentUserId,
                otherUserId,
                MessageStatus.READ,
                now
        );

        log.info("Messages marked as read: receiver={}, sender={}, count={}",
                currentUserId, otherUserId, updated);

        if (updated > 0) {
            MessageEventDto event =
                    messageMapper.createReadReceiptEvent(otherUserId, currentUserId);

            kafkaProducer.sendStatusUpdate(event);
        }
    }

    /**
     * Get total unread count
     */
    @Transactional(readOnly = true)
    public Integer getUnreadCount(Long userId) {
        return messageRepository.countUnreadMessages(userId);
    }

    /**
     * Get unread count from specific sender
     */
    @Transactional(readOnly = true)
    public Integer getUnreadCountFromUser(Long receiverId, Long senderId) {
        return messageRepository.countUnreadFromUser(receiverId, senderId);
    }

    /**
     * Send typing indicator
     */
    public void sendTypingIndicator(Long senderId,
                                    Long receiverId,
                                    Boolean isTyping) {

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        MessageEventDto event = messageMapper.createTypingEvent(
                senderId,
                receiverId,
                sender.getUsername(),
                isTyping
        );

        kafkaProducer.sendTypingIndicator(event);
    }

    /**
     * Soft delete message
     */
    @Transactional
    public void deleteMessage(Long messageId, Long userId) {

        int deleted = messageRepository.softDelete(
                messageId,
                userId,
                LocalDateTime.now()
        );

        if (deleted == 0) {
            throw new ResourceNotFoundException(
                    "Message not found or you don't have permission to delete it");
        }

        log.info("Message deleted: id={}, userId={}", messageId, userId);
    }
}