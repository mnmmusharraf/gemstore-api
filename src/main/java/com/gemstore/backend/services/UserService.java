package com.gemstore.backend.services;


import com.gemstore.backend.dtos.ChangePasswordRequest;
import com.gemstore.backend.dtos.UpdateProfileRequest;
import com.gemstore.backend.entities.User;
import com.gemstore.backend.exceptions.UserNotFoundException;
import com.gemstore.backend.mappers.UserMapper;
import com.gemstore.backend.repositories.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * Returns all users, excluding soft-deleted ones.
     * Controller will map these to UserResponse via UserMapper.
     */
    public List<User> findAll() {
        return userRepository.findAll()
                .stream()
                .filter(u -> u.getDeletedAt() == null)
                .toList();
    }

    /**
     * If you still need the raw list (including soft-deleted), keep this.
     */
    public List<User> findAllRaw() {
        return userRepository.findAll();
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    public User updateProfile(Long id, UpdateProfileRequest req) {
        User user = getById(id);
        userMapper.updateUserFromProfile(req, user); // partial update mapping
        return userRepository.save(user);
    }

    public void softDelete(Long id) {
        User user = getById(id);
        user.softDelete();
        userRepository.save(user);
    }

    public void deleteHard(Long id) {
        if (!userRepository.existsById(id)) throw new UserNotFoundException(id);
        userRepository.deleteById(id);
    }

    public User updateStatus(Long id, String status) {
        User user = getById(id);
        user.setStatus(status.toUpperCase());
        return userRepository.save(user);
    }

    /**
     * Change the authenticated user's password.
     * Rules:
     * - If the user already has a local password, currentPassword must match.
     * - New password must differ from the existing one.
     * - Password is stored as a hash (never plaintext).
     * - Resets login failures and updates passwordChangedAt for JWT invalidation strategies.
     */
    public void changePassword(Long id, @Valid ChangePasswordRequest request) {
        User user = getById(id);

        String currentPassword = request.getCurrentPassword();
        String newPassword = request.getNewPassword();

        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("New password must not be blank");
        }

        // If user already has a local password, verify current password
        String existingHash = user.getPasswordHash();
        if (existingHash != null && !existingHash.isBlank()) {
            if (currentPassword == null || currentPassword.isBlank()) {
                throw new IllegalArgumentException("Current password is required");
            }
            if (!passwordEncoder.matches(currentPassword, existingHash)) {
                throw new IllegalArgumentException("Current password is incorrect");
            }
            // Prevent setting the same password again
            if (passwordEncoder.matches(newPassword, existingHash)) {
                throw new IllegalArgumentException("New password must be different from the current password");
            }
        }

        // Set/replace password hash
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.markPasswordChanged();   // updates passwordChangedAt
        user.resetLoginFailures();    // clear lock/failures if any

        userRepository.save(user);
    }

    public User updateRole(Long id, String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Role must not be blank");
        }

        // Normalize: accept "admin", "ADMIN", "ROLE_ADMIN"
        String normalized = role.trim().toUpperCase();
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }

        // Whitelist allowed roles. Extend as needed (e.g., MODERATOR).
        java.util.Set<String> allowed = java.util.Set.of("USER", "ADMIN");
        if (!allowed.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported role: " + role + ". Allowed: " + allowed);
        }

        User user = getById(id);
        if (user.isSoftDeleted()) {
            throw new IllegalStateException("Cannot change role for a deleted account");
        }

        if (normalized.equals(user.getRole())) {
            return user; // no change
        }

        user.setRole(normalized);
        return userRepository.save(user);
    }
}
