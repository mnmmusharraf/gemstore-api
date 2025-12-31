package com.gemstore.backend.repositories.listing;

import com.gemstore.backend. entities.listing.ListingSale;
import org.springframework.data. domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework. data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype. Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ListingSaleRepository extends JpaRepository<ListingSale, Long> {

    Optional<ListingSale> findByListingId(Long listingId);

    List<ListingSale> findBySellerIdOrderByCreatedAtDesc(Long sellerId);

    List<ListingSale> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);

    Page<ListingSale> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // ML: Sales by gemstone type
    @Query("""
            SELECT ls FROM ListingSale ls 
            JOIN ls.listing l 
            WHERE l.gemstoneType.id = :gemstoneTypeId 
            ORDER BY ls.createdAt DESC
            """)
    List<ListingSale> findByGemstoneType(@Param("gemstoneTypeId") Integer gemstoneTypeId);

    // ML: Average sold price by gemstone type
    @Query("""
            SELECT l.gemstoneType.id, AVG(ls.soldPrice), COUNT(ls) 
            FROM ListingSale ls 
            JOIN ls.listing l 
            GROUP BY l.gemstoneType. id
            """)
    List<Object[]> getAverageSoldPriceByGemstoneType();

    // ML: Average discount percentage
    @Query("SELECT AVG((ls.listedPrice - ls.soldPrice) / ls.listedPrice * 100) FROM ListingSale ls")
    BigDecimal getAverageDiscountPercentage();

    // ML: Average days on market
    @Query("SELECT AVG(ls. daysOnMarket) FROM ListingSale ls WHERE ls. daysOnMarket IS NOT NULL")
    Double getAverageDaysOnMarket();

    // Sales within date range
    @Query("SELECT ls FROM ListingSale ls WHERE ls.createdAt BETWEEN :startDate AND :endDate")
    List<ListingSale> findSalesBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Count sales by seller
    long countBySellerId(Long sellerId);
}