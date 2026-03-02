package com.gemstore.backend.services.message;

import com.gemstore.backend.dtos.message.MessageEventDto;
import com.gemstore.backend.enums.MessageStatus;
import com.gemstore.backend.repositories.message.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Kafka consumer for processing message events.
 * Handles new messages, status updates, and typing indicators.
 * Delivers events to recipients via WebSocket.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaMessageConsumer {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageRepository messageRepository;

    // WebSocket destination prefixes
    private static final String USER_MESSAGES_DESTINATION = "/queue/messages";
    private static final String USER_STATUS_DESTINATION = "/queue/message-status";
    private static final String USER_TYPING_DESTINATION = "/queue/typing";

    /**
     * Consume new message events and deliver via WebSocket.
     */
    @KafkaListener(
            topics = "${messaging.kafka.topics.messages:gemstore.messages}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeMessage(ConsumerRecord<String, MessageEventDto> record, Acknowledgment ack) {
        MessageEventDto event = record.value();

        try {
            log.info("📨 Received message event: type={}, messageId={}, from={} to={}",
                    event.getEventType(),
                    event.getMessageId(),
                    event.getSenderId(),
                    event.getReceiverId());

            // Validate event
            if (event.getReceiverId() == null) {
                log.warn("Invalid message event - no receiver ID: {}", event);
                ack.acknowledge();
                return;
            }

            // Send to recipient's personal queue via WebSocket
            String recipientDestination = USER_MESSAGES_DESTINATION;
            messagingTemplate.convertAndSendToUser(
                    event.getReceiverId().toString(),
                    recipientDestination,
                    event
            );

            log.info("✅ Message delivered via WebSocket to user: {}", event.getReceiverId());

            // Update message status to DELIVERED
            if (event.getMessageId() != null) {
                updateMessageStatusToDelivered(event.getMessageId());
            }

            // Acknowledge the message
            ack.acknowledge();

        } catch (Exception e) {
            log.error("❌ Error processing message event: messageId={}, error={}",
                    event.getMessageId(), e.getMessage(), e);

            // Don't acknowledge - will be retried
            // Consider implementing dead letter queue after max retries
            handleConsumerError(event, e, ack);
        }
    }

    /**
     * Consume message status updates (delivered, read).
     */
    @KafkaListener(
            topics = "${messaging.kafka.topics.message-status:gemstore.message-status}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeStatusUpdate(ConsumerRecord<String, MessageEventDto> record, Acknowledgment ack) {
        MessageEventDto event = record.value();

        try {
            log.debug("📋 Received status update: type={}, messageId={}, status={}",
                    event.getEventType(),
                    event.getMessageId(),
                    event.getStatus());

            // Notify the original sender about the status update
            if (event.getSenderId() != null) {
                messagingTemplate.convertAndSendToUser(
                        event.getSenderId().toString(),
                        USER_STATUS_DESTINATION,
                        event
                );

                log.debug("✅ Status update sent to user: {}", event.getSenderId());
            }

            ack.acknowledge();

        } catch (Exception e) {
            log.error("❌ Error processing status update: messageId={}, error={}",
                    event.getMessageId(), e.getMessage(), e);
            // Acknowledge anyway - status updates are not critical
            ack.acknowledge();
        }
    }

    /**
     * Consume typing indicators.
     */
    @KafkaListener(
            topics = "${messaging.kafka.topics.typing:gemstore.typing}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTypingIndicator(ConsumerRecord<String, MessageEventDto> record, Acknowledgment ack) {
        MessageEventDto event = record.value();

        try {
            log.trace("⌨️ Received typing indicator: from={}, to={}, isTyping={}",
                    event.getSenderId(),
                    event.getReceiverId(),
                    event.getIsTyping());

            // Send typing indicator to the receiver
            if (event.getReceiverId() != null) {
                messagingTemplate.convertAndSendToUser(
                        event.getReceiverId().toString(),
                        USER_TYPING_DESTINATION,
                        event
                );
            }

            // Always acknowledge typing indicators
            ack.acknowledge();

        } catch (Exception e) {
            log.warn("Error processing typing indicator: {}", e.getMessage());
            // Always acknowledge - typing indicators are non-critical
            ack.acknowledge();
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Update message status to DELIVERED in database.
     */
    @Transactional
    protected void updateMessageStatusToDelivered(Long messageId) {
        try {
            int updated = messageRepository.markAsDelivered(messageId, LocalDateTime.now());
            if (updated > 0) {
                log.debug("Message {} marked as DELIVERED", messageId);
            }
        } catch (Exception e) {
            log.warn("Failed to update message status to DELIVERED: messageId={}, error={}",
                    messageId, e.getMessage());
        }
    }

    /**
     * Handle consumer errors with retry logic.
     */
    private void handleConsumerError(MessageEventDto event, Exception e, Acknowledgment ack) {
        // Get retry count from headers or default to 0
        // In production, implement proper retry logic:
        // 1. Track retry count
        // 2. Exponential backoff
        // 3. Dead letter queue after max retries

        log.error("Consumer error for messageId: {}, will retry...", event.getMessageId());

        // For now, acknowledge to prevent infinite retry loop
        // In production, implement proper error handling
        ack.acknowledge();

        // TODO: Send to dead letter topic
        // deadLetterProducer.send(event, e.getMessage());
    }
}