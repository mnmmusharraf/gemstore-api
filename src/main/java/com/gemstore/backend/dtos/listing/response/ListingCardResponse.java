package com.gemstore.backend.dtos.listing.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util. List;

/**
 * Lightweight DTO for listing cards (grid/list view)
 * Includes all specs needed for feed display
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingCardResponse {

    private Long id;
    private String listingNumber;
    private String title;

    // Images - ALL images for carousel
    private List<String> imageUrls;
    private String primaryImageUrl;

    // Gemstone specs (all as strings - names, not IDs)
    private String gemstoneType;
    private BigDecimal caratWeight;
    private String color;
    private String colorQuality;  // ADD
    private String clarity;       // ADD
    private String cut;           // ADD
    private String origin;
    private String treatment;     // ADD

    // Price
    private BigDecimal price;
    private String currency;
    private BigDecimal pricePerCarat;

    // Stats
    private Integer viewsCount;
    private Integer likesCount;
    private Integer favoritesCount;

    // Status
    private String status;
    private Boolean isSold;
    private Boolean isCertified;  // ADD

    // Seller
    private Long sellerId;
    private String sellerName;
    private String sellerAvatar;

    // Timestamps
    private LocalDateTime createdAt;

    // User context
    private Boolean isLiked;
    private Boolean isFavorited;

    // Dimensions
    private BigDecimal lengthMm;
    private BigDecimal widthMm;
    private BigDecimal depthMm;

}