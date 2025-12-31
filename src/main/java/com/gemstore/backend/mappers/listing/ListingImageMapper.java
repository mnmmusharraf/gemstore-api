package com.gemstore.backend. mappers.listing;

import com.gemstore.backend.dtos.listing.response.ListingImageResponse;
import com.gemstore. backend.entities.listing.ListingImage;
import org.mapstruct.*;

import java.util. List;

/**
 * ListingImageMapper converts between ListingImage entity and DTOs.
 *
 * Design notes:
 * - Simple 1:1 mapping for image data.
 * - Used by ListingMapper for nested image lists.
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ListingImageMapper {

    /* ===================== Entity -> DTO ===================== */

    @Mapping(target = "id", source = "id")
    @Mapping(target = "imageUrl", source = "imageUrl")
    @Mapping(target = "isPrimary", source = "isPrimary")
    @Mapping(target = "displayOrder", source = "displayOrder")
    ListingImageResponse toDTO(ListingImage entity);

    List<ListingImageResponse> toDTOList(List<ListingImage> entities);

    /* ===================== DTO -> Entity ===================== */

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "listing", ignore = true)
    @Mapping(target = "imageUrl", source = "imageUrl")
    @Mapping(target = "isPrimary", source = "isPrimary")
    @Mapping(target = "displayOrder", source = "displayOrder")
    @Mapping(target = "createdAt", ignore = true)
    ListingImage toEntity(ListingImageResponse dto);
}