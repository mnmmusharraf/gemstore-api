package com.gemstore.backend.repositories. listing;

import com.gemstore.backend.entities.listing.ListingPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype. Repository;

import java.util. List;

@Repository
public interface ListingPriceHistoryRepository extends JpaRepository<ListingPriceHistory, Long> {

    List<ListingPriceHistory> findByListingIdOrderByCreatedAtDesc(Long listingId);

    @Query("SELECT COUNT(ph) FROM ListingPriceHistory ph WHERE ph.listing.id = :listingId")
    long countPriceChangesByListingId(@Param("listingId") Long listingId);

    @Query("SELECT ph FROM ListingPriceHistory ph WHERE ph.listing.id = : listingId ORDER BY ph.createdAt ASC")
    List<ListingPriceHistory> findPriceHistoryAsc(@Param("listingId") Long listingId);
}