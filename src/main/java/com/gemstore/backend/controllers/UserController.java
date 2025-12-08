package com.gemstore.backend.controllers;


import com.gemstore.backend.dtos.UpdateProfileRequest;
import com.gemstore.backend.dtos.UserResponse;
import com.gemstore.backend.entities.User;
import com.gemstore.backend.mappers.UserMapper;
import com.gemstore.backend.security.UserPrincipal;
import com.gemstore.backend.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * User resource operations.
 * Distinguish between:
 *   - /api/users/me  (self operations)
 *   - /api/users/{id} (admin / general lookup)
 *
 * Adjust security rules accordingly.
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

    /**
     * Partially update current user's profile (display name, names, avatar, locale, etc.).
     */
    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request,
                                                      Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User updated = userService.updateProfile(principal.getId(), request);
        return ResponseEntity.ok(userMapper.toUserResponse(updated));
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

    /**
     * Hard delete a user by ID (admin-only action).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        userService.deleteHard(id);
        return ResponseEntity.noContent().build();
    }
}
