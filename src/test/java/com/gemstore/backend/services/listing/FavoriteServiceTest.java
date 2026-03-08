package com.gemstore.backend.services.listing;

import com.gemstore.backend.entities.listing.Favorite;
import com.gemstore.backend.entities.listing.Listing;
import com.gemstore.backend.entities.user.User;
import com.gemstore.backend.exceptions.ResourceNotFoundException;
import com.gemstore.backend.mappers.listing.FavoriteMapper;
import com.gemstore.backend.mappers.listing.ListingMapper;
import com.gemstore.backend.repositories.listing.FavoriteRepository;
import com.gemstore.backend.repositories.listing.ListingRepository;
import com.gemstore.backend.repositories.user.UserRepository;
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
@DisplayName("FavoriteService - Unit Tests")
class FavoriteServiceTest {

    @Mock private FavoriteRepository favoriteRepository;
    @Mock private ListingRepository listingRepository;
    @Mock private UserRepository userRepository;
    @Mock private FavoriteMapper favoriteMapper;
    @Mock private ListingMapper listingMapper;

    @InjectMocks
    private FavoriteService favoriteService;

    private User user;
    private Listing listing;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        listing = new Listing();
        listing.setId(10L);
        listing.setTitle("Ruby");
    }

    @Nested
    @DisplayName("addFavorite()")
    class AddFavorite {

        @Test
        @DisplayName("TC-FAV-001: Should add favorite successfully")
        void shouldAddFavorite() {
            when(favoriteRepository.existsByUserIdAndListingId(1L, 10L)).thenReturn(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(listingRepository.findById(10L)).thenReturn(Optional.of(listing));

            favoriteService.addFavorite(1L, 10L);

            verify(favoriteRepository).save(any(Favorite.class));
        }

        @Test
        @DisplayName("TC-FAV-002: Should skip if already favorited")
        void shouldSkipIfAlreadyFavorited() {
            when(favoriteRepository.existsByUserIdAndListingId(1L, 10L)).thenReturn(true);

            favoriteService.addFavorite(1L, 10L);

            verify(favoriteRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-FAV-003: Should throw when user not found")
        void shouldThrowWhenUserNotFound() {
            when(favoriteRepository.existsByUserIdAndListingId(99L, 10L)).thenReturn(false);
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> favoriteService.addFavorite(99L, 10L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("TC-FAV-004: Should throw when listing not found")
        void shouldThrowWhenListingNotFound() {
            when(favoriteRepository.existsByUserIdAndListingId(1L, 999L)).thenReturn(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(listingRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> favoriteService.addFavorite(1L, 999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Listing not found");
        }
    }

    @Nested
    @DisplayName("removeFavorite()")
    class RemoveFavorite {

        @Test
        @DisplayName("TC-FAV-005: Should remove favorite")
        void shouldRemoveFavorite() {
            favoriteService.removeFavorite(1L, 10L);

            verify(favoriteRepository).deleteByUserIdAndListingId(1L, 10L);
        }
    }

    @Nested
    @DisplayName("toggleFavorite()")
    class ToggleFavorite {

        @Test
        @DisplayName("TC-FAV-006: Should add when not favorited (returns true)")
        void shouldAddWhenNotFavorited() {
            when(favoriteRepository.existsByUserIdAndListingId(1L, 10L)).thenReturn(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(listingRepository.findById(10L)).thenReturn(Optional.of(listing));

            boolean result = favoriteService.toggleFavorite(1L, 10L);

            assertThat(result).isTrue();
            verify(favoriteRepository).save(any());
        }

        @Test
        @DisplayName("TC-FAV-007: Should remove when already favorited (returns false)")
        void shouldRemoveWhenFavorited() {
            when(favoriteRepository.existsByUserIdAndListingId(1L, 10L)).thenReturn(true);

            boolean result = favoriteService.toggleFavorite(1L, 10L);

            assertThat(result).isFalse();
            verify(favoriteRepository).deleteByUserIdAndListingId(1L, 10L);
        }
    }

    @Nested
    @DisplayName("isFavorited()")
    class IsFavorited {

        @Test
        @DisplayName("TC-FAV-008: Should return true when favorited")
        void shouldReturnTrue() {
            when(favoriteRepository.existsByUserIdAndListingId(1L, 10L)).thenReturn(true);

            assertThat(favoriteService.isFavorited(10L, 1L)).isTrue();
        }

        @Test
        @DisplayName("TC-FAV-009: Should return false when null userId")
        void shouldReturnFalseForNull() {
            assertThat(favoriteService.isFavorited(10L, null)).isFalse();
        }
    }

    @Nested
    @DisplayName("getFavoritedListingIds()")
    class GetFavoritedListingIds {

        @Test
        @DisplayName("TC-FAV-010: Should return favorited IDs")
        void shouldReturnIds() {
            when(favoriteRepository.findFavoritedListingIdsByUserId(1L, List.of(1L, 2L, 3L)))
                    .thenReturn(Set.of(2L));

            Set<Long> result = favoriteService.getFavoritedListingIds(1L, List.of(1L, 2L, 3L));

            assertThat(result).containsExactly(2L);
        }

        @Test
        @DisplayName("TC-FAV-011: Should return empty for null userId")
        void shouldReturnEmptyForNull() {
            assertThat(favoriteService.getFavoritedListingIds(null, List.of(1L))).isEmpty();
        }

        @Test
        @DisplayName("TC-FAV-012: Should return empty for empty list")
        void shouldReturnEmptyForEmptyList() {
            assertThat(favoriteService.getFavoritedListingIds(1L, List.of())).isEmpty();
        }
    }

    @Nested
    @DisplayName("counts")
    class Counts {

        @Test
        @DisplayName("TC-FAV-013: Should return user favorites count")
        void shouldReturnUserCount() {
            when(favoriteRepository.countByUserId(1L)).thenReturn(5L);

            assertThat(favoriteService.getFavoritesCount(1L)).isEqualTo(5L);
        }

        @Test
        @DisplayName("TC-FAV-014: Should return listing favorites count")
        void shouldReturnListingCount() {
            when(favoriteRepository.countByListingId(10L)).thenReturn(3L);

            assertThat(favoriteService.getListingFavoritesCount(10L)).isEqualTo(3L);
        }
    }
}