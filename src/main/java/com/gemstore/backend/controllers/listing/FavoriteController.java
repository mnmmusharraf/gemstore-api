// controllers/listing/FavoriteController.java
package com.gemstore.backend. controllers.listing;

import com. gemstore.backend.dtos. common.ApiResponse;
import com.gemstore.backend.dtos.common.PageResponse;
import com. gemstore.backend.dtos. listing.response.ListingCardResponse;
import com.gemstore.backend.services.listing.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework. security.core.userdetails. UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * FavoriteController handles user favorites/wishlist endpoints.
 */
@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    /* ===================== Get Favorites ===================== */

    /**
     * Get current user's favorites.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ListingCardResponse>>> getMyFavorites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = extractUserId(userDetails);
        PageResponse<ListingCardResponse> favorites = favoriteService.getUserFavorites(userId, page, size);

        return ResponseEntity. ok(ApiResponse.success(favorites));
    }

    /**
     * Get favorites count.
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getFavoritesCount(
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = extractUserId(userDetails);
        long count = favoriteService.getFavoritesCount(userId);

        return ResponseEntity.ok(ApiResponse.success(count));
    }

    /* ===================== Add/Remove Favorites ===================== */

    /**
     * Add listing to favorites.
     */
    @PostMapping("/{listingId}")
    public ResponseEntity<ApiResponse<Void>> addFavorite(
            @PathVariable Long listingId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = extractUserId(userDetails);
        favoriteService.addFavorite(userId, listingId);

        return ResponseEntity.ok(ApiResponse. success("Added to favorites", null));
    }

    /**
     * Remove listing from favorites.
     */
    @DeleteMapping("/{listingId}")
    public ResponseEntity<ApiResponse<Void>> removeFavorite(
            @PathVariable Long listingId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = extractUserId(userDetails);
        favoriteService.removeFavorite(userId, listingId);

        return ResponseEntity.ok(ApiResponse.success("Removed from favorites", null));
    }

    /**
     * Toggle favorite status.
     */
    @PostMapping("/{listingId}/toggle")
    public ResponseEntity<ApiResponse<ToggleResponse>> toggleFavorite(
            @PathVariable Long listingId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = extractUserId(userDetails);
        boolean isFavorited = favoriteService.toggleFavorite(userId, listingId);

        ToggleResponse response = new ToggleResponse(isFavorited);
        String message = isFavorited ? "Added to favorites" : "Removed from favorites";

        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    /**
     * Check if listing is favorited.
     */
    @GetMapping("/{listingId}/check")
    public ResponseEntity<ApiResponse<ToggleResponse>> checkFavorite(
            @PathVariable Long listingId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = extractUserId(userDetails);
        boolean isFavorited = favoriteService.isFavorited(userId, listingId);

        return ResponseEntity.ok(ApiResponse. success(new ToggleResponse(isFavorited)));
    }

    /* ===================== Helper ===================== */

    private Long extractUserId(UserDetails userDetails) {
        if (userDetails == null) {
            throw new com.gemstore.backend.exceptions. UnauthorizedException("Authentication required");
        }
        if (userDetails instanceof com.gemstore.backend.security.CustomUserDetails customUserDetails) {
            return customUserDetails.getId();
        }
        throw new IllegalStateException("Unable to extract user ID");
    }

    /* ===================== Response DTO ===================== */

    public record ToggleResponse(boolean isFavorited) {}
}