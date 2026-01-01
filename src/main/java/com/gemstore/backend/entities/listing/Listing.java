package com.gemstore.backend.entities.listing;

import com.gemstore.backend.entities.user.User;
import com.gemstore.backend.entities.listing.lookup.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations. UpdateTimestamp;

import java. math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "listings", indexes = {
        @Index(name = "idx_listings_seller", columnList = "seller_id"),
        @Index(name = "idx_listings_status", columnList = "status"),
        @Index(name = "idx_listings_type", columnList = "gemstone_type_id"),
        @Index(name = "idx_listings_price", columnList = "price"),
        @Index(name = "idx_listings_carat", columnList = "carat_weight"),
        @Index(name = "idx_listings_created", columnList = "created_at DESC")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Auto-generated
    @Column(name = "listing_number", unique = true, length = 20)
    private String listingNumber;

    // Seller
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    // Basic Info
    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Gemstone Type
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gemstone_type_id", nullable = false)
    private GemstoneType gemstoneType;

    // Carat Weight
    @Column(name = "carat_weight", nullable = false, precision = 8, scale = 3)
    private BigDecimal caratWeight;

    // Color
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "color_id")
    private Color color;

    // Color Quality
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "color_quality_id")
    private ColorQuality colorQuality;

    // Clarity
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clarity_id")
    private ClarityGrade clarity;

    // Cut/Shape
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cut_id")
    private Cut cut;

    // Origin
    @ManyToOne(fetch = FetchType. LAZY)
    @JoinColumn(name = "origin_id")
    private Origin origin;

    // Treatment
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "treatment_id")
    private Treatment treatment;

    // Dimensions
    @Column(name = "length_mm", precision = 6, scale = 2)
    private BigDecimal lengthMm;

    @Column(name = "width_mm", precision = 6, scale = 2)
    private BigDecimal widthMm;

    @Column(name = "depth_mm", precision = 6, scale = 2)
    private BigDecimal depthMm;

    // Pricing
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal price;

    @Column(length = 3)
    @Builder.Default
    private String currency = "LKR";

    // Auto-calculated (read-only in Java, computed by DB)
    @Column(name = "price_per_carat", precision = 14, scale = 2, insertable = false, updatable = false)
    private BigDecimal pricePerCarat;

    // Certificate
    @Column(name = "is_certified")
    @Builder.Default
    private Boolean isCertified = false;

    @Column(name = "certificate_info", length = 255)
    private String certificateInfo;

    // Status
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private ListingStatus status = ListingStatus.ACTIVE;

    // Engagement (auto-updated by triggers)
    @Column(name = "views_count")
    @Builder.Default
    private Integer viewsCount = 0;

    @Column(name = "favorites_count")
    @Builder.Default
    private Integer favoritesCount = 0;

    // Auto-calculated
    @Column(name = "completeness_score")
    @Builder.Default
    private Integer completenessScore = 0;

    // Sale Outcome
    @Column(name = "is_sold")
    @Builder.Default
    private Boolean isSold = false;

    @Column(name = "sold_price", precision = 14, scale = 2)
    private BigDecimal soldPrice;

    @Column(name = "sold_at")
    private LocalDateTime soldAt;

    @Column(name = "days_to_sell")
    private Integer daysToSell;

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<ListingImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Favorite> favorites = new ArrayList<>();

    // Helper methods
    public void addImage(ListingImage image) {
        images.add(image);
        image.setListing(this);
    }

    public void removeImage(ListingImage image) {
        images.remove(image);
        image.setListing(null);
    }

    @Column(name = "likes_count")
    @Builder. Default
    private Integer likesCount = 0;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Like> likes = new ArrayList<>();

}