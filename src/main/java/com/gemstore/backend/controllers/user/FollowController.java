package com.gemstore.backend.controllers.user;

import com.gemstore.backend.dtos.common.ApiResponse;
import com.gemstore.backend.dtos.common.PageResponse;
import com.gemstore.backend.dtos.user.UserSummaryResponse;
import com.gemstore.backend.security.CustomUserDetails;
import com.gemstore.backend.services.user.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    /* ===================== Follow / Unfollow ===================== */

    /**
     * Follow a user
     */
    @PostMapping("/{userId}/follow")
    public ResponseEntity<ApiResponse<FollowResponse>> follow(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        var result = followService.follow(principal.getId(), userId);
        String message = result.isPending()
                ? "Follow request sent"
                :  "Successfully followed user";
        return ResponseEntity.ok(ApiResponse.success(message,
                new FollowResponse(result.isFollowing(), result.isPending())));
    }

    /**
     * Unfollow a user
     */
    @DeleteMapping("/{userId}/follow")
    public ResponseEntity<ApiResponse<Void>> unfollow(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        followService.unfollow(principal.getId(), userId);
        return ResponseEntity.ok(ApiResponse.success("Successfully unfollowed user", null));
    }

    /**
     * Toggle follow status
     */
    @PostMapping("/{userId}/follow/toggle")
    public ResponseEntity<ApiResponse<FollowResponse>> toggleFollow(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        var result = followService.toggleFollow(principal.getId(), userId);
        String message;
        if (! result.isFollowing()) {
            message = "Successfully unfollowed user";
        } else if (result.isPending()) {
            message = "Follow request sent";
        } else {
            message = "Successfully followed user";
        }
        return ResponseEntity.ok(ApiResponse.success(message,
                new FollowResponse(result.isFollowing(), result.isPending())));
    }

    /**
     * Check follow status
     */
    @GetMapping("/{userId}/follow/status")
    public ResponseEntity<ApiResponse<FollowStatusResponse>> getFollowStatus(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        String status = followService. getFollowStatus(principal.getId(), userId);
        return ResponseEntity.ok(ApiResponse.success(new FollowStatusResponse(status)));
    }

    /* ===================== Accept/Reject Requests ===================== */

    /**
     * Accept a follow request
     */
    @PostMapping("/follow-requests/{followerId}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptFollowRequest(
            @PathVariable Long followerId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        followService.acceptFollowRequest(principal. getId(), followerId);
        return ResponseEntity.ok(ApiResponse.success("Follow request accepted", null));
    }

    /**
     * Reject a follow request
     */
    @DeleteMapping("/follow-requests/{followerId}")
    public ResponseEntity<ApiResponse<Void>> rejectFollowRequest(
            @PathVariable Long followerId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        followService.rejectFollowRequest(principal.getId(), followerId);
        return ResponseEntity.ok(ApiResponse.success("Follow request rejected", null));
    }

    /**
     * Get pending follow requests
     */
    @GetMapping("/follow-requests")
    public ResponseEntity<ApiResponse<PageResponse<UserSummaryResponse>>> getPendingRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        PageResponse<UserSummaryResponse> requests =
                followService.getPendingRequests(principal.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.success(requests));
    }

    /**
     * Get pending requests count
     */
    @GetMapping("/follow-requests/count")
    public ResponseEntity<ApiResponse<CountResponse>> getPendingRequestsCount(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        long count = followService. getPendingRequestsCount(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(new CountResponse(count)));
    }

    /* ===================== Followers / Following Lists ===================== */

    /**
     * Get followers of a user
     */
    @GetMapping("/{userId}/followers")
    public ResponseEntity<ApiResponse<PageResponse<UserSummaryResponse>>> getFollowers(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        PageResponse<UserSummaryResponse> followers =
                followService. getFollowers(userId, currentUserId, page, size);
        return ResponseEntity. ok(ApiResponse. success(followers));
    }

    /**
     * Get users that a user is following
     */
    @GetMapping("/{userId}/following")
    public ResponseEntity<ApiResponse<PageResponse<UserSummaryResponse>>> getFollowing(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        PageResponse<UserSummaryResponse> following =
                followService.getFollowing(userId, currentUserId, page, size);
        return ResponseEntity.ok(ApiResponse.success(following));
    }

    /* ===================== Response DTOs ===================== */

    public record FollowResponse(boolean isFollowing, boolean isPending) {}
    public record FollowStatusResponse(String status) {} // ACTIVE, PENDING, NONE
    public record CountResponse(long count) {}
}