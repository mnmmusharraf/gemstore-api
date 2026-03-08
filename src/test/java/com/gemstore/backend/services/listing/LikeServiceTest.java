package com.gemstore.backend.services.listing;

import com.gemstore.backend.entities.listing.Listing;
import com.gemstore.backend.entities.listing.ListingStatus;
import com.gemstore.backend.entities.user.User;
import com.gemstore.backend.repositories.listing.LikeRepository;
import com.gemstore.backend.repositories.listing.ListingRepository;
import com.gemstore.backend.repositories.user.UserRepository;
import com.gemstore.backend.services.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LikeService - Unit Tests")
class LikeServiceTest {

    @Mock private LikeRepository likeRepository;
    @Mock private ListingRepository listingRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private LikeService likeService;

    private User liker;
    private User seller;
    private Listing listing;

    @BeforeEach
    void setUp() {
        liker = new User();
        liker.setId(1L);
        liker.setUsername("liker");

        seller = new User();
        seller.setId(2L);
        seller.setUsername("seller");

        listing = new Listing();
        listing.setId(10L);
        listing.setTitle("Blue Sapphire");
        listing.setStatus(ListingStatus.ACTIVE);
        listing.setSeller(seller);
    }

    @Nested
    @DisplayName("toggleLike()")
    class ToggleLike {

        @Test
        @DisplayName("TC-LIKE-001: Should like when not already liked")
        void shouldLikeWhenNotLiked() {
            when(likeRepository.existsByListingIdAndUserId(10L, 1L)).thenReturn(false);
            when(listingRepository.findById(10L)).thenReturn(Optional.of(listing));
            when(userRepository.findById(1L)).thenReturn(Optional.of(liker));

            boolean result = likeService.toggleLike(10L, 1L);

            assertThat(result).isTrue();
            verify(likeRepository).save(any());
            verify(listingRepository).incrementLikesCount(10L);
            verify(notificationService).notifyLike(seller, liker, listing);
        }

        @Test
        @DisplayName("TC-LIKE-002: Should unlike when already liked")
        void shouldUnlikeWhenAlreadyLiked() {
            when(likeRepository.existsByListingIdAndUserId(10L, 1L)).thenReturn(true);

            boolean result = likeService.toggleLike(10L, 1L);

            assertThat(result).isFalse();
            verify(likeRepository).deleteByListingIdAndUserId(10L, 1L);
            verify(listingRepository).decrementLikesCount(10L);
            verify(likeRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-LIKE-003: Should not notify when liker is seller (self-like)")
        void shouldNotNotifySelfLike() {
            listing.setSeller(liker); // seller is the liker

            when(likeRepository.existsByListingIdAndUserId(10L, 1L)).thenReturn(false);
            when(listingRepository.findById(10L)).thenReturn(Optional.of(listing));
            when(userRepository.findById(1L)).thenReturn(Optional.of(liker));

            likeService.toggleLike(10L, 1L);

            verify(notificationService, never()).notifyLike(any(), any(), any());
        }

        @Test
        @DisplayName("TC-LIKE-004: Should throw when listing not found")
        void shouldThrowWhenListingNotFound() {
            when(likeRepository.existsByListingIdAndUserId(999L, 1L)).thenReturn(false);
            when(listingRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> likeService.toggleLike(999L, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Listing not found");
        }
    }

    @Nested
    @DisplayName("isLiked()")
    class IsLiked {

        @Test
        @DisplayName("TC-LIKE-005: Should return true when liked")
        void shouldReturnTrueWhenLiked() {
            when(likeRepository.existsByListingIdAndUserId(10L, 1L)).thenReturn(true);

            assertThat(likeService.isLiked(10L, 1L)).isTrue();
        }

        @Test
        @DisplayName("TC-LIKE-006: Should return false when not liked")
        void shouldReturnFalseWhenNotLiked() {
            when(likeRepository.existsByListingIdAndUserId(10L, 1L)).thenReturn(false);

            assertThat(likeService.isLiked(10L, 1L)).isFalse();
        }

        @Test
        @DisplayName("TC-LIKE-007: Should return false when userId is null")
        void shouldReturnFalseWhenNullUser() {
            assertThat(likeService.isLiked(10L, null)).isFalse();
        }
    }

    @Nested
    @DisplayName("getLikesCount()")
    class GetLikesCount {

        @Test
        @DisplayName("TC-LIKE-008: Should return correct count")
        void shouldReturnCount() {
            when(likeRepository.countByListingId(10L)).thenReturn(5L);

            assertThat(likeService.getLikesCount(10L)).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("getLikedListingIds()")
    class GetLikedListingIds {

        @Test
        @DisplayName("TC-LIKE-009: Should return liked IDs")
        void shouldReturnLikedIds() {
            List<Long> listingIds = List.of(1L, 2L, 3L);
            when(likeRepository.findLikedListingIdsByUserId(1L, listingIds))
                    .thenReturn(Set.of(1L, 3L));

            Set<Long> result = likeService.getLikedListingIds(1L, listingIds);

            assertThat(result).containsExactlyInAnyOrder(1L, 3L);
        }

        @Test
        @DisplayName("TC-LIKE-010: Should return empty set for null userId")
        void shouldReturnEmptyForNullUser() {
            assertThat(likeService.getLikedListingIds(null, List.of(1L))).isEmpty();
        }

        @Test
        @DisplayName("TC-LIKE-011: Should return empty set for empty list")
        void shouldReturnEmptyForEmptyList() {
            assertThat(likeService.getLikedListingIds(1L, List.of())).isEmpty();
        }
    }
}