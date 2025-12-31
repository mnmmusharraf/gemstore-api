package com.gemstore.backend.controllers.listing;

import com.gemstore.backend.dtos.common.ApiResponse;
import com.gemstore.backend.dtos.common.PageResponse;
import com.gemstore.backend.dtos.listing.request.*;
import com.gemstore.backend.dtos.listing.response.*;
import com.gemstore.backend.security.CustomUserDetails;
import com.gemstore.backend.services.listing.ListingImageService;
import com.gemstore.backend.services.listing.ListingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;
    private final ListingImageService listingImageService;

    /* ===================== Create ===================== */

    @PostMapping
    public ResponseEntity<ApiResponse<ListingResponse>> createListing(
            @Valid @RequestBody CreateListingRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {

        ListingResponse listing = listingService.createListing(request, user.getId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Listing created successfully", listing));
    }

    /* ===================== Read ===================== */

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ListingResponse>> getListingById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {

        Long userId = user != null ? user.getId() : null;
        ListingResponse listing = listingService.getListingById(id, userId);

        return ResponseEntity.ok(ApiResponse.success(listing));
    }

    @GetMapping("/{id}/detail")
    public ResponseEntity<ApiResponse<ListingDetailResponse>> getListingDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {

        Long userId = user != null ? user.getId() : null;
        ListingDetailResponse listing = listingService.getListingDetail(id, userId);

        return ResponseEntity.ok(ApiResponse.success(listing));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ListingCardResponse>>> getActiveListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails user) {

        Long userId = user != null ? user.getId() : null;
        PageResponse<ListingCardResponse> listings =
                listingService.getActiveListings(page, size, userId);

        return ResponseEntity.ok(ApiResponse.success(listings));
    }

    /* ===================== Search ===================== */

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
            @AuthenticationPrincipal CustomUserDetails user) {

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

        Long userId = user != null ? user.getId() : null;
        PageResponse<ListingCardResponse> listings =
                listingService.searchListings(request, userId);

        return ResponseEntity.ok(ApiResponse.success(listings));
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<ListingCardResponse>>> searchListingsPost(
            @RequestBody ListingSearchRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {

        Long userId = user != null ? user.getId() : null;
        PageResponse<ListingCardResponse> listings =
                listingService.searchListings(request, userId);

        return ResponseEntity.ok(ApiResponse.success(listings));
    }

    /* ===================== Seller / My Listings ===================== */

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<ApiResponse<PageResponse<ListingCardResponse>>> getListingsBySeller(
            @PathVariable Long sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails user) {

        Long userId = user != null ? user.getId() : null;
        PageResponse<ListingCardResponse> listings =
                listingService.getListingsBySeller(sellerId, page, size, userId);

        return ResponseEntity.ok(ApiResponse.success(listings));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PageResponse<ListingCardResponse>>> getMyListings(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails user) {

        PageResponse<ListingCardResponse> listings =
                listingService.getMyListings(user.getId(), status, page, size);

        return ResponseEntity.ok(ApiResponse.success(listings));
    }

    /* ===================== Update ===================== */

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ListingResponse>> updateListing(
            @PathVariable Long id,
            @Valid @RequestBody UpdateListingRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {

        ListingResponse listing =
                listingService.updateListing(id, request, user.getId());

        return ResponseEntity.ok(ApiResponse.success("Listing updated successfully", listing));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ListingResponse>> patchListing(
            @PathVariable Long id,
            @RequestBody UpdateListingRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {

        ListingResponse listing =
                listingService.updateListing(id, request, user.getId());

        return ResponseEntity.ok(ApiResponse.success("Listing updated successfully", listing));
    }

    /* ===================== Status ===================== */

    @PostMapping("/{id}/sold")
    public ResponseEntity<ApiResponse<ListingResponse>> markAsSold(
            @PathVariable Long id,
            @RequestParam(required = false) BigDecimal soldPrice,
            @AuthenticationPrincipal CustomUserDetails user) {

        ListingResponse listing =
                listingService.markAsSold(id, soldPrice, user.getId());

        return ResponseEntity.ok(ApiResponse.success("Listing marked as sold", listing));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<Void>> archiveListing(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {

        listingService.archiveListing(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Listing archived", null));
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<ApiResponse<ListingResponse>> reactivateListing(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {

        ListingResponse listing =
                listingService.reactivateListing(id, user.getId());

        return ResponseEntity.ok(ApiResponse.success("Listing reactivated", listing));
    }

    /* ===================== Delete ===================== */

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteListing(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {

        listingService.deleteListing(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Listing deleted", null));
    }

    /* ===================== Image Upload ===================== */

    @PostMapping("/upload")
    public ResponseEntity<?> uploadListingImage(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails user
    ) {

        if (user == null) {
            log.warn("[UPLOAD] user is NULL (unauthenticated request)");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required"));
        }

        log.info("[UPLOAD] Authenticated user: id={}, username={}, roles={}",
                user.getId(),
                user.getUsername(),
                user.getAuthorities());

        String imageUrl = listingImageService.uploadListingImage(user.getId(), file);
        return ResponseEntity.ok(Map.of("url", imageUrl));
    }

}
