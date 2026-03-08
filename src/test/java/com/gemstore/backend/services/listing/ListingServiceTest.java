package com.gemstore.backend.services.listing;

import com.gemstore.backend.dtos.listing.request.CreateListingRequest;
import com.gemstore.backend.dtos.listing.response.ListingResponse;
import com.gemstore.backend.entities.listing.Listing;
import com.gemstore.backend.entities.listing.ListingStatus;
import com.gemstore.backend.entities.user.User;
import com.gemstore.backend.exceptions.ResourceNotFoundException;
import com.gemstore.backend.exceptions.UnauthorizedException;
import com.gemstore.backend.mappers.listing.ListingMapper;
import com.gemstore.backend.mappers.listing.ListingPriceHistoryMapper;
import com.gemstore.backend.repositories.listing.*;
import com.gemstore.backend.repositories.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListingService - Unit Tests")
class ListingServiceTest {

    @Mock private ListingRepository listingRepository;
    @Mock private ListingImageRepository listingImageRepository;
    @Mock private ListingPriceHistoryRepository priceHistoryRepository;
    @Mock private FavoriteRepository favoriteRepository;
    @Mock private UserRepository userRepository;
    @Mock private LookupService lookupService;
    @Mock private ListingImageService listingImageService;
    @Mock private ListingViewService listingViewService;
    @Mock private LikeService likeService;
    @Mock private FavoriteService favoriteService;
    @Mock private ListingMapper listingMapper;
    @Mock private ListingPriceHistoryMapper priceHistoryMapper;

    @InjectMocks
    private ListingService listingService;

    private User seller;
    private User otherUser;
    private Listing listing;
    private ListingResponse listingResponse;

    @BeforeEach
    void setUp() {
        seller = new User();
        seller.setId(1L);
        seller.setUsername("seller");

        otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("other");

        listing = new Listing();
        listing.setId(100L);
        listing.setTitle("Blue Sapphire");
        listing.setPrice(new BigDecimal("50000.00"));
        listing.setCurrency("LKR");
        listing.setStatus(ListingStatus.ACTIVE);
        listing.setSeller(seller);
        listing.setIsSold(false);

        listingResponse = new ListingResponse();
        listingResponse.setId(100L);
        listingResponse.setTitle("Blue Sapphire");
    }

    @Nested
    @DisplayName("getListingById()")
    class GetListingById {

        @Test
        @DisplayName("TC-LST-001: Should return listing by ID")
        void shouldReturnListing() {
            when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));
            when(listingMapper.toResponse(listing)).thenReturn(listingResponse);

            ListingResponse result = listingService.getListingById(100L, null);

            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("Blue Sapphire");
        }

        @Test
        @DisplayName("TC-LST-002: Should throw when listing not found")
        void shouldThrowWhenNotFound() {
            when(listingRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> listingService.getListingById(999L, null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Listing not found");
        }

        @Test
        @DisplayName("TC-LST-003: Should set like/favorite status when userId provided")
        void shouldSetLikeFavoriteStatus() {
            when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));
            when(listingMapper.toResponse(listing)).thenReturn(listingResponse);
            when(likeService.isLiked(100L, 1L)).thenReturn(true);
            when(favoriteService.isFavorited(100L, 1L)).thenReturn(false);

            ListingResponse result = listingService.getListingById(100L, 1L);

            verify(likeService).isLiked(100L, 1L);
            verify(favoriteService).isFavorited(100L, 1L);
        }
    }

    @Nested
    @DisplayName("updateListing()")
    class UpdateListing {

        @Test
        @DisplayName("TC-LST-004: Should reject update by non-owner")
        void shouldRejectNonOwner() {
            when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));

            assertThatThrownBy(() -> listingService.updateListing(100L, null, 2L))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("only update your own");
        }

        @Test
        @DisplayName("TC-LST-005: Should reject update of sold listing")
        void shouldRejectSoldListing() {
            listing.setIsSold(true);
            when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));

            assertThatThrownBy(() -> listingService.updateListing(100L, null, 1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("sold");
        }
    }

    @Nested
    @DisplayName("markAsSold()")
    class MarkAsSold {

        @Test
        @DisplayName("TC-LST-006: Should mark listing as sold")
        void shouldMarkAsSold() {
            when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));
            when(listingRepository.save(any())).thenReturn(listing);
            when(listingMapper.toResponse(listing)).thenReturn(listingResponse);

            ListingResponse result = listingService.markAsSold(100L, new BigDecimal("45000"), 1L);

            assertThat(listing.getIsSold()).isTrue();
            assertThat(listing.getStatus()).isEqualTo(ListingStatus.SOLD);
            assertThat(listing.getSoldPrice()).isEqualTo(new BigDecimal("45000"));
        }

        @Test
        @DisplayName("TC-LST-007: Should use listing price when soldPrice is null")
        void shouldUseListingPriceWhenNull() {
            when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));
            when(listingRepository.save(any())).thenReturn(listing);
            when(listingMapper.toResponse(listing)).thenReturn(listingResponse);

            listingService.markAsSold(100L, null, 1L);

            assertThat(listing.getSoldPrice()).isEqualTo(new BigDecimal("50000.00"));
        }

        @Test
        @DisplayName("TC-LST-008: Should reject if not owner")
        void shouldRejectIfNotOwner() {
            when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));

            assertThatThrownBy(() -> listingService.markAsSold(100L, null, 2L))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("TC-LST-009: Should reject if already sold")
        void shouldRejectIfAlreadySold() {
            listing.setIsSold(true);
            when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));

            assertThatThrownBy(() -> listingService.markAsSold(100L, null, 1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already sold");
        }
    }

    @Nested
    @DisplayName("archiveListing()")
    class ArchiveListing {

        @Test
        @DisplayName("TC-LST-010: Should archive listing")
        void shouldArchive() {
            when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));

            listingService.archiveListing(100L, 1L);

            assertThat(listing.getStatus()).isEqualTo(ListingStatus.ARCHIVED);
            verify(listingRepository).save(listing);
        }

        @Test
        @DisplayName("TC-LST-011: Should reject archive by non-owner")
        void shouldRejectNonOwner() {
            when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));

            assertThatThrownBy(() -> listingService.archiveListing(100L, 2L))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }

    @Nested
    @DisplayName("reactivateListing()")
    class ReactivateListing {

        @Test
        @DisplayName("TC-LST-012: Should reactivate archived listing")
        void shouldReactivate() {
            listing.setStatus(ListingStatus.ARCHIVED);
            when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));
            when(listingRepository.save(any())).thenReturn(listing);
            when(listingMapper.toResponse(listing)).thenReturn(listingResponse);

            listingService.reactivateListing(100L, 1L);

            assertThat(listing.getStatus()).isEqualTo(ListingStatus.ACTIVE);
        }

        @Test
        @DisplayName("TC-LST-013: Should reject reactivation of sold listing")
        void shouldRejectSoldReactivation() {
            listing.setIsSold(true);
            when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));

            assertThatThrownBy(() -> listingService.reactivateListing(100L, 1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("sold");
        }
    }

    @Nested
    @DisplayName("deleteListing()")
    class DeleteListing {

        @Test
        @DisplayName("TC-LST-014: Should delete listing by owner")
        void shouldDelete() {
            when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));

            listingService.deleteListing(100L, 1L);

            verify(listingRepository).delete(listing);
        }

        @Test
        @DisplayName("TC-LST-015: Should reject delete by non-owner")
        void shouldRejectNonOwner() {
            when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));

            assertThatThrownBy(() -> listingService.deleteListing(100L, 2L))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }
}