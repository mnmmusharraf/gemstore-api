package com.gemstore.backend.dtos.listing.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lightweight DTO for listing cards (grid/list view)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingCardResponse {

    private Long id;
    private String listingNumber;
    private String title;

    // Primary image only
    private String imageUrl;

    // Key specs
    private String gemstoneType;
    private BigDecimal caratWeight;
    private String color;
    private String origin;

    // Price
    private BigDecimal price;
    private String currency;
    private BigDecimal pricePerCarat;

    // Stats
    private Integer viewsCount;
    private Integer favoritesCount;

    // Status
    private String status;
    private Boolean isSold;

    // Seller
    private Long sellerId;
    private String sellerName;
    private String sellerAvatar;

    // Timestamps
    private LocalDateTime createdAt;

    // User context (for logged-in users)
    private Boolean isFavorited;
}