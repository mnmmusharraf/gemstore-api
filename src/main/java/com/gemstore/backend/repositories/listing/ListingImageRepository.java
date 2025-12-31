package com.gemstore.backend.repositories.listing;

import com.gemstore.backend. entities.listing.ListingImage;
import org.springframework.data.jpa.repository. JpaRepository;
import org. springframework.data.jpa.repository. Modifying;
import org.springframework.data.jpa.repository. Query;
import org.springframework. data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ListingImageRepository extends JpaRepository<ListingImage, Long> {

    List<ListingImage> findByListingIdOrderByDisplayOrderAsc(Long listingId);

    Optional<ListingImage> findByListingIdAndIsPrimaryTrue(Long listingId);

    long countByListingId(Long listingId);

    @Modifying
    @Query("DELETE FROM ListingImage li WHERE li.listing.id = : listingId")
    void deleteByListingId(@Param("listingId") Long listingId);

    @Modifying
    @Query("UPDATE ListingImage li SET li.isPrimary = false WHERE li.listing.id = :listingId")
    void clearPrimaryImage(@Param("listingId") Long listingId);
}