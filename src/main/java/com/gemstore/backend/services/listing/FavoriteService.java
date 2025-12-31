package com.gemstore.backend. services.listing;

import com. gemstore.backend. dtos.common.PageResponse;
import com.gemstore.backend.dtos.listing. FavoriteDTO;
import com.gemstore. backend.dtos.listing.response.ListingCardResponse;
import com. gemstore.backend.entities.user.User;
import com.gemstore. backend.entities.listing.Favorite;
import com.gemstore. backend.entities.listing.Listing;
import com.gemstore. backend.exceptions.ResourceNotFoundException;
import com. gemstore.backend.mappers.listing.FavoriteMapper;
import com.gemstore. backend.mappers.listing.ListingMapper;
import com.gemstore.backend.repositories.listing.FavoriteRepository;
import com.gemstore.backend.repositories.listing.ListingRepository;
import com.gemstore.backend.repositories.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j. Slf4j;
import org. springframework.data.domain.*;
import org.springframework.stereotype. Service;
import org.springframework. transaction.annotation.Transactional;

import java.util.List;

/**
 * FavoriteService handles user favorites/wishlist functionality.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final FavoriteMapper favoriteMapper;
    private final ListingMapper listingMapper;

    @Transactional
    public void addFavorite(Long userId, Long listingId) {
        log.info("Adding favorite - User: {}, Listing: {}", userId, listingId);

        // Check if already favorited
        if (favoriteRepository.existsByUserIdAndListingId(userId, listingId)) {
            log.info("Listing already favorited");
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found: " + listingId));

        Favorite favorite = Favorite.builder()
                .user(user)
                .listing(listing)
                .build();

        favoriteRepository.save(favorite);

        log.info("Favorite added - User: {}, Listing: {}", userId, listingId);
    }

    @Transactional
    public void removeFavorite(Long userId, Long listingId) {
        log.info("Removing favorite - User: {}, Listing: {}", userId, listingId);

        favoriteRepository.deleteByUserIdAndListingId(userId, listingId);

        log.info("Favorite removed - User: {}, Listing: {}", userId, listingId);
    }

    @Transactional
    public boolean toggleFavorite(Long userId, Long listingId) {
        if (favoriteRepository.existsByUserIdAndListingId(userId, listingId)) {
            removeFavorite(userId, listingId);
            return false;
        } else {
            addFavorite(userId, listingId);
            return true;
        }
    }

    @Transactional(readOnly = true)
    public boolean isFavorited(Long userId, Long listingId) {
        return favoriteRepository.existsByUserIdAndListingId(userId, listingId);
    }

    @Transactional(readOnly = true)
    public PageResponse<ListingCardResponse> getUserFavorites(Long userId, int page, int size) {
        Pageable pageable = PageRequest. of(page, size);
        Page<Listing> favorites = favoriteRepository.findFavoriteListingsByUserId(userId, pageable);

        List<ListingCardResponse> content = favorites.getContent().stream()
                .map(listing -> {
                    ListingCardResponse card = listingMapper.toCardResponse(listing);
                    card.setIsFavorited(true);
                    return card;
                })
                .toList();

        return PageResponse.<ListingCardResponse>builder()
                .content(content)
                .page(favorites.getNumber())
                .size(favorites.getSize())
                .totalElements(favorites. getTotalElements())
                .totalPages(favorites.getTotalPages())
                .first(favorites.isFirst())
                .last(favorites.isLast())
                .hasNext(favorites.hasNext())
                .hasPrevious(favorites.hasPrevious())
                .build();
    }

    @Transactional(readOnly = true)
    public long getFavoritesCount(Long userId) {
        return favoriteRepository. countByUserId(userId);
    }

    @Transactional(readOnly = true)
    public long getListingFavoritesCount(Long listingId) {
        return favoriteRepository.countByListingId(listingId);
    }
}