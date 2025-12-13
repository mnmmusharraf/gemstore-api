package com.gemstore.backend.dtos.listing.response;

import com.gemstore.backend.dtos.listing.lookup.LookupDTO;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java. util.List;

/**
 * Full listing details (for single listing page)
 * Includes related listings and price history
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingDetailResponse {

    private Long id;
    private String listingNumber;

    // Basic Info
    private String title;
    private String description;

    // Gemstone Specs
    private LookupDTO gemstoneType;
    private BigDecimal caratWeight;
    private LookupDTO color;
    private LookupDTO colorQuality;
    private LookupDTO clarity;
    private LookupDTO cut;
    private LookupDTO origin;
    private LookupDTO treatment;

    // Dimensions
    private BigDecimal lengthMm;
    private BigDecimal widthMm;
    private BigDecimal depthMm;

    // Pricing
    private BigDecimal price;
    private String currency;
    private BigDecimal pricePerCarat;

    // Certificate
    private Boolean isCertified;
    private String certificateInfo;

    // Status
    private String status;
    private Integer completenessScore;

    // Engagement
    private Integer viewsCount;
    private Integer favoritesCount;

    // Sale
    private Boolean isSold;
    private BigDecimal soldPrice;
    private LocalDateTime soldAt;
    private Integer daysToSell;

    // Images
    private List<ListingImageResponse> images;

    // Seller
    private SellerInfo seller;

    // Price History
    private List<PriceHistoryItem> priceHistory;

    // Related Listings
    private List<ListingCardResponse> relatedListings;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // User context
    private Boolean isFavorited;
    private Boolean isOwner;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SellerInfo {
        private Long id;
        private String displayName;
        private String username;
        private String avatarUrl;
        private Integer totalListings;
        private Integer soldListings;
        private LocalDateTime memberSince;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PriceHistoryItem {
        private BigDecimal oldPrice;
        private BigDecimal newPrice;
        private String changeReason;
        private LocalDateTime changedAt;
    }
}