package com.gemstore.backend.repositories;



import com.gemstore.backend.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByUsernameIgnoreCase(String username);

    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email) AND u.deletedAt IS NULL")
    Optional<User> findActiveByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.provider = :provider AND u.providerId = :providerId AND u.deletedAt IS NULL")
    Optional<User> findActiveByProviderAndProviderId(String provider, String providerId);
}
