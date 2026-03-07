package com.gemstore.backend.controllers.admin;

import com.gemstore.backend.mappers.user.UserMapper;
import com.gemstore.backend.services.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;
    private final UserMapper userMapper;

    /**
     * Update a user's status (ACTIVE, LOCKED, SUSPENDED, BANNED, DELETED).
     */
    @PatchMapping("/{id}/status/{status}")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable Long id,
            @PathVariable String status
    ) {
        var updated = userService.updateStatus(id, status);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "User status updated to " + status.toUpperCase(),
                "data", userMapper.toUserResponse(updated)
        ));
    }

    /**
     * Promote or demote a user's role.
     */
    @PatchMapping("/{id}/role/{role}")
    public ResponseEntity<Map<String, Object>> updateRole(
            @PathVariable Long id,
            @PathVariable String role
    ) {
        var updated = userService.updateRole(id, role);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "User role updated to " + role.toUpperCase(),
                "data", userMapper.toUserResponse(updated)
        ));
    }
}