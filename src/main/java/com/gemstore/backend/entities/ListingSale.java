package com.gemstore.backend.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time. LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "listing_sales")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingSale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id")
    private User buyer;

    @Column(name = "listed_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal listedPrice;

    @Column(name = "sold_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal soldPrice;

    @Column(name = "days_on_market")
    private Integer daysOnMarket;

    // JSONB for ML snapshot
    @JdbcTypeCode(SqlTypes. JSON)
    @Column(name = "listing_snapshot", columnDefinition = "jsonb")
    private Map<String, Object> listingSnapshot;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}