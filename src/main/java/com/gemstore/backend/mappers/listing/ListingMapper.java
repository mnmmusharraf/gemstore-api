package com.gemstore.backend. mappers.listing;

import com.gemstore.backend.dtos.listing.request.CreateListingRequest;
import com. gemstore.backend.dtos. listing.request.UpdateListingRequest;
import com.gemstore. backend.dtos.listing.response.*;
import com.gemstore.backend. dtos.user.PublicUserDTO;
import com. gemstore.backend.entities.listing. Listing;
import com.gemstore.backend.entities.listing. ListingImage;
import com. gemstore.backend.entities.listing.lookup.*;
import com.gemstore.backend.entities.user.User;
import org.mapstruct.*;

import java.util.List;

/**
 * ListingMapper converts between Listing entity and various DTO layers.
 *
 * Design notes:
 * - CreateListingRequest:  maps basic fields, lookup IDs resolved in service layer.
 * - UpdateListingRequest: partial update with IGNORE null strategy.
 * - ListingCardResponse: lightweight for grid/list views.
 * - ListingResponse: standard response with nested lookups.
 * - ListingDetailResponse: full details with price history and related listings.
 * - Seller info mapped separately to control exposed fields.
 * - Images mapped via ListingImageMapper.
 * - User context (isFavorited, isOwner) set in service layer via @AfterMapping or manually.
 */
@Mapper(
        componentModel = "spring",
        uses = {LookupMapper.class, ListingImageMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy. IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ListingMapper {

    /* ===================== Entity -> ListingCardResponse ===================== */

    /**
     * Lightweight DTO for listing cards in grid/list views.
     * Only essential fields + primary image.
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "listingNumber", source = "listingNumber")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "imageUrl", expression = "java(getPrimaryImageUrl(entity))")
    @Mapping(target = "gemstoneType", source = "gemstoneType.name")
    @Mapping(target = "caratWeight", source = "caratWeight")
    @Mapping(target = "color", source = "color.name")
    @Mapping(target = "origin", source = "origin.name")
    @Mapping(target = "price", source = "price")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "pricePerCarat", source = "pricePerCarat")
    @Mapping(target = "viewsCount", source = "viewsCount")
    @Mapping(target = "favoritesCount", source = "favoritesCount")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "isSold", source = "isSold")
    @Mapping(target = "sellerId", source = "seller.id")
    @Mapping(target = "sellerName", source = "seller.displayName")
    @Mapping(target = "sellerAvatar", source = "seller.avatarUrl")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "isFavorited", ignore = true) // Set in service
    ListingCardResponse toCardResponse(Listing entity);

    List<ListingCardResponse> toCardResponseList(List<Listing> entities);

    /* ===================== Entity -> ListingResponse ===================== */

    /**
     * Standard listing response with all details and nested lookups.
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "listingNumber", source = "listingNumber")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "gemstoneType", source = "gemstoneType")
    @Mapping(target = "caratWeight", source = "caratWeight")
    @Mapping(target = "color", source = "color")
    @Mapping(target = "colorQuality", source = "colorQuality")
    @Mapping(target = "clarity", source = "clarity")
    @Mapping(target = "cut", source = "cut")
    @Mapping(target = "origin", source = "origin")
    @Mapping(target = "treatment", source = "treatment")
    @Mapping(target = "lengthMm", source = "lengthMm")
    @Mapping(target = "widthMm", source = "widthMm")
    @Mapping(target = "depthMm", source = "depthMm")
    @Mapping(target = "price", source = "price")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "pricePerCarat", source = "pricePerCarat")
    @Mapping(target = "isCertified", source = "isCertified")
    @Mapping(target = "certificateInfo", source = "certificateInfo")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "completenessScore", source = "completenessScore")
    @Mapping(target = "viewsCount", source = "viewsCount")
    @Mapping(target = "favoritesCount", source = "favoritesCount")
    @Mapping(target = "isSold", source = "isSold")
    @Mapping(target = "soldPrice", source = "soldPrice")
    @Mapping(target = "soldAt", source = "soldAt")
    @Mapping(target = "images", source = "images")
    @Mapping(target = "seller", expression = "java(toSellerInfo(entity. getSeller()))")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "isFavorited", ignore = true) // Set in service
    ListingResponse toResponse(Listing entity);

    List<ListingResponse> toResponseList(List<Listing> entities);

    /* ===================== Entity -> ListingDetailResponse ===================== */

    /**
     * Full listing details for single listing page.
     * Includes price history and related listings (set in service).
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "listingNumber", source = "listingNumber")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "gemstoneType", source = "gemstoneType")
    @Mapping(target = "caratWeight", source = "caratWeight")
    @Mapping(target = "color", source = "color")
    @Mapping(target = "colorQuality", source = "colorQuality")
    @Mapping(target = "clarity", source = "clarity")
    @Mapping(target = "cut", source = "cut")
    @Mapping(target = "origin", source = "origin")
    @Mapping(target = "treatment", source = "treatment")
    @Mapping(target = "lengthMm", source = "lengthMm")
    @Mapping(target = "widthMm", source = "widthMm")
    @Mapping(target = "depthMm", source = "depthMm")
    @Mapping(target = "price", source = "price")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "pricePerCarat", source = "pricePerCarat")
    @Mapping(target = "isCertified", source = "isCertified")
    @Mapping(target = "certificateInfo", source = "certificateInfo")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "completenessScore", source = "completenessScore")
    @Mapping(target = "viewsCount", source = "viewsCount")
    @Mapping(target = "favoritesCount", source = "favoritesCount")
    @Mapping(target = "isSold", source = "isSold")
    @Mapping(target = "soldPrice", source = "soldPrice")
    @Mapping(target = "soldAt", source = "soldAt")
    @Mapping(target = "daysToSell", source = "daysToSell")
    @Mapping(target = "images", source = "images")
    @Mapping(target = "seller", expression = "java(toDetailSellerInfo(entity.getSeller()))")
    @Mapping(target = "priceHistory", ignore = true) // Set in service
    @Mapping(target = "relatedListings", ignore = true) // Set in service
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "isFavorited", ignore = true) // Set in service
    @Mapping(target = "isOwner", ignore = true) // Set in service
    ListingDetailResponse toDetailResponse(Listing entity);

    /* ===================== DTO -> Entity (Creation) ===================== */

    /**
     * Maps create request to a new Listing entity.
     * Service layer should: 
     *   1) Call mapper
     *   2) Resolve lookup IDs to entities
     *   3) Set seller from authenticated user
     *   4) Handle images separately
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "listingNumber", ignore = true) // Auto-generated
    @Mapping(target = "seller", ignore = true) // Set in service
    @Mapping(target = "title", source = "title")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "gemstoneType", ignore = true) // Resolved in service
    @Mapping(target = "caratWeight", source = "caratWeight")
    @Mapping(target = "color", ignore = true) // Resolved in service
    @Mapping(target = "colorQuality", ignore = true) // Resolved in service
    @Mapping(target = "clarity", ignore = true) // Resolved in service
    @Mapping(target = "cut", ignore = true) // Resolved in service
    @Mapping(target = "origin", ignore = true) // Resolved in service
    @Mapping(target = "treatment", ignore = true) // Resolved in service
    @Mapping(target = "lengthMm", source = "lengthMm")
    @Mapping(target = "widthMm", source = "widthMm")
    @Mapping(target = "depthMm", source = "depthMm")
    @Mapping(target = "price", source = "price")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "pricePerCarat", ignore = true) // Auto-calculated by DB
    @Mapping(target = "isCertified", source = "isCertified")
    @Mapping(target = "certificateInfo", source = "certificateInfo")
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "viewsCount", constant = "0")
    @Mapping(target = "favoritesCount", constant = "0")
    @Mapping(target = "completenessScore", ignore = true) // Auto-calculated by trigger
    @Mapping(target = "isSold", constant = "false")
    @Mapping(target = "soldPrice", ignore = true)
    @Mapping(target = "soldAt", ignore = true)
    @Mapping(target = "daysToSell", ignore = true)
    @Mapping(target = "images", ignore = true) // Handled separately
    @Mapping(target = "favorites", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    //@Mapping(target = "searchVector", ignore = true)
    Listing toEntity(CreateListingRequest request);

    /* ===================== Partial Update ===================== */

    /**
     * Applies non-null fields from UpdateListingRequest onto existing Listing.
     * Null fields are ignored (due to IGNORE strategy).
     * Service layer should:
     *   1) Fetch existing entity
     *   2) Call mapper to apply updates
     *   3) Resolve any changed lookup IDs
     *   4) Handle images separately if changed
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy. IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "listingNumber", ignore = true)
    @Mapping(target = "seller", ignore = true)
    @Mapping(target = "title", source = "title")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "gemstoneType", ignore = true) // Resolved in service if changed
    @Mapping(target = "caratWeight", source = "caratWeight")
    @Mapping(target = "color", ignore = true) // Resolved in service if changed
    @Mapping(target = "colorQuality", ignore = true) // Resolved in service if changed
    @Mapping(target = "clarity", ignore = true) // Resolved in service if changed
    @Mapping(target = "cut", ignore = true) // Resolved in service if changed
    @Mapping(target = "origin", ignore = true) // Resolved in service if changed
    @Mapping(target = "treatment", ignore = true) // Resolved in service if changed
    @Mapping(target = "lengthMm", source = "lengthMm")
    @Mapping(target = "widthMm", source = "widthMm")
    @Mapping(target = "depthMm", source = "depthMm")
    @Mapping(target = "price", source = "price")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "pricePerCarat", ignore = true)
    @Mapping(target = "isCertified", source = "isCertified")
    @Mapping(target = "certificateInfo", source = "certificateInfo")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "viewsCount", ignore = true)
    @Mapping(target = "favoritesCount", ignore = true)
    @Mapping(target = "completenessScore", ignore = true)
    @Mapping(target = "isSold", ignore = true)
    @Mapping(target = "soldPrice", ignore = true)
    @Mapping(target = "soldAt", ignore = true)
    @Mapping(target = "daysToSell", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "favorites", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    //@Mapping(target = "searchVector", ignore = true)
    void updateListingFromRequest(UpdateListingRequest request, @MappingTarget Listing listing);

    /* ===================== Helper Methods ===================== */

    /**
     * Extracts primary image URL from listing images.
     */
    default String getPrimaryImageUrl(Listing entity) {
        if (entity.getImages() == null || entity.getImages().isEmpty()) {
            return null;
        }
        return entity.getImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .findFirst()
                .map(ListingImage::getImageUrl)
                .orElseGet(() -> entity.getImages().get(0).getImageUrl());
    }

    /**
     * Maps User to SellerInfo for ListingResponse.
     */
    default ListingResponse. SellerInfo toSellerInfo(User seller) {
        if (seller == null) {
            return null;
        }
        return ListingResponse.SellerInfo.builder()
                .id(seller.getId())
                .displayName(seller.getDisplayName())
                .username(seller.getUsername())
                .avatarUrl(seller.getAvatarUrl())
                .listingsCount(null) // Set in service if needed
                .build();
    }

    /**
     * Maps User to detailed SellerInfo for ListingDetailResponse.
     */
    default ListingDetailResponse.SellerInfo toDetailSellerInfo(User seller) {
        if (seller == null) {
            return null;
        }
        return ListingDetailResponse.SellerInfo.builder()
                .id(seller.getId())
                .displayName(seller.getDisplayName())
                .username(seller.getUsername())
                .avatarUrl(seller.getAvatarUrl())
                .totalListings(null) // Set in service
                .soldListings(null) // Set in service
                .memberSince(seller.getCreatedAt() != null
                        ? java.time.LocalDateTime.ofInstant(seller.getCreatedAt(), java.time.ZoneId. systemDefault())
                        : null)
                .build();
    }

    /* ===================== After-Mapping Hooks ===================== */

    /**
     * Optional hook to set default values after mapping.
     */
    @AfterMapping
    default void afterCreateMapping(@MappingTarget Listing listing) {
        if (listing.getCurrency() == null) {
            listing.setCurrency("LKR");
        }
        if (listing.getIsCertified() == null) {
            listing.setIsCertified(false);
        }
    }

    /* ===================== Context for Service Layer ===================== */

    /**
     * Context interface for passing additional data during mapping.
     * Service can implement this to provide user-specific data.
     */
    interface ListingMappingContext {
        boolean isFavorited(Long listingId, Long userId);
        boolean isOwner(Long listingId, Long userId);
    }
}