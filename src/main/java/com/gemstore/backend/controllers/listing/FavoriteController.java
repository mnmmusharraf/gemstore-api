// controllers/listing/FavoriteController.java
package com.gemstore.backend.controllers.listing;

import com.gemstore.backend.dtos.common.ApiResponse;
import com.gemstore.backend.dtos.common.PageResponse;
import com.gemstore.backend.dtos.listing.response.ListingCardResponse;
import com.gemstore.backend.security.CustomUserDetails;
import com.gemstore.backend.services.listing.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        PageResponse<ListingCardResponse> favorites =
                favoriteService.getUserFavorites(principal.getId(), page, size);

        return ResponseEntity.ok(ApiResponse.success(favorites));
    }

    /**
     * Get favorites count.
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getFavoritesCount(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        long count = favoriteService.getFavoritesCount(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    /* ===================== Add / Remove Favorites ===================== */

    /**
     * Add listing to favorites.
     */
    @PostMapping("/{listingId}")
    public ResponseEntity<ApiResponse<Void>> addFavorite(
            @PathVariable Long listingId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        favoriteService.addFavorite(principal.getId(), listingId);
        return ResponseEntity.ok(ApiResponse.success("Added to favorites", null));
    }

    /**
     * Remove listing from favorites.
     */
    @DeleteMapping("/{listingId}")
    public ResponseEntity<ApiResponse<Void>> removeFavorite(
            @PathVariable Long listingId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        favoriteService.removeFavorite(principal.getId(), listingId);
        return ResponseEntity.ok(ApiResponse.success("Removed from favorites", null));
    }

    /* ===================== Toggle / Check ===================== */

    /**
     * Toggle favorite status.
     */
    @PostMapping("/{listingId}/toggle")
    public ResponseEntity<ApiResponse<ToggleResponse>> toggleFavorite(
            @PathVariable Long listingId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        boolean isFavorited =
                favoriteService.toggleFavorite(principal.getId(), listingId);

        String message = isFavorited
                ? "Added to favorites"
                : "Removed from favorites";

        return ResponseEntity.ok(
                ApiResponse.success(message, new ToggleResponse(isFavorited))
        );
    }

    /**
     * Check if listing is favorited.
     */
    @GetMapping("/{listingId}/check")
    public ResponseEntity<ApiResponse<ToggleResponse>> checkFavorite(
            @PathVariable Long listingId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        boolean isFavorited =
                favoriteService.isFavorited(principal.getId(), listingId);

        return ResponseEntity.ok(
                ApiResponse.success(new ToggleResponse(isFavorited))
        );
    }

    /* ===================== Response DTO ===================== */

    public record ToggleResponse(boolean isFavorited) {}
}
