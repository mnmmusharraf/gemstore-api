package com.gemstore.backend.entities.user;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations. CreationTimestamp;

import java.time.Instant;

@Entity
@Table(
        name = "followers",
        uniqueConstraints = @UniqueConstraint(columnNames = {"follower_id", "following_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower;  // The user who is following

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following_id", nullable = false)
    private User following; // The user being followed

    @Column(length = 20)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, PENDING (for private accounts)

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}