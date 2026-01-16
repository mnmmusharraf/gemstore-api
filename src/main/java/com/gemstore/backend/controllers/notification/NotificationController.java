package com.gemstore.backend.controllers.notification;

import com.gemstore.backend.dtos.common.ApiResponse;
import com.gemstore.backend.dtos.common.PageResponse;
import com.gemstore.backend.dtos.notification.NotificationResponse;
import com.gemstore.backend.security.CustomUserDetails;
import com.gemstore.backend.services.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /* ========================= Read Operations ========================= */

    /**
     * Get notifications (paginated)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        PageResponse<NotificationResponse> notifications =
                notificationService.getNotifications(principal.getId(), page, size);

        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    /**
     * Get unread notification count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        long count = notificationService.getUnreadCount(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(Map.of("count", count)));
    }

    /* ========================= Update Operations ========================= */

    /**
     * Mark single notification as read
     */
    @PostMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        notificationService.markAsRead(notificationId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }

    /**
     * Mark all notifications as read
     */
    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        notificationService.markAllAsRead(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }
}
