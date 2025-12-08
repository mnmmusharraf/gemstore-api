package com.gemstore.backend.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.time.Instant;

/**
 * User entity representing both local and social-auth identities.
 * DB indexes (partial ones for email/username case-insensitive) are created via Liquibase, not annotations.
 */
@Entity
@Table(
        name = "users_table",
        uniqueConstraints = {
                // Enforces uniqueness ONLY when provider_id is not null at DB level via index; this is a fallback.
                @UniqueConstraint(name = "uq_provider_provider_id", columnNames = {"provider", "provider_id"})
        },
        indexes = {
                @Index(name = "idx_users_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"passwordHash"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // Display name (what you show in UI)
    @Column(name = "display_name", length = 150)
    private String displayName;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(length = 200)
    private String email; // May be null for some OAuth (e.g., private GitHub email)

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(length = 200)
    private String username; // Public handle (nullable if you don't require one)

    /**
     * BCrypt/Argon2 hash. Never store plaintext.
     * Null for pure OAuth-only accounts (unless they later set a local password).
     */
    @JsonIgnore
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(length = 30, nullable = false)
    private String provider = "LOCAL"; // LOCAL, GOOGLE, GITHUB, etc.

    @Column(name = "provider_id", length = 255)
    private String providerId; // External unique user id from provider

    @Column(length = 50, nullable = false)
    private String role = "USER"; // Consider a separate role table for multi-role systems

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(length = 30, nullable = false)
    private String status = "ACTIVE"; // ACTIVE, LOCKED, DISABLED, DELETED (soft)

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt; // Soft deletion timestamp

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled = false;

    @Column(name = "timezone", length = 64)
    private String timezone;

    @Column(length = 16)
    private String locale;

    // ---------- Lifecycle Hooks (if not using Spring Data JPA auditing) ----------

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.displayName == null) {
            // Fallback logic
            if (firstName != null || lastName != null) {
                this.displayName = ((firstName != null) ? firstName : "") +
                        ((lastName != null) ? " " + lastName : "");
                this.displayName = this.displayName.trim().isEmpty() ? null : this.displayName.trim();
            }
            if (this.displayName == null && username != null) {
                this.displayName = username;
            }
            if (this.displayName == null && email != null) {
                this.displayName = email.split("@")[0];
            }
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // ---------- Convenience Methods ----------

    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(Instant.now());
    }

    public boolean isSoftDeleted() {
        return deletedAt != null;
    }

    public void registerFailedLogin(int maxAttempts, long lockSeconds) {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= maxAttempts) {
            this.lockedUntil = Instant.now().plusSeconds(lockSeconds);
            this.failedLoginAttempts = 0; // reset after locking
            this.status = "LOCKED";
        }
    }

    public void resetLoginFailures() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        if ("LOCKED".equals(this.status)) {
            this.status = "ACTIVE";
        }
    }

    public void markPasswordChanged() {
        this.passwordChangedAt = Instant.now();
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
        this.status = "DELETED";
    }
}
