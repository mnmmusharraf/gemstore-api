package com.gemstore. backend.mappers.listing;

import com.gemstore.backend. dtos.listing.response.ListingDetailResponse;
import com.gemstore.backend.entities.listing.ListingPriceHistory;
import org.mapstruct.*;

import java.util.List;

/**
 * ListingPriceHistoryMapper converts price history entities to DTOs.
 *
 * Design notes: 
 * - Maps to nested PriceHistoryItem within ListingDetailResponse.
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ListingPriceHistoryMapper {

    /* ===================== Entity -> DTO ===================== */

    @Mapping(target = "oldPrice", source = "oldPrice")
    @Mapping(target = "newPrice", source = "newPrice")
    @Mapping(target = "changeReason", source = "changeReason")
    @Mapping(target = "changedAt", source = "createdAt")
    ListingDetailResponse.PriceHistoryItem toDTO(ListingPriceHistory entity);

    List<ListingDetailResponse.PriceHistoryItem> toDTOList(List<ListingPriceHistory> entities);
}