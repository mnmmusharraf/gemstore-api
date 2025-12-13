package com.gemstore.backend.controllers.listing;

import com.gemstore.backend.dtos.common.ApiResponse;
import com. gemstore.backend.dtos. common.PageResponse;
import com.gemstore. backend.dtos.listing.request.*;
import com.gemstore.backend.dtos.listing.response.*;
import com.gemstore.backend.services.listing.ListingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework. http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security. core.userdetails.UserDetails;
import org.springframework. web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * ListingController handles all listing-related endpoints.
 */
@RestController
@RequestMapping("/api/v1/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;

    /* ===================== Create ===================== */

    /**
     * Create a new listing.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ListingResponse>> createListing(
            @Valid @RequestBody CreateListingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = extractUserId(userDetails);
        ListingResponse listing = listingService. createListing(request, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Listing created successfully", listing));
    }

    /* ===================== Read ===================== */

    /**
     * Get listing by ID (basic response).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ListingResponse>> getListingById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = extractUserIdOrNull(userDetails);
        ListingResponse listing = listingService.getListingById(id, userId);

        return ResponseEntity. ok(ApiResponse.success(listing));
    }

    /**
     * Get listing detail (full response with price history & related).
     */
    @GetMapping("/{id}/detail")
    public ResponseEntity<ApiResponse<ListingDetailResponse>> getListingDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = extractUserIdOrNull(userDetails);
        ListingDetailResponse listing = listingService.getListingDetail(id, userId);

        return ResponseEntity. ok(ApiResponse.success(listing));
    }

    /**
     * Get all active listings (paginated).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ListingCardResponse>>> getActiveListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = extractUserIdOrNull(userDetails);
        PageResponse<ListingCardResponse> listings = listingService.getActiveListings(page, size, userId);

        return ResponseEntity.ok(ApiResponse.success(listings));
    }

    /**
     * Search listings with filters.
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<ListingCardResponse>>> searchListings(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer gemstoneTypeId,
            @RequestParam(required = false) Integer colorId,
            @RequestParam(required = false) Integer colorQualityId,
            @RequestParam(required = false) Integer clarityId,
            @RequestParam(required = false) Integer cutId,
            @RequestParam(required = false) Integer originId,
            @RequestParam(required = false) Integer treatmentId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) BigDecimal minCarat,
            @RequestParam(required = false) BigDecimal maxCarat,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        ListingSearchRequest request = ListingSearchRequest.builder()
                .query(query)
                .gemstoneTypeId(gemstoneTypeId)
                .colorId(colorId)
                .colorQualityId(colorQualityId)
                .clarityId(clarityId)
                .cutId(cutId)
                .originId(originId)
                .treatmentId(treatmentId)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .minCarat(minCarat)
                .maxCarat(maxCarat)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .page(page)
                .size(size)
                .build();

        Long userId = extractUserIdOrNull(userDetails);
        PageResponse<ListingCardResponse> listings = listingService.searchListings(request, userId);

        return ResponseEntity.ok(ApiResponse.success(listings));
    }

    /**
     * Search listings with POST (for complex filters).
     */
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<ListingCardResponse>>> searchListingsPost(
            @RequestBody ListingSearchRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = extractUserIdOrNull(userDetails);
        PageResponse<ListingCardResponse> listings = listingService.searchListings(request, userId);

        return ResponseEntity.ok(ApiResponse.success(listings));
    }

    /**
     * Get listings by seller. 
     */
    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<ApiResponse<PageResponse<ListingCardResponse>>> getListingsBySeller(
            @PathVariable Long sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = extractUserIdOrNull(userDetails);
        PageResponse<ListingCardResponse> listings = listingService.getListingsBySeller(sellerId, page, size, userId);

        return ResponseEntity.ok(ApiResponse.success(listings));
    }

    /**
     * Get current user's listings.
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PageResponse<ListingCardResponse>>> getMyListings(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = extractUserId(userDetails);
        PageResponse<ListingCardResponse> listings = listingService.getMyListings(userId, status, page, size);

        return ResponseEntity.ok(ApiResponse. success(listings));
    }

    /* ===================== Update ===================== */

    /**
     * Update a listing.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ListingResponse>> updateListing(
            @PathVariable Long id,
            @Valid @RequestBody UpdateListingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = extractUserId(userDetails);
        ListingResponse listing = listingService.updateListing(id, request, userId);

        return ResponseEntity. ok(ApiResponse.success("Listing updated successfully", listing));
    }

    /**
     * Partial update (PATCH).
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ListingResponse>> patchListing(
            @PathVariable Long id,
            @RequestBody UpdateListingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = extractUserId(userDetails);
        ListingResponse listing = listingService.updateListing(id, request, userId);

        return ResponseEntity.ok(ApiResponse.success("Listing updated successfully", listing));
    }

    /* ===================== Status Management ===================== */

    /**
     * Mark listing as sold.
     */
    @PostMapping("/{id}/sold")
    public ResponseEntity<ApiResponse<ListingResponse>> markAsSold(
            @PathVariable Long id,
            @RequestParam(required = false) BigDecimal soldPrice,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = extractUserId(userDetails);
        ListingResponse listing = listingService. markAsSold(id, soldPrice, userId);

        return ResponseEntity.ok(ApiResponse.success("Listing marked as sold", listing));
    }

    /**
     * Archive listing.
     */
    @PostMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<Void>> archiveListing(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = extractUserId(userDetails);
        listingService. archiveListing(id, userId);

        return ResponseEntity.ok(ApiResponse.success("Listing archived", null));
    }

    /**
     * Reactivate listing.
     */
    @PostMapping("/{id}/reactivate")
    public ResponseEntity<ApiResponse<ListingResponse>> reactivateListing(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = extractUserId(userDetails);
        ListingResponse listing = listingService.reactivateListing(id, userId);

        return ResponseEntity.ok(ApiResponse.success("Listing reactivated", listing));
    }

    /* ===================== Delete ===================== */

    /**
     * Delete listing.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteListing(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = extractUserId(userDetails);
        listingService.deleteListing(id, userId);

        return ResponseEntity. ok(ApiResponse.success("Listing deleted", null));
    }

    /* ===================== Helper Methods ===================== */

    private Long extractUserId(UserDetails userDetails) {
        if (userDetails == null) {
            throw new com.gemstore.backend.exceptions. UnauthorizedException("Authentication required");
        }
        // Assuming your UserDetails implementation has getId() method
        // Adjust based on your actual implementation
        if (userDetails instanceof com.gemstore.backend.security.CustomUserDetails customUserDetails) {
            return customUserDetails.getId();
        }
        throw new IllegalStateException("Unable to extract user ID");
    }

    private Long extractUserIdOrNull(UserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }
        try {
            return extractUserId(userDetails);
        } catch (Exception e) {
            return null;
        }
    }
}