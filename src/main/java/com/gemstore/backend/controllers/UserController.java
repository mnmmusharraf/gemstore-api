package com.gemstore.backend.controllers;

import com.gemstore.backend.dtos.user.UpdateProfileRequest;
import com.gemstore.backend.dtos.user.UserResponse;
import com.gemstore.backend.entities.user.User;
import com.gemstore.backend.mappers.user.UserMapper;
import com. gemstore.backend.security.UserPrincipal;
import com.gemstore.backend.services.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * User resource operations.
 * Distinguish between:
 *   - /api/users/me  (self operations)
 *   - /api/users/{id} (admin / general lookup)
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    /**
     * List all users (consider pagination & restricting to admins).
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> listAll() {
        List<UserResponse> users = userService.findAll()
                .stream()
                .map(userMapper::toUserResponse)
                .toList();
        return ResponseEntity.ok(users);
    }

    /**
     * Get a user by ID (admin or self depending on policy).
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        User user = userService.getById(id);
        return ResponseEntity.ok(userMapper.toUserResponse(user));
    }

    // ==================== /me ENDPOINTS ====================

    /**
     * Get current authenticated user's profile.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userService.getById(principal.getId());
        return ResponseEntity.ok(userMapper.toUserResponse(user));
    }

    /**
     * Partially update current user's profile (PATCH).
     */
    @PatchMapping("/me")
    public ResponseEntity<UserResponse> patchProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication
    ) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User updated = userService.updateProfile(principal.getId(), request);
        return ResponseEntity.ok(userMapper. toUserResponse(updated));
    }

    /**
     * Full update current user's profile (PUT).
     * Frontend can use either PATCH or PUT.
     */
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication
    ) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User updated = userService.updateProfile(principal. getId(), request);
        return ResponseEntity.ok(userMapper.toUserResponse(updated));
    }

    /**
     * Upload/update current user's avatar.
     */
    @PostMapping("/me/avatar")
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @RequestParam("avatar") MultipartFile file,
            Authentication authentication
    ) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String avatarUrl = userService.uploadAvatar(principal.getId(), file);
        return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));
    }

    /**
     * Delete current user's avatar.
     */
    @DeleteMapping("/me/avatar")
    public ResponseEntity<Void> deleteAvatar(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        userService.deleteAvatar(principal.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Soft delete (deactivate) the current user's account.
     */
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteSelf(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        userService.softDelete(principal.getId());
        return ResponseEntity.noContent().build();
    }

    // ==================== ADMIN ENDPOINTS ====================

    /**
     * Hard delete a user by ID (admin-only action).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        userService.deleteHard(id);
        return ResponseEntity.noContent().build();
    }
}