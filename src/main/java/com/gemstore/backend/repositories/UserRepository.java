package com.gemstore.backend.repositories;

import com.gemstore.backend.entities.User;
import org.springframework.data.jpa.repository. JpaRepository;
import org. springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ========== Case-insensitive lookups (recommended for email/username) ==========

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    // ========== Exact match lookups (if needed) ==========

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    // ========== Provider lookups ==========

    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    // ========== Active user queries (excludes soft-deleted) ==========

    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email) AND u.deletedAt IS NULL")
    Optional<User> findActiveByEmail(String email);

    @Query("SELECT u FROM User u WHERE LOWER(u.username) = LOWER(:username) AND u.deletedAt IS NULL")
    Optional<User> findActiveByUsername(String username);

    @Query("SELECT u FROM User u WHERE u.provider = :provider AND u.providerId = :providerId AND u.deletedAt IS NULL")
    Optional<User> findActiveByProviderAndProviderId(String provider, String providerId);
}