package com.gemstore.backend.mappers.listing;

import com.gemstore.backend. dtos.listing.FavoriteDTO;
import com.gemstore. backend.dtos.listing.response.ListingCardResponse;
import com.gemstore.backend.entities.listing.Favorite;
import org.mapstruct.*;

import java.util.List;

/**
 * FavoriteMapper converts between Favorite entity and DTOs.
 *
 * Design notes:
 * - FavoriteDTO includes the full ListingCardResponse for the favorited listing.
 * - Uses ListingMapper to convert nested listing.
 */
@Mapper(
        componentModel = "spring",
        uses = {ListingMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface FavoriteMapper {

    /* ===================== Entity -> DTO ===================== */

    @Mapping(target = "id", source = "id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "listing", source = "listing")
    @Mapping(target = "createdAt", source = "createdAt")
    FavoriteDTO toDTO(Favorite entity);

    List<FavoriteDTO> toDTOList(List<Favorite> entities);

    /* ===================== Custom Mapping ===================== */

    /**
     * Maps Favorite to ListingCardResponse directly.
     * Useful when you only need the listing card, not the full FavoriteDTO.
     */
    default ListingCardResponse toListingCard(Favorite entity, ListingMapper listingMapper) {
        if (entity == null || entity.getListing() == null) {
            return null;
        }
        ListingCardResponse card = listingMapper.toCardResponse(entity.getListing());
        card.setIsFavorited(true); // Always true since it's from favorites
        return card;
    }
}