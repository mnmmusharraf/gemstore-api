package com.gemstore.backend.services.listing;

import com.gemstore.backend. dtos.common.PageResponse;
import com.gemstore.backend.dtos.listing. request.*;
import com.gemstore.backend.dtos.listing.response.*;
import com.gemstore.backend.entities.user.User;
import com.gemstore.backend.entities.listing.*;
import com.gemstore.backend.exceptions.ResourceNotFoundException;
import com. gemstore.backend.exceptions.UnauthorizedException;
import com. gemstore.backend.mappers.listing.ListingMapper;
import com.gemstore.backend.mappers.listing.ListingPriceHistoryMapper;
import com.gemstore.backend.repositories.listing.*;
import com.gemstore.backend.repositories.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math. BigDecimal;
import java. time.LocalDateTime;
import java.util. List;
import java.util. Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ListingService {

    private final ListingRepository listingRepository;
    private final ListingImageRepository listingImageRepository;
    private final ListingPriceHistoryRepository priceHistoryRepository;
    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;

    private final LookupService lookupService;
    private final ListingImageService listingImageService;
    private final ListingViewService listingViewService;
    private final LikeService likeService;           // NEW
    private final FavoriteService favoriteService;   // NEW

    private final ListingMapper listingMapper;
    private final ListingPriceHistoryMapper priceHistoryMapper;

    /* ===================== Create Listing ===================== */

    @Transactional
    public ListingResponse createListing(CreateListingRequest request, Long sellerId) {
        log.info("Creating listing for seller: {}", sellerId);

        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + sellerId));

        Listing listing = listingMapper.toEntity(request);
        listing.setSeller(seller);

        resolveLookupEntities(listing, request.getGemstoneTypeId(), request.getColorId(),
                request.getColorQualityId(), request.getClarityId(), request.getCutId(),
                request.getOriginId(), request.getTreatmentId());

        listing = listingRepository. save(listing);

        listing. setListingNumber(generateListingNumber(listing.getId()));
        listing = listingRepository.save(listing);

        if (request.getImageUrls() != null && ! request.getImageUrls().isEmpty()) {
            listingImageService.addImages(listing, request. getImageUrls(), request.getPrimaryImageIndex());
        }

        log.info("Created listing: {}", listing.getListingNumber());
        return listingMapper.toResponse(listing);
    }

    /* ===================== Update Listing ===================== */

    @Transactional
    public ListingResponse updateListing(Long listingId, UpdateListingRequest request, Long userId) {
        log.info("Updating listing: {} by user: {}", listingId, userId);

        Listing listing = getListingEntity(listingId);

        if (!listing.getSeller().getId().equals(userId)) {
            throw new UnauthorizedException("You can only update your own listings");
        }

        if (listing.getIsSold()) {
            throw new IllegalStateException("Cannot update a sold listing");
        }

        listingMapper.updateListingFromRequest(request, listing);

        if (request.getGemstoneTypeId() != null) {
            listing.setGemstoneType(lookupService.getGemstoneTypeById(request.getGemstoneTypeId()));
        }
        if (request.getColorId() != null) {
            listing. setColor(lookupService.getColorById(request.getColorId()));
        }
        if (request.getColorQualityId() != null) {
            listing.setColorQuality(lookupService.getColorQualityById(request.getColorQualityId()));
        }
        if (request.getClarityId() != null) {
            listing. setClarity(lookupService. getClarityGradeById(request.getClarityId()));
        }
        if (request. getCutId() != null) {
            listing.setCut(lookupService.getCutById(request.getCutId()));
        }
        if (request.getOriginId() != null) {
            listing.setOrigin(lookupService.getOriginById(request.getOriginId()));
        }
        if (request. getTreatmentId() != null) {
            listing.setTreatment(lookupService. getTreatmentById(request. getTreatmentId()));
        }

        if (request.getImageUrls() != null) {
            listingImageService. updateImages(listing, request.getImageUrls(), request.getPrimaryImageIndex());
        }

        listing = listingRepository.save(listing);

        log.info("Updated listing: {}", listing.getListingNumber());
        return listingMapper.toResponse(listing);
    }

    /* ===================== Get Listing ===================== */

    @Transactional(readOnly = true)
    public ListingResponse getListingById(Long listingId, Long userId) {
        Listing listing = getListingEntity(listingId);

        ListingResponse response = listingMapper.toResponse(listing);

        if (userId != null) {
            response.setIsLiked(likeService.isLiked(listingId, userId));
            response.setIsFavorited(favoriteService.isFavorited(listingId, userId));
        }

        return response;
    }

    @Transactional
    public ListingDetailResponse getListingDetail(Long listingId, Long userId) {
        Listing listing = getListingEntity(listingId);

        if (userId != null) {
            listingViewService.recordView(listingId, userId);
        } else {
            listingViewService.recordAnonymousView(listingId);
        }

        ListingDetailResponse response = listingMapper.toDetailResponse(listing);

        List<ListingPriceHistory> priceHistory = priceHistoryRepository
                .findByListingIdOrderByCreatedAtDesc(listingId);
        response.setPriceHistory(priceHistoryMapper.toDTOList(priceHistory));

        List<Listing> relatedListings = findRelatedListings(listing, 4);
        response.setRelatedListings(listingMapper.toCardResponseList(relatedListings));

        if (userId != null) {
            response.setIsLiked(likeService.isLiked(listingId, userId));
            response.setIsFavorited(favoriteService. isFavorited(listingId, userId));
            response.setIsOwner(listing.getSeller().getId().equals(userId));
        }

        if (response.getSeller() != null) {
            long totalListings = listingRepository.countBySellerIdAndStatus(
                    listing.getSeller().getId(), ListingStatus.ACTIVE);
            response.getSeller().setTotalListings((int) totalListings);
        }

        return response;
    }

    /* ===================== Search & List ===================== */

    @Transactional(readOnly = true)
    public PageResponse<ListingCardResponse> getActiveListings(int page, int size, Long userId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction. DESC, "createdAt"));
        Page<Listing> listings = listingRepository.findByStatusOrderByCreatedAtDesc(
                ListingStatus.ACTIVE, pageable);

        return toPageResponse(listings, userId);
    }

    @Transactional(readOnly = true)
    public PageResponse<ListingCardResponse> searchListings(ListingSearchRequest request, Long userId) {
        Pageable pageable = createPageable(request);

        Page<Listing> listings;

        if (request.getQuery() != null && !request.getQuery().isBlank()) {
            // Use LIKE-based search (works without search_vector column)
            listings = listingRepository.searchByLike(request.getQuery(), pageable);
        } else {
            listings = listingRepository.searchListings(
                    request.getGemstoneTypeId(),
                    request.getColorId(),
                    request.getOriginId(),
                    request.getMinPrice(),
                    request.getMaxPrice(),
                    request.getMinCarat(),
                    request.getMaxCarat(),
                    pageable
            );
        }

        return toPageResponse(listings, userId);
    }

    @Transactional(readOnly = true)
    public PageResponse<ListingCardResponse> getListingsBySeller(Long sellerId, int page, int size, Long userId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Listing> listings = listingRepository.findBySellerIdAndStatus(
                sellerId, ListingStatus. ACTIVE, pageable);

        return toPageResponse(listings, userId);
    }

    @Transactional(readOnly = true)
    public PageResponse<ListingCardResponse> getMyListings(Long userId, String status, int page, int size) {
        Pageable pageable = PageRequest. of(page, size, Sort. by(Sort.Direction.DESC, "createdAt"));

        Page<Listing> listings;
        if (status != null && !status.isBlank()) {
            ListingStatus listingStatus = ListingStatus.valueOf(status.toUpperCase());
            listings = listingRepository.findBySellerIdAndStatus(userId, listingStatus, pageable);
        } else {
            listings = listingRepository.findBySellerId(userId, pageable);
        }

        return toPageResponse(listings, userId);
    }

    /* ===================== Status Management ===================== */

    @Transactional
    public ListingResponse markAsSold(Long listingId, BigDecimal soldPrice, Long userId) {
        log.info("Marking listing as sold: {}", listingId);

        Listing listing = getListingEntity(listingId);

        if (!listing.getSeller().getId().equals(userId)) {
            throw new UnauthorizedException("You can only update your own listings");
        }

        if (listing.getIsSold()) {
            throw new IllegalStateException("Listing is already sold");
        }

        listing.setIsSold(true);
        listing.setSoldPrice(soldPrice != null ? soldPrice : listing.getPrice());
        listing.setSoldAt(LocalDateTime.now());
        listing. setStatus(ListingStatus. SOLD);

        if (listing.getCreatedAt() != null) {
            long days = java.time.Duration.between(
                    listing.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant(),
                    java.time. Instant.now()
            ).toDays();
            listing.setDaysToSell((int) days);
        }

        listing = listingRepository.save(listing);

        log.info("Listing marked as sold: {}", listing. getListingNumber());
        return listingMapper.toResponse(listing);
    }

    @Transactional
    public void archiveListing(Long listingId, Long userId) {
        log.info("Archiving listing: {}", listingId);

        Listing listing = getListingEntity(listingId);

        if (!listing.getSeller().getId().equals(userId)) {
            throw new UnauthorizedException("You can only archive your own listings");
        }

        listing.setStatus(ListingStatus.ARCHIVED);
        listingRepository.save(listing);

        log.info("Listing archived: {}", listing.getListingNumber());
    }

    @Transactional
    public ListingResponse reactivateListing(Long listingId, Long userId) {
        log.info("Reactivating listing: {}", listingId);

        Listing listing = getListingEntity(listingId);

        if (!listing.getSeller().getId().equals(userId)) {
            throw new UnauthorizedException("You can only reactivate your own listings");
        }

        if (listing.getIsSold()) {
            throw new IllegalStateException("Cannot reactivate a sold listing");
        }

        listing.setStatus(ListingStatus.ACTIVE);
        listing = listingRepository.save(listing);

        log.info("Listing reactivated: {}", listing. getListingNumber());
        return listingMapper.toResponse(listing);
    }

    @Transactional
    public void deleteListing(Long listingId, Long userId) {
        log.info("Deleting listing: {}", listingId);

        Listing listing = getListingEntity(listingId);

        if (!listing.getSeller().getId().equals(userId)) {
            throw new UnauthorizedException("You can only delete your own listings");
        }

        listingRepository.delete(listing);

        log.info("Listing deleted: {}", listingId);
    }

    /* ===================== Helper Methods ===================== */

    private Listing getListingEntity(Long listingId) {
        return listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found: " + listingId));
    }

    private void resolveLookupEntities(Listing listing, Integer gemstoneTypeId, Integer colorId,
                                       Integer colorQualityId, Integer clarityId, Integer cutId,
                                       Integer originId, Integer treatmentId) {
        listing.setGemstoneType(lookupService.getGemstoneTypeById(gemstoneTypeId));

        if (colorId != null) {
            listing.setColor(lookupService.getColorById(colorId));
        }
        if (colorQualityId != null) {
            listing.setColorQuality(lookupService.getColorQualityById(colorQualityId));
        }
        if (clarityId != null) {
            listing.setClarity(lookupService.getClarityGradeById(clarityId));
        }
        if (cutId != null) {
            listing.setCut(lookupService.getCutById(cutId));
        }
        if (originId != null) {
            listing.setOrigin(lookupService.getOriginById(originId));
        }
        if (treatmentId != null) {
            listing.setTreatment(lookupService.getTreatmentById(treatmentId));
        }
    }

    private String generateListingNumber(Long id) {
        return "GEM-" + java.time.Year.now().getValue() + "-" + String.format("%06d", id);
    }

    private List<Listing> findRelatedListings(Listing listing, int limit) {
        Pageable pageable = PageRequest. of(0, limit);
        return listingRepository.findByGemstoneTypeIdAndStatus(
                        listing.getGemstoneType().getId(),
                        ListingStatus.ACTIVE,
                        pageable
                ).stream()
                .filter(l -> !l.getId().equals(listing.getId()))
                .toList();
    }

    private Pageable createPageable(ListingSearchRequest request) {
        Sort sort = Sort.by(
                "ASC".equalsIgnoreCase(request.getSortDirection())
                        ? Sort.Direction. ASC
                        : Sort. Direction.DESC,
                request.getSortBy() != null ? request.getSortBy() : "createdAt"
        );
        return PageRequest.of(
                request.getPage() != null ? request.getPage() : 0,
                request.getSize() != null ? request.getSize() : 20,
                sort
        );
    }

    /**
     * Convert Page to PageResponse with user context (isLiked, isFavorited)
     */
    private PageResponse<ListingCardResponse> toPageResponse(Page<Listing> page, Long userId) {
        List<ListingCardResponse> content = listingMapper.toCardResponseList(page.getContent());

        // Set isLiked and isFavorited for current user (batch queries for efficiency)
        if (userId != null && !content.isEmpty()) {
            List<Long> listingIds = content.stream()
                    .map(ListingCardResponse::getId)
                    .toList();

            Set<Long> likedIds = likeService.getLikedListingIds(userId, listingIds);
            Set<Long> favoritedIds = favoriteService.getFavoritedListingIds(userId, listingIds);

            content. forEach(card -> {
                card.setIsLiked(likedIds.contains(card.getId()));
                card.setIsFavorited(favoritedIds. contains(card.getId()));
            });
        }

        return PageResponse.<ListingCardResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page. getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }
}