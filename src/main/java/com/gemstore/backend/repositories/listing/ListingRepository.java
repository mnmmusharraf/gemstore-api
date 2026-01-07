package com.gemstore.backend.repositories.listing;

import com.gemstore.backend.entities.listing.Listing;
import com.gemstore.backend.entities.listing.ListingStatus;
import org.springframework.data.domain. Page;
import org.springframework. data.domain.Pageable;
import org.springframework.data. jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository. Modifying;
import org.springframework.data.jpa.repository. Query;
import org.springframework. data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long>, JpaSpecificationExecutor<Listing> {

    // Find by listing number
    Optional<Listing> findByListingNumber(String listingNumber);

    // Find by seller
    List<Listing> findBySellerIdOrderByCreatedAtDesc(Long sellerId);

    Page<Listing> findBySellerIdAndStatus(Long sellerId, ListingStatus status, Pageable pageable);

    // Find active listings
    Page<Listing> findByStatusOrderByCreatedAtDesc(ListingStatus status, Pageable pageable);

    @Query("SELECT l FROM Listing l WHERE l.status = 'ACTIVE' ORDER BY l.createdAt DESC")
    Page<Listing> findActiveListings(Pageable pageable);

    // Search by gemstone type
    Page<Listing> findByGemstoneTypeIdAndStatus(Integer gemstoneTypeId, ListingStatus status, Pageable pageable);

    // Price range search
    @Query("SELECT l FROM Listing l WHERE l.status = 'ACTIVE' AND l.price BETWEEN :minPrice AND :maxPrice")
    Page<Listing> findByPriceRange(
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );

    // Carat range search
    @Query("SELECT l FROM Listing l WHERE l. status = 'ACTIVE' AND l.caratWeight BETWEEN :minCarat AND :maxCarat")
    Page<Listing> findByCaratRange(
            @Param("minCarat") BigDecimal minCarat,
            @Param("maxCarat") BigDecimal maxCarat,
            Pageable pageable
    );

    // Combined search
    @Query("""
            SELECT l FROM Listing l 
            WHERE l.status = 'ACTIVE'
            AND (:gemstoneTypeId IS NULL OR l.gemstoneType.id = :gemstoneTypeId)
            AND (:colorId IS NULL OR l.color.id = :colorId)
            AND (:originId IS NULL OR l.origin. id = :originId)
            AND (:minPrice IS NULL OR l.price >= :minPrice)
            AND (:maxPrice IS NULL OR l. price <= :maxPrice)
            AND (:minCarat IS NULL OR l.caratWeight >= : minCarat)
            AND (:maxCarat IS NULL OR l.caratWeight <= :maxCarat)
            ORDER BY l.createdAt DESC
            """)
    Page<Listing> searchListings(
            @Param("gemstoneTypeId") Integer gemstoneTypeId,
            @Param("colorId") Integer colorId,
            @Param("originId") Integer originId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("minCarat") BigDecimal minCarat,
            @Param("maxCarat") BigDecimal maxCarat,
            Pageable pageable
    );

    // Full-text search
    @Query(value = """
            SELECT * FROM listings 
            WHERE status = 'ACTIVE' 
            AND search_vector @@ plainto_tsquery('english', : query)
            ORDER BY ts_rank(search_vector, plainto_tsquery('english', : query)) DESC
            """, nativeQuery = true)
    Page<Listing> searchByText(@Param("query") String query, Pageable pageable);

    // Count by seller
    long countBySellerIdAndStatus(Long sellerId, ListingStatus status);

    // Count by gemstone type (for facets)
    @Query("SELECT l.gemstoneType.id, COUNT(l) FROM Listing l WHERE l.status = 'ACTIVE' GROUP BY l.gemstoneType.id")
    List<Object[]> countByGemstoneType();

    // Featured listings
//    @Query("SELECT l FROM Listing l WHERE l.status = 'ACTIVE' AND l.isFeatured = true ORDER BY l.createdAt DESC")
//    List<Listing> findFeaturedListings(Pageable pageable);

    // Update view count
    @Modifying
    @Query("UPDATE Listing l SET l.viewsCount = l.viewsCount + 1 WHERE l.id = :id")
    void incrementViewCount(@Param("id") Long id);

    // ML:  Find sold listings
    @Query("SELECT l FROM Listing l WHERE l.isSold = true ORDER BY l.soldAt DESC")
    Page<Listing> findSoldListings(Pageable pageable);

    // ML: Find listings with high completeness
    @Query("SELECT l FROM Listing l WHERE l.isSold = true AND l.completenessScore >= :minScore")
    List<Listing> findSoldListingsWithMinCompleteness(@Param("minScore") Integer minScore);

    @Modifying
    @Query("UPDATE Listing l SET l.likesCount = l.likesCount + 1 WHERE l.id = :listingId")
    void incrementLikesCount(@Param("listingId") Long listingId);

    @Modifying
    @Query("UPDATE Listing l SET l.likesCount = GREATEST(l.likesCount - 1, 0) WHERE l.id = :listingId")
    void decrementLikesCount(@Param("listingId") Long listingId);

    // Get all listings by seller (regardless of status)
    Page<Listing> findBySellerId(Long sellerId, Pageable pageable);}