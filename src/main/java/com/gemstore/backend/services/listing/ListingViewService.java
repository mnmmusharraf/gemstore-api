package com.gemstore.backend.services.listing;

import com.gemstore.backend.entities.user.User;
import com.gemstore. backend.entities.listing.Listing;
import com.gemstore. backend.entities.listing.ListingView;
import com.gemstore.backend.repositories.listing.ListingRepository;
import com.gemstore.backend.repositories.listing. ListingViewRepository;
import com.gemstore.backend.repositories.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j. Slf4j;
import org. springframework.stereotype.Service;
import org.springframework.transaction.annotation. Transactional;

import java.time.LocalDateTime;

/**
 * ListingViewService handles view tracking for analytics and ML. 
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ListingViewService {

    private final ListingViewRepository listingViewRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    // Prevent spam: don't count views from same user within X minutes
    private static final int VIEW_COOLDOWN_MINUTES = 30;

    @Transactional
    public void recordView(Long listingId, Long userId) {
        // Check cooldown
        LocalDateTime cooldownStart = LocalDateTime.now().minusMinutes(VIEW_COOLDOWN_MINUTES);

        if (listingViewRepository.hasUserViewedRecently(listingId, userId, cooldownStart)) {
            log.debug("View cooldown active - Listing: {}, User: {}", listingId, userId);
            return;
        }

        Listing listing = listingRepository.findById(listingId).orElse(null);
        User user = userRepository.findById(userId).orElse(null);

        if (listing == null) {
            log.warn("Listing not found: {}", listingId);
            return;
        }

        // Record view
        ListingView view = ListingView.builder()
                .listing(listing)
                .user(user)
                .build();

        listingViewRepository. save(view);

        // Increment counter
        listingRepository.incrementViewCount(listingId);

        log.debug("View recorded - Listing: {}, User: {}", listingId, userId);
    }

    @Transactional
    public void recordAnonymousView(Long listingId) {
        Listing listing = listingRepository.findById(listingId).orElse(null);

        if (listing == null) {
            return;
        }

        ListingView view = ListingView. builder()
                .listing(listing)
                .user(null)
                .build();

        listingViewRepository.save(view);
        listingRepository.incrementViewCount(listingId);

        log.debug("Anonymous view recorded - Listing: {}", listingId);
    }

    @Transactional(readOnly = true)
    public long getTotalViews(Long listingId) {
        return listingViewRepository.countByListingId(listingId);
    }

    @Transactional(readOnly = true)
    public long getUniqueViews(Long listingId) {
        return listingViewRepository.countUniqueViewsByListingId(listingId);
    }

    @Transactional(readOnly = true)
    public long getViewsSince(Long listingId, LocalDateTime since) {
        return listingViewRepository.countViewsSince(listingId, since);
    }
}