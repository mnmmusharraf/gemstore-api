package com.gemstore.backend.services.notification;

import com.gemstore.backend.entities.listing.Listing;
import com.gemstore.backend.entities.notification.Notification;
import com.gemstore.backend.entities.notification.NotificationType;
import com.gemstore.backend.entities.user.User;
import com.gemstore.backend.mappers.notification.NotificationMapper;
import com.gemstore.backend.repositories.notification.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService - Unit Tests")
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationMapper notificationMapper;
    @Mock private NotificationWebSocketService socketService;

    @InjectMocks
    private NotificationService notificationService;

    private User user;
    private User actor;
    private Listing listing;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("owner");

        actor = new User();
        actor.setId(2L);
        actor.setUsername("actor");

        listing = new Listing();
        listing.setId(10L);
        listing.setTitle("Ruby");
    }

    @Nested
    @DisplayName("createNotification()")
    class CreateNotification {

        @Test
        @DisplayName("TC-NOTIF-001: Should create notification successfully")
        void shouldCreateNotification() {
            when(notificationRepository.existsByUserIdAndActorIdAndTypeAndListingId(
                    1L, 2L, NotificationType.LIKE, 10L)).thenReturn(false);

            Notification savedNotif = Notification.builder()
                    .id(1L).user(user).actor(actor)
                    .type(NotificationType.LIKE).listing(listing).isRead(false).build();
            when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotif);

            Notification result = notificationService.createNotification(
                    user, actor, NotificationType.LIKE, listing, null);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(notificationRepository).save(any(Notification.class));
            verify(socketService).sendToUser(savedNotif);
        }

        @Test
        @DisplayName("TC-NOTIF-002: Should return null for self-notification")
        void shouldReturnNullForSelf() {
            Notification result = notificationService.createNotification(
                    user, user, NotificationType.LIKE, listing, null);

            assertThat(result).isNull();
            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-NOTIF-003: Should return null for duplicate notification")
        void shouldReturnNullForDuplicate() {
            when(notificationRepository.existsByUserIdAndActorIdAndTypeAndListingId(
                    1L, 2L, NotificationType.LIKE, 10L)).thenReturn(true);

            Notification result = notificationService.createNotification(
                    user, actor, NotificationType.LIKE, listing, null);

            assertThat(result).isNull();
            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-NOTIF-004: Should check without listing when listing is null")
        void shouldCheckWithoutListing() {
            when(notificationRepository.existsByUserIdAndActorIdAndType(
                    1L, 2L, NotificationType.FOLLOW)).thenReturn(false);

            Notification savedNotif = Notification.builder()
                    .id(2L).user(user).actor(actor)
                    .type(NotificationType.FOLLOW).isRead(false).build();
            when(notificationRepository.save(any())).thenReturn(savedNotif);

            Notification result = notificationService.createNotification(
                    user, actor, NotificationType.FOLLOW, null, null);

            assertThat(result).isNotNull();
            verify(notificationRepository).existsByUserIdAndActorIdAndType(1L, 2L, NotificationType.FOLLOW);
        }
    }

    @Nested
    @DisplayName("notifyLike()")
    class NotifyLike {

        @Test
        @DisplayName("TC-NOTIF-005: Should create LIKE notification")
        void shouldNotifyLike() {
            when(notificationRepository.existsByUserIdAndActorIdAndTypeAndListingId(
                    1L, 2L, NotificationType.LIKE, 10L)).thenReturn(false);
            when(notificationRepository.save(any())).thenReturn(
                    Notification.builder().id(1L).user(user).actor(actor)
                            .type(NotificationType.LIKE).listing(listing).isRead(false).build());

            notificationService.notifyLike(user, actor, listing);

            verify(notificationRepository).save(argThat(n ->
                    n.getType() == NotificationType.LIKE));
        }
    }

    @Nested
    @DisplayName("notifyFollow()")
    class NotifyFollow {

        @Test
        @DisplayName("TC-NOTIF-006: Should create FOLLOW notification")
        void shouldNotifyFollow() {
            when(notificationRepository.existsByUserIdAndActorIdAndType(
                    1L, 2L, NotificationType.FOLLOW)).thenReturn(false);
            when(notificationRepository.save(any())).thenReturn(
                    Notification.builder().id(1L).user(user).actor(actor)
                            .type(NotificationType.FOLLOW).isRead(false).build());

            notificationService.notifyFollow(user, actor);

            verify(notificationRepository).save(argThat(n ->
                    n.getType() == NotificationType.FOLLOW));
        }
    }

    @Nested
    @DisplayName("getUnreadCount()")
    class GetUnreadCount {

        @Test
        @DisplayName("TC-NOTIF-007: Should return unread count")
        void shouldReturnUnreadCount() {
            when(notificationRepository.countByUserIdAndIsReadFalse(1L)).thenReturn(7L);

            assertThat(notificationService.getUnreadCount(1L)).isEqualTo(7L);
        }
    }

    @Nested
    @DisplayName("markAsRead()")
    class MarkAsRead {

        @Test
        @DisplayName("TC-NOTIF-008: Should mark single notification as read")
        void shouldMarkAsRead() {
            notificationService.markAsRead(1L, 1L);

            verify(notificationRepository).markAsRead(1L, 1L);
        }
    }

    @Nested
    @DisplayName("markAllAsRead()")
    class MarkAllAsRead {

        @Test
        @DisplayName("TC-NOTIF-009: Should mark all as read")
        void shouldMarkAllAsRead() {
            notificationService.markAllAsRead(1L);

            verify(notificationRepository).markAllAsRead(1L);
        }
    }
}