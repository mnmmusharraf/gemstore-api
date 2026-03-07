package com.gemstore.backend.services.message;

import com.gemstore.backend.dtos.message.MessageEventDto;
import com.gemstore.backend.entities.message.enums.MessageStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer for sending message events to different topics.
 * Handles new messages, status updates, and typing indicators.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaMessageProducer {

    private final KafkaTemplate<String, MessageEventDto> kafkaTemplate;

    @Value("${messaging.kafka.topics.messages:gemstore.messages}")
    private String messagesTopic;

    @Value("${messaging.kafka.topics.message-status:gemstore.message-status}")
    private String messageStatusTopic;

    @Value("${messaging.kafka.topics.typing:gemstore.typing}")
    private String typingTopic;

    /**
     * Send a new message event to Kafka.
     * This is used when a user sends a new message.
     *
     * @param event The message event containing all message details
     */
    public void sendMessage(MessageEventDto event) {
        String key = generateMessageKey(event.getSenderId(), event.getReceiverId());

        Message<MessageEventDto> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, messagesTopic)
                .setHeader(KafkaHeaders.KEY, key)
                .setHeader("eventType", event.getEventType())
                .setHeader("senderId", event.getSenderId())
                .setHeader("receiverId", event.getReceiverId())
                .build();

        CompletableFuture<SendResult<String, MessageEventDto>> future = kafkaTemplate.send(message);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("✅ Message sent to Kafka: topic={}, partition={}, offset={}, messageId={}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        event.getMessageId());
            } else {
                log.error("❌ Failed to send message to Kafka: messageId={}, error={}",
                        event.getMessageId(), ex.getMessage(), ex);
                // TODO: Implement retry logic or dead letter queue
                handleSendFailure(event, ex);
            }
        });
    }

    /**
     * Send a message status update (delivered, read).
     * Used to notify sender when their message status changes.
     *
     * @param event The status update event
     */
    public void sendStatusUpdate(MessageEventDto event) {
        String key = generateStatusKey(event.getMessageId(), event.getSenderId());

        Message<MessageEventDto> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, messageStatusTopic)
                .setHeader(KafkaHeaders.KEY, key)
                .setHeader("eventType", event.getEventType())
                .setHeader("status", event.getStatus() != null ? event.getStatus().name() : null)
                .build();

        CompletableFuture<SendResult<String, MessageEventDto>> future = kafkaTemplate.send(message);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.debug("✅ Status update sent to Kafka: messageId={}, status={}",
                        event.getMessageId(), event.getStatus());
            } else {
                log.error("❌ Failed to send status update: messageId={}, error={}",
                        event.getMessageId(), ex.getMessage());
            }
        });
    }

    /**
     * Send typing indicator.
     * Used to show "user is typing..." in real-time.
     *
     * @param event The typing indicator event
     */
    public void sendTypingIndicator(MessageEventDto event) {
        // Use compact key for typing - will be compacted by Kafka
        String key = generateTypingKey(event.getSenderId(), event.getReceiverId());

        Message<MessageEventDto> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, typingTopic)
                .setHeader(KafkaHeaders.KEY, key)
                .setHeader("eventType", "TYPING")
                .setHeader("isTyping", event.getIsTyping())
                .build();

        // Fire and forget for typing indicators (non-critical)
        kafkaTemplate.send(message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Failed to send typing indicator: senderId={}, error={}",
                                event.getSenderId(), ex.getMessage());
                    }
                });
    }

    /**
     * Send delivery confirmation.
     * Called when a message is delivered to the recipient's device.
     *
     * @param messageId  The message ID
     * @param senderId   Original sender ID (to notify)
     * @param receiverId The receiver ID
     */
    public void sendDeliveryConfirmation(Long messageId, Long senderId, Long receiverId) {
        MessageEventDto event = MessageEventDto.builder()
                .eventType("DELIVERED")
                .messageId(messageId)
                .senderId(senderId)
                .receiverId(receiverId)
                .status(MessageStatus.DELIVERED)
                .timestamp(java.time.LocalDateTime.now())
                .build();

        sendStatusUpdate(event);
    }

    /**
     * Send read receipt.
     * Called when a message is read by the recipient.
     *
     * @param messageId  The message ID
     * @param senderId   Original sender ID (to notify)
     * @param receiverId The receiver ID who read the message
     */
    public void sendReadReceipt(Long messageId, Long senderId, Long receiverId) {
        MessageEventDto event = MessageEventDto.builder()
                .eventType("READ_RECEIPT")
                .messageId(messageId)
                .senderId(senderId)
                .receiverId(receiverId)
                .status(MessageStatus.READ)
                .timestamp(java.time.LocalDateTime.now())
                .build();

        sendStatusUpdate(event);
    }

    // ==================== Key Generation ====================

    /**
     * Generate a consistent key for message partitioning.
     * Messages between same users go to same partition for ordering.
     */
    private String generateMessageKey(Long senderId, Long receiverId) {
        // Ensure consistent ordering regardless of who sends
        long lower = Math.min(senderId, receiverId);
        long higher = Math.max(senderId, receiverId);
        return String.format("conv-%d-%d", lower, higher);
    }

    /**
     * Generate key for status updates.
     */
    private String generateStatusKey(Long messageId, Long senderId) {
        return String.format("status-%d-%d", messageId, senderId);
    }

    /**
     * Generate key for typing indicators (will be compacted).
     */
    private String generateTypingKey(Long senderId, Long receiverId) {
        return String.format("typing-%d-%d", senderId, receiverId);
    }

    // ==================== Error Handling ====================

    /**
     * Handle failed message sends.
     * Could implement retry logic, dead letter queue, etc.
     */
    private void handleSendFailure(MessageEventDto event, Throwable ex) {
        // Log for now - in production, implement:
        // 1. Retry with exponential backoff
        // 2. Dead letter queue
        // 3. Notification to monitoring system
        log.error("Message send failure - messageId: {}, type: {}, error: {}",
                event.getMessageId(),
                event.getEventType(),
                ex.getMessage());

        // TODO: Save to dead letter table for retry
        // deadLetterRepository.save(new DeadLetterMessage(event, ex.getMessage()));
    }
}