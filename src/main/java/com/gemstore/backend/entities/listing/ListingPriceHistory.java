package com.gemstore.backend.entities.listing;

import jakarta. persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "listing_price_history", indexes = {
        @Index(name = "idx_price_history_listing", columnList = "listing_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingPriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType. IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType. LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @Column(name = "old_price", precision = 14, scale = 2)
    private BigDecimal oldPrice;

    @Column(name = "new_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal newPrice;

    @Column(name = "change_reason", length = 20)
    private String changeReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}