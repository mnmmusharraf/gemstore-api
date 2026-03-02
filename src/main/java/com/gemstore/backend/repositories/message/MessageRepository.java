package com.gemstore.backend.repositories.message;

import com.gemstore.backend.entities.message.Message;
import com.gemstore.backend.enums.MessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Get messages between two users (conversation)
     */
    @Query("""
        SELECT m FROM Message m 
        WHERE m.isDeleted = FALSE
        AND ((m.sender.id = :userId1 AND m.receiver.id = :userId2) 
             OR (m.sender.id = :userId2 AND m.receiver.id = :userId1))
        ORDER BY m.createdAt DESC
    """)
    Page<Message> findConversation(
            @Param("userId1") Long userId1,
            @Param("userId2") Long userId2,
            Pageable pageable
    );

    /**
     * Search messages in specific conversation
     */
    @Query("""
        SELECT m FROM Message m 
        WHERE m.isDeleted = FALSE
        AND ((m.sender.id = :userId AND m.receiver.id = :partnerId) 
             OR (m.sender.id = :partnerId AND m.receiver.id = :userId))
        AND LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%'))
        ORDER BY m.createdAt DESC
    """)
    Page<Message> searchInConversation(
            @Param("userId") Long userId,
            @Param("partnerId") Long partnerId,
            @Param("query") String query,
            Pageable pageable
    );

    /**
     * Search messages in all conversations
     */
    @Query("""
        SELECT m FROM Message m 
        WHERE m.isDeleted = FALSE
        AND (m.sender.id = :userId OR m.receiver.id = :userId)
        AND LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%'))
        ORDER BY m.createdAt DESC
    """)
    Page<Message> searchAllMessages(
            @Param("userId") Long userId,
            @Param("query") String query,
            Pageable pageable
    );

    /**
     * Get latest message per conversation partner
     * (Native query because JPQL cannot handle this cleanly)
     */
    @Query(value = """
        WITH conversation_partners AS (
            SELECT 
                CASE WHEN sender_id = :userId THEN receiver_id ELSE sender_id END as partner_id,
                MAX(created_at) as last_message_at
            FROM messages 
            WHERE (sender_id = :userId OR receiver_id = :userId)
            AND is_deleted = FALSE
            GROUP BY partner_id
        )
        SELECT m.* FROM messages m
        INNER JOIN conversation_partners cp 
            ON ((m.sender_id = :userId AND m.receiver_id = cp.partner_id) 
                OR (m.receiver_id = :userId AND m.sender_id = cp.partner_id))
            AND m.created_at = cp.last_message_at
        WHERE m.is_deleted = FALSE
        ORDER BY m.created_at DESC
    """, nativeQuery = true)
    List<Message> findUserConversations(@Param("userId") Long userId);

    /**
     * Count total unread messages
     */
    @Query("""
        SELECT COUNT(m) FROM Message m 
        WHERE m.receiver.id = :userId 
        AND m.status <> com.gemstore.backend.enums.MessageStatus.READ
        AND m.isDeleted = FALSE
    """)
    Integer countUnreadMessages(@Param("userId") Long userId);

    /**
     * Count unread messages from specific sender
     */
    @Query("""
        SELECT COUNT(m) FROM Message m 
        WHERE m.receiver.id = :receiverId 
        AND m.sender.id = :senderId 
        AND m.status <> com.gemstore.backend.enums.MessageStatus.READ
        AND m.isDeleted = FALSE
    """)
    Integer countUnreadFromUser(
            @Param("receiverId") Long receiverId,
            @Param("senderId") Long senderId
    );

    /**
     * Mark messages as read
     */
    @Modifying
    @Query("""
        UPDATE Message m 
        SET m.status = :status, 
            m.readAt = :readAt, 
            m.updatedAt = :readAt
        WHERE m.receiver.id = :receiverId 
        AND m.sender.id = :senderId 
        AND m.status <> com.gemstore.backend.enums.MessageStatus.READ
        AND m.isDeleted = FALSE
    """)
    int markMessagesAsRead(
            @Param("receiverId") Long receiverId,
            @Param("senderId") Long senderId,
            @Param("status") MessageStatus status,
            @Param("readAt") LocalDateTime readAt
    );

    /**
     * Mark single message as delivered
     */
    @Modifying
    @Query("""
        UPDATE Message m 
        SET m.status = com.gemstore.backend.enums.MessageStatus.DELIVERED,
            m.updatedAt = :timestamp
        WHERE m.id = :messageId 
        AND m.status = com.gemstore.backend.enums.MessageStatus.SENT
    """)
    int markAsDelivered(
            @Param("messageId") Long messageId,
            @Param("timestamp") LocalDateTime timestamp
    );

    /**
     * Get last message between two users
     * (Spring Data derived query instead of LIMIT 1)
     */
    Message findTopByIsDeletedFalseAndSenderIdAndReceiverIdOrIsDeletedFalseAndSenderIdAndReceiverIdOrderByCreatedAtDesc(
            Long sender1, Long receiver1,
            Long sender2, Long receiver2
    );

    /**
     * Soft delete message
     */
    @Modifying
    @Query("""
        UPDATE Message m 
        SET m.isDeleted = TRUE, 
            m.deletedAt = :timestamp, 
            m.updatedAt = :timestamp
        WHERE m.id = :messageId 
        AND m.sender.id = :userId
    """)
    int softDelete(
            @Param("messageId") Long messageId,
            @Param("userId") Long userId,
            @Param("timestamp") LocalDateTime timestamp
    );
}