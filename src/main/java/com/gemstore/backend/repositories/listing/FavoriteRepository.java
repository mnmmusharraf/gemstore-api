package com.gemstore.backend.repositories. listing;

import com.gemstore.backend.entities.listing. Favorite;
import com.gemstore.backend.entities. listing.Listing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework. data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype. Repository;

import java.util. List;
import java.util. Optional;
import java.util.Set;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Optional<Favorite> findByUserIdAndListingId(Long userId, Long listingId);

    boolean existsByUserIdAndListingId(Long userId, Long listingId);

    List<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId);

    Page<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT f. listing FROM Favorite f WHERE f. user.id = :userId ORDER BY f.createdAt DESC")
    Page<Listing> findFavoriteListingsByUserId(@Param("userId") Long userId, Pageable pageable);

    long countByListingId(Long listingId);

    long countByUserId(Long userId);

    void deleteByUserIdAndListingId(Long userId, Long listingId);

    @Query("SELECT f. listing. id FROM Favorite f WHERE f. user.id = :userId AND f.listing.id IN :listingIds")
    Set<Long> findFavoritedListingIdsByUserId(@Param("userId") Long userId, @Param("listingIds") List<Long> listingIds);}