package com.gemstore. backend.services;

import com. gemstore.backend.dtos. ChangePasswordRequest;
import com. gemstore.backend.dtos. UpdateProfileRequest;
import com. gemstore.backend.entities.User;
import com.gemstore. backend.exceptions.UserNotFoundException;
import com.gemstore.backend. mappers.UserMapper;
import com.gemstore.backend.repositories.UserRepository;
import jakarta. validation.Valid;
import lombok. RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService; // <-- ADD THIS

    /**
     * Returns all users, excluding soft-deleted ones.
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

    /**
     * Find user by username.
     */
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));
    }

    /**
     * Find user by email.
     */
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }

    @Transactional
    public User updateProfile(Long id, UpdateProfileRequest req) {
        User user = getById(id);
        userMapper.updateUserFromProfile(req, user); // partial update mapping
        return userRepository.save(user);
    }

    /**
     * Upload and update user's avatar.
     */
    @Transactional
    public String uploadAvatar(Long userId, MultipartFile file) {
        User user = getById(userId);

        // Validate file
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image");
        }

        // Validate file size (max 5MB)
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File size must be less than 5MB");
        }

        // Upload to storage (S3, local, etc.)
        String avatarUrl = fileStorageService.uploadFile(file, "avatars/" + userId);

        // Update user
        user.setAvatarUrl(avatarUrl);
        userRepository. save(user);

        return avatarUrl;
    }

    /**
     * Delete user's avatar.
     */
    @Transactional
    public void deleteAvatar(Long userId) {
        User user = getById(userId);

        if (user.getAvatarUrl() != null) {
            // Optionally delete from storage
            // fileStorageService.deleteFile(user.getAvatarUrl());
            user.setAvatarUrl(null);
            userRepository.save(user);
        }
    }

    @Transactional
    public void softDelete(Long id) {
        User user = getById(id);
        user.softDelete();
        userRepository.save(user);
    }

    @Transactional
    public void deleteHard(Long id) {
        if (!userRepository. existsById(id)) {
            throw new UserNotFoundException(id);
        }
        userRepository. deleteById(id);
    }

    @Transactional
    public User updateStatus(Long id, String status) {
        User user = getById(id);
        user.setStatus(status.toUpperCase());
        return userRepository.save(user);
    }

    /**
     * Change the authenticated user's password.
     */
    @Transactional
    public void changePassword(Long id, @Valid ChangePasswordRequest request) {
        User user = getById(id);

        String currentPassword = request.getCurrentPassword();
        String newPassword = request.getNewPassword();

        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("New password must not be blank");
        }

        // If user already has a local password, verify current password
        String existingHash = user.getPasswordHash();
        if (existingHash != null && !existingHash. isBlank()) {
            if (currentPassword == null || currentPassword.isBlank()) {
                throw new IllegalArgumentException("Current password is required");
            }
            if (!passwordEncoder.matches(currentPassword, existingHash)) {
                throw new IllegalArgumentException("Current password is incorrect");
            }
            // Prevent setting the same password again
            if (passwordEncoder. matches(newPassword, existingHash)) {
                throw new IllegalArgumentException("New password must be different from the current password");
            }
        }

        // Set/replace password hash
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.markPasswordChanged();
        user.resetLoginFailures();

        userRepository.save(user);
    }

    @Transactional
    public User updateRole(Long id, String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Role must not be blank");
        }

        String normalized = role.trim().toUpperCase();
        if (normalized. startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }

        Set<String> allowed = Set.of("USER", "ADMIN");
        if (!allowed.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported role: " + role + ". Allowed: " + allowed);
        }

        User user = getById(id);
        if (user.isSoftDeleted()) {
            throw new IllegalStateException("Cannot change role for a deleted account");
        }

        if (normalized.equals(user. getRole())) {
            return user;
        }

        user.setRole(normalized);
        return userRepository.save(user);
    }
}