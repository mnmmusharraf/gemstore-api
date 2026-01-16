package com.gemstore.backend.services.listing;

import com.gemstore.backend.entities.listing.Like;
import com.gemstore.backend.entities.listing.Listing;
import com.gemstore.backend.entities.user.User;
import com.gemstore.backend.repositories.listing.LikeRepository;
import com.gemstore.backend.repositories.listing.ListingRepository;
import com.gemstore.backend.repositories.user.UserRepository;
import com.gemstore.backend.services.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation. Transactional;

import java.util. List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class LikeService {

    private final LikeRepository likeRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;


    /**
     * Toggle like on a listing
     * @return true if liked, false if unliked
     */
    @Transactional
    public boolean toggleLike(Long listingId, Long userId) {

        boolean exists = likeRepository.existsByListingIdAndUserId(listingId, userId);

        if (exists) {
            // Unlike
            likeRepository.deleteByListingIdAndUserId(listingId, userId);
            listingRepository.decrementLikesCount(listingId);
            return false;
        }

        // Like
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found"));

        User liker = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        likeRepository.save(
                Like.builder()
                        .listing(listing)
                        .user(liker)
                        .build()
        );

        listingRepository.incrementLikesCount(listingId);

        //  SEND REAL-TIME NOTIFICATION
        User seller = listing.getSeller();

        if (seller != null && !seller.getId().equals(userId)) { // avoid self-like
            notificationService.notifyLike(seller, liker, listing);
        }
        return true;
    }


    /**
     * Check if user has liked a listing
     */
    public boolean isLiked(Long listingId, Long userId) {
        if (userId == null) return false;
        return likeRepository.existsByListingIdAndUserId(listingId, userId);
    }

    /**
     * Get likes count for a listing
     */
    public long getLikesCount(Long listingId) {
        return likeRepository.countByListingId(listingId);
    }

    /**
     * Batch check - get all liked listing IDs for a user
     */
    public Set<Long> getLikedListingIds(Long userId, List<Long> listingIds) {
        if (userId == null || listingIds.isEmpty()) {
            return Set.of();
        }
        return likeRepository.findLikedListingIdsByUserId(userId, listingIds);
    }
}