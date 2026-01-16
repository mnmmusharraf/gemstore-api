package com.gemstore.backend.repositories.notification;

import com.gemstore.backend.entities.notification.Notification;
import com.gemstore.backend.entities.notification.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Get notifications for a user (paginated, newest first)
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Get notifications by type
    Page<Notification> findByUserIdAndTypeInOrderByCreatedAtDesc(
            Long userId,
            List<NotificationType> types,
            Pageable pageable
    );

    // Count unread notifications
    long countByUserIdAndIsReadFalse(Long userId);

    // Mark single notification as read
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id AND n.user.id = :userId")
    int markAsRead(@Param("id") Long id, @Param("userId") Long userId);

    // Mark all notifications as read for a user
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    int markAllAsRead(@Param("userId") Long userId);


    // Check if notification already exists (to prevent duplicates)
    boolean existsByUserIdAndActorIdAndTypeAndListingId(
            Long userId,
            Long actorId,
            NotificationType type,
            Long listingId
    );

    // For follow notifications (no listing)
    boolean existsByUserIdAndActorIdAndType(
            Long userId,
            Long actorId,
            NotificationType type
    );

    // Delete old notifications (cleanup job)
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoffDate")
    int deleteOldNotifications(@Param("cutoffDate") java.time. Instant cutoffDate);

    // Get recent notifications (for quick check)
    List<Notification> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);
}