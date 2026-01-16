package com.gemstore.backend.services.notification;

import com.gemstore.backend.dtos.common.PageResponse;
import com.gemstore.backend.dtos.notification.NotificationResponse;
import com.gemstore.backend.entities.listing.Listing;
import com.gemstore.backend.entities.notification.Notification;
import com.gemstore.backend.entities.notification.NotificationType;
import com.gemstore.backend.entities.user.User;
import com.gemstore.backend.mappers.notification.NotificationMapper;
import com.gemstore.backend.repositories.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final NotificationWebSocketService socketService;


    /* ========================= Create Notifications ========================= */

    @Transactional
    public Notification createNotification(
            User user,
            User actor,
            NotificationType type,
            Listing listing,
            String message
    ) {
        // Don't notify yourself
        if (user.getId().equals(actor.getId())) {
            return null;
        }

        // Prevent duplicates
        boolean exists = (listing != null)
                ? notificationRepository.existsByUserIdAndActorIdAndTypeAndListingId(
                user.getId(), actor.getId(), type, listing.getId())
                : notificationRepository.existsByUserIdAndActorIdAndType(
                user.getId(), actor.getId(), type);

        if (exists) {
            return null;
        }

        Notification notification = Notification.builder()
                .user(user)
                .actor(actor)
                .type(type)
                .listing(listing)
                .message(message)
                .isRead(false)
                .build();

        notification = notificationRepository.save(notification);
        socketService.sendToUser(notification);

        log.info("Created notification: {} for user {} from actor {}",
                type, user.getId(), actor.getId());

        return notification;
    }

    /* ========================= Notification Types ========================= */

    @Transactional
    public void notifyLike(User listingOwner, User liker, Listing listing) {
        createNotification(listingOwner, liker, NotificationType.LIKE, listing, null);
    }

    @Transactional
    public void notifyFollow(User followedUser, User follower) {
        createNotification(followedUser, follower, NotificationType.FOLLOW, null, null);
    }

    @Transactional
    public void notifyFollowRequest(User targetUser, User requester) {
        createNotification(targetUser, requester, NotificationType.FOLLOW_REQUEST, null, null);
    }

    @Transactional
    public void notifyFollowAccepted(User requester, User accepter) {
        createNotification(requester, accepter, NotificationType.FOLLOW_ACCEPTED, null, null);
    }

    @Transactional
    public void notifyComment(User listingOwner, User commenter, Listing listing) {
        createNotification(listingOwner, commenter, NotificationType.COMMENT, listing, null);
    }

    /* ========================= Read Operations ========================= */

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications =
                notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return toPageResponse(notifications);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /* ========================= Update Operations ========================= */

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        notificationRepository.markAsRead(notificationId, userId);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    /* ========================= Helpers ========================= */

    private PageResponse<NotificationResponse> toPageResponse(Page<Notification> notifications) {
        return PageResponse.<NotificationResponse>builder()
                .content(
                        notifications.getContent()
                                .stream()
                                .map(notificationMapper::toResponse)
                                .toList()
                )
                .page(notifications.getNumber())
                .size(notifications.getSize())
                .totalElements(notifications.getTotalElements())
                .totalPages(notifications.getTotalPages())
                .first(notifications.isFirst())
                .last(notifications.isLast())
                .hasNext(notifications.hasNext())
                .hasPrevious(notifications.hasPrevious())
                .build();
    }
}
