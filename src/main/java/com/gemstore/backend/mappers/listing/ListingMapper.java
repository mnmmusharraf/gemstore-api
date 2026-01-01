package com.gemstore. backend.mappers.listing;

import com.gemstore.backend. dtos.listing.request.CreateListingRequest;
import com.gemstore.backend. dtos.listing.request.UpdateListingRequest;
import com.gemstore.backend. dtos.listing.response.*;
import com.gemstore.backend. dtos.user.PublicUserDTO;
import com. gemstore.backend.entities.listing.Listing;
import com. gemstore.backend.entities.listing.ListingImage;
import com.gemstore.backend.entities. listing.lookup.*;
import com.gemstore.backend.entities.user.User;
import org.mapstruct.*;

import java.util. Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ListingMapper converts between Listing entity and various DTO layers.
 */
@Mapper(
        componentModel = "spring",
        uses = {LookupMapper.class, ListingImageMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ListingMapper {

    /* ===================== Entity -> ListingCardResponse ===================== */

    @Mapping(target = "id", source = "id")
    @Mapping(target = "listingNumber", source = "listingNumber")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "imageUrls", expression = "java(getAllImageUrls(entity))")
    @Mapping(target = "primaryImageUrl", expression = "java(getPrimaryImageUrl(entity))")
    @Mapping(target = "gemstoneType", source = "gemstoneType.name")
    @Mapping(target = "caratWeight", source = "caratWeight")
    @Mapping(target = "color", source = "color.name")
    @Mapping(target = "colorQuality", source = "colorQuality.name")  // ADD
    @Mapping(target = "clarity", source = "clarity.name")            // ADD
    @Mapping(target = "cut", source = "cut.name")                    // ADD
    @Mapping(target = "origin", source = "origin.name")
    @Mapping(target = "treatment", source = "treatment.name")        // ADD
    @Mapping(target = "price", source = "price")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "pricePerCarat", source = "pricePerCarat")
    @Mapping(target = "viewsCount", source = "viewsCount")
    @Mapping(target = "likesCount", source = "likesCount")
    @Mapping(target = "favoritesCount", source = "favoritesCount")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "isSold", source = "isSold")
    @Mapping(target = "isCertified", source = "isCertified")         // ADD
    @Mapping(target = "sellerId", source = "seller.id")
    @Mapping(target = "sellerName", source = "seller.displayName")
    @Mapping(target = "sellerAvatar", source = "seller.avatarUrl")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "isLiked", ignore = true)
    @Mapping(target = "isFavorited", ignore = true)
    @Mapping(target = "lengthMm", source = "lengthMm")
    @Mapping(target = "widthMm", source = "widthMm")
    @Mapping(target = "depthMm", source = "depthMm")
    ListingCardResponse toCardResponse(Listing entity);

    List<ListingCardResponse> toCardResponseList(List<Listing> entities);

    /* ===================== Entity -> ListingResponse ===================== */

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
    @Mapping(target = "seller", expression = "java(toSellerInfo(entity.getSeller()))")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "isFavorited", ignore = true)
    @Mapping(target = "likesCount", source = "likesCount")
    @Mapping(target = "isLiked", ignore = true)
    ListingResponse toResponse(Listing entity);

    List<ListingResponse> toResponseList(List<Listing> entities);

    /* ===================== Entity -> ListingDetailResponse ===================== */

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
    @Mapping(target = "priceHistory", ignore = true)
    @Mapping(target = "relatedListings", ignore = true)
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "isFavorited", ignore = true)
    @Mapping(target = "isOwner", ignore = true)
    ListingDetailResponse toDetailResponse(Listing entity);

    /* ===================== DTO -> Entity (Creation) ===================== */

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "listingNumber", ignore = true)
    @Mapping(target = "seller", ignore = true)
    @Mapping(target = "title", source = "title")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "gemstoneType", ignore = true)
    @Mapping(target = "caratWeight", source = "caratWeight")
    @Mapping(target = "color", ignore = true)
    @Mapping(target = "colorQuality", ignore = true)
    @Mapping(target = "clarity", ignore = true)
    @Mapping(target = "cut", ignore = true)
    @Mapping(target = "origin", ignore = true)
    @Mapping(target = "treatment", ignore = true)
    @Mapping(target = "lengthMm", source = "lengthMm")
    @Mapping(target = "widthMm", source = "widthMm")
    @Mapping(target = "depthMm", source = "depthMm")
    @Mapping(target = "price", source = "price")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "pricePerCarat", ignore = true)
    @Mapping(target = "isCertified", source = "isCertified")
    @Mapping(target = "certificateInfo", source = "certificateInfo")
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "viewsCount", constant = "0")
    @Mapping(target = "favoritesCount", constant = "0")
    @Mapping(target = "completenessScore", ignore = true)
    @Mapping(target = "isSold", constant = "false")
    @Mapping(target = "soldPrice", ignore = true)
    @Mapping(target = "soldAt", ignore = true)
    @Mapping(target = "daysToSell", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "favorites", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Listing toEntity(CreateListingRequest request);

    /* ===================== Partial Update ===================== */

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "listingNumber", ignore = true)
    @Mapping(target = "seller", ignore = true)
    @Mapping(target = "title", source = "title")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "gemstoneType", ignore = true)
    @Mapping(target = "caratWeight", source = "caratWeight")
    @Mapping(target = "color", ignore = true)
    @Mapping(target = "colorQuality", ignore = true)
    @Mapping(target = "clarity", ignore = true)
    @Mapping(target = "cut", ignore = true)
    @Mapping(target = "origin", ignore = true)
    @Mapping(target = "treatment", ignore = true)
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
    void updateListingFromRequest(UpdateListingRequest request, @MappingTarget Listing listing);

    /* ===================== Helper Methods ===================== */

    /**
     * Gets ALL image URLs sorted by display order (for carousel).
     */
    default List<String> getAllImageUrls(Listing entity) {
        if (entity.getImages() == null || entity.getImages().isEmpty()) {
            return List.of();
        }
        return entity. getImages().stream()
                .sorted(Comparator.comparing(ListingImage::getDisplayOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(ListingImage:: getImageUrl)
                .collect(Collectors.toList());
    }

    /**
     * Extracts primary image URL from listing images.
     */
    default String getPrimaryImageUrl(Listing entity) {
        if (entity. getImages() == null || entity.getImages().isEmpty()) {
            return null;
        }
        return entity.getImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .findFirst()
                .map(ListingImage::getImageUrl)
                .orElseGet(() -> {
                    // Fallback to first image if no primary is set
                    return entity.getImages().stream()
                            .sorted(Comparator.comparing(ListingImage::getDisplayOrder,
                                    Comparator. nullsLast(Comparator.naturalOrder())))
                            .findFirst()
                            . map(ListingImage::getImageUrl)
                            .orElse(null);
                });
    }

    /**
     * Maps User to SellerInfo for ListingResponse.
     */
    default ListingResponse. SellerInfo toSellerInfo(User seller) {
        if (seller == null) {
            return null;
        }
        return ListingResponse.SellerInfo. builder()
                .id(seller.getId())
                .displayName(seller.getDisplayName())
                .username(seller.getUsername())
                .avatarUrl(seller.getAvatarUrl())
                .listingsCount(null)
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
                .avatarUrl(seller. getAvatarUrl())
                .totalListings(null)
                .soldListings(null)
                .memberSince(seller.getCreatedAt() != null
                        ? java.time.LocalDateTime.ofInstant(seller.getCreatedAt(), java.time.ZoneId. systemDefault())
                        : null)
                .build();
    }

    /* ===================== After-Mapping Hooks ===================== */

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

    interface ListingMappingContext {
        boolean isFavorited(Long listingId, Long userId);
        boolean isOwner(Long listingId, Long userId);
    }
}