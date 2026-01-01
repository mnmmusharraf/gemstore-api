package com.gemstore.backend.controllers.listing;

import com.gemstore.backend.security.CustomUserDetails;
import com.gemstore.backend.services.listing.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/likes")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    /**
     * Toggle like on a listing
     */
    @PostMapping("/{listingId}/toggle")
    public ResponseEntity<Map<String, Object>> toggleLike(
            @PathVariable Long listingId,
            @AuthenticationPrincipal CustomUserDetails user) {

        if (user == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "You must be logged in to like listings"
            ));
        }

        boolean isLiked = likeService.toggleLike(listingId, user.getId());
        long likesCount = likeService.getLikesCount(listingId);

        return ResponseEntity.ok(Map.of(
                "isLiked", isLiked,
                "likesCount", likesCount
        ));
    }

    /**
     * Check if user liked a listing
     */
    @GetMapping("/{listingId}/check")
    public ResponseEntity<Map<String, Object>> checkLike(
            @PathVariable Long listingId,
            @AuthenticationPrincipal CustomUserDetails user) {

        Long userId = user != null ? user.getId() : null;
        boolean isLiked = userId != null && likeService.isLiked(listingId, userId);
        long likesCount = likeService. getLikesCount(listingId);

        return ResponseEntity. ok(Map.of(
                "isLiked", isLiked,
                "likesCount", likesCount
        ));
    }

    /**
     * Get likes count (public)
     */
    @GetMapping("/{listingId}/count")
    public ResponseEntity<Long> getLikesCount(@PathVariable Long listingId) {
        return ResponseEntity.ok(likeService.getLikesCount(listingId));
    }
}