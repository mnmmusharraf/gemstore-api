package com. gemstore.backend.repositories.listing;

import com.gemstore. backend.entities.listing.Like;
import org.springframework.data. jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework. data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype. Repository;

import java.util. List;
import java.util. Set;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    boolean existsByListingIdAndUserId(Long listingId, Long userId);

    long countByListingId(Long listingId);

    @Modifying
    @Query("DELETE FROM Like l WHERE l.listing.id = :listingId AND l. user.id = :userId")
    void deleteByListingIdAndUserId(@Param("listingId") Long listingId, @Param("userId") Long userId);

    @Query("SELECT l. listing.id FROM Like l WHERE l.user.id = :userId AND l.listing.id IN :listingIds")
    Set<Long> findLikedListingIdsByUserId(@Param("userId") Long userId, @Param("listingIds") List<Long> listingIds);
}