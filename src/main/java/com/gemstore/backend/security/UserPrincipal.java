package com.gemstore.backend.security;


import com.gemstore.backend.entities.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.*;

/**
 * Bridges your JPA User entity to Spring Security's UserDetails.
 * Reflects dynamic role, lock state, soft deletion, and status.
 */
public class UserPrincipal implements UserDetails, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final User user;
    private final Set<SimpleGrantedAuthority> authorities;

    private UserPrincipal(User user) {
        this.user = user;
        this.authorities = resolveAuthorities(user);
    }

    public static UserPrincipal from(User user) {
        return new UserPrincipal(user);
    }

    private Set<SimpleGrantedAuthority> resolveAuthorities(User user) {
        // If you later support multiple roles (comma-separated), split here.
        String role = Optional.ofNullable(user.getRole()).orElse("USER").trim();
        if (!role.startsWith("ROLE_")) {
            role = "ROLE_" + role;
        }
        return Set.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /**
     * Spring Security will call this. Must return the hashed password.
     */
    @Override
    public String getPassword() {
        return user.getPasswordHash(); // never expose raw password
    }

    /**
     * The principal identifier used for authentication. We use username;
     * if you prefer email-based login, adapt MyUserDetailsService logic accordingly.
     */
    @Override
    public String getUsername() {
        return user.getUsername() != null ? user.getUsername()
                : (user.getEmail() != null ? user.getEmail() : "user-" + user.getId());
    }

    /**
     * You are not tracking explicit account expiry. Always true for now.
     * Add field later (e.g., accountExpiresAt) if needed.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Locked if:
     * - lockedUntil in future OR status == LOCKED
     */
    @Override
    public boolean isAccountNonLocked() {
        if (user.getDeletedAt() != null) return false;
        if ("LOCKED".equalsIgnoreCase(user.getStatus())) return false;
        return user.getLockedUntil() == null || user.getLockedUntil().isBefore(Instant.now());
    }

    /**
     * You can implement credential expiry rules (e.g., password must be changed every N days).
     * Currently always returns true.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        // Example (disabled): if passwordChangedAt older than 180 days -> false
        // if (user.getPasswordChangedAt() != null &&
        //     user.getPasswordChangedAt().isBefore(Instant.now().minus(180, ChronoUnit.DAYS))) return false;
        return true;
    }

    /**
     * Enabled if:
     * - status == ACTIVE
     * - not soft deleted
     */
    @Override
    public boolean isEnabled() {
        if (user.getDeletedAt() != null) return false;
        return "ACTIVE".equalsIgnoreCase(user.getStatus());
    }

    /* ---------- Convenience Accessors (optional) ---------- */

    public Long getId() {
        return user.getId();
    }

    public String getDisplayName() {
        return user.getDisplayName();
    }

    public String getEmail() {
        return user.getEmail();
    }

    public String getProvider() {
        return user.getProvider();
    }

    public boolean isEmailVerified() {
        return user.isEmailVerified();
    }

    public User getUser() {
        return user;
    }

    @Override
    public String toString() {
        return "UserPrincipal{id=" + user.getId() +
                ", username=" + getUsername() +
                ", role=" + user.getRole() +
                ", status=" + user.getStatus() + '}';
    }
}
