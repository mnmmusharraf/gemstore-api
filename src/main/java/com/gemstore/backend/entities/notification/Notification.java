package com.gemstore.backend.entities.notification;

import com.gemstore.backend.entities.listing.Listing;
import com.gemstore.backend.entities.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_user_id", columnList = "user_id"),
        @Index(name = "idx_notifications_created_at", columnList = "created_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType. IDENTITY)
    private Long id;

    // Who receives the notification
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Who triggered the notification (liked, followed, commented, etc.)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    // Related listing (if applicable)
    @ManyToOne(fetch = FetchType. LAZY)
    @JoinColumn(name = "listing_id")
    private Listing listing;

    @Enumerated(EnumType. STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}