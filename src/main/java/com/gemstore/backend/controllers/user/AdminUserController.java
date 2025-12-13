package com.gemstore.backend.controllers.user;


import com.gemstore.backend.dtos.user.UserResponse;
import com.gemstore.backend.mappers.user.UserMapper;
import com.gemstore.backend.services.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Administrative user management endpoints.
 * Requires method security: @EnableMethodSecurity in configuration.
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;
    private final UserMapper userMapper;

    /**
     * Update a user's status (ACTIVE, LOCKED, DISABLED, DELETED).
     */
    @PatchMapping("/{id}/status/{status}")
    public ResponseEntity<UserResponse> updateStatus(@PathVariable Long id,
                                                     @PathVariable String status) {
        var updated = userService.updateStatus(id, status);
        return ResponseEntity.ok(userMapper.toUserResponse(updated));
    }

    /**
     * Promote or demote a user's role (optional).
     */
    @PatchMapping("/{id}/role/{role}")
    public ResponseEntity<UserResponse> updateRole(@PathVariable Long id,
                                                   @PathVariable String role) {
        var updated = userService.updateRole(id, role);
        return ResponseEntity.ok(userMapper.toUserResponse(updated));
    }
}
