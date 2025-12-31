package com.gemstore.backend.repositories.listing;

import com.gemstore.backend. entities.listing.ListingView;
import org.springframework.data. jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data. repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ListingViewRepository extends JpaRepository<ListingView, Long> {

    long countByListingId(Long listingId);

    @Query("SELECT COUNT(DISTINCT lv.user.id) FROM ListingView lv WHERE lv. listing.id = :listingId AND lv.user IS NOT NULL")
    long countUniqueViewsByListingId(@Param("listingId") Long listingId);

    @Query("SELECT COUNT(lv) FROM ListingView lv WHERE lv.listing.id = :listingId AND lv.viewedAt >= :since")
    long countViewsSince(@Param("listingId") Long listingId, @Param("since") LocalDateTime since);

    // Check if user viewed recently (prevent spam)
    @Query("""
            SELECT COUNT(lv) > 0 FROM ListingView lv 
            WHERE lv.listing.id = :listingId 
            AND lv.user.id = :userId 
            AND lv.viewedAt >= :since
            """)
    boolean hasUserViewedRecently(
            @Param("listingId") Long listingId,
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since
    );

    // Most viewed listings
    @Query("""
            SELECT lv.listing.id, COUNT(lv) as viewCount 
            FROM ListingView lv 
            WHERE lv.viewedAt >= :since 
            GROUP BY lv.listing.id 
            ORDER BY viewCount DESC
            """)
    List<Object[]> findMostViewedListings(@Param("since") LocalDateTime since);
}