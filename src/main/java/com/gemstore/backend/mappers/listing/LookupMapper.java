package com.gemstore.backend. mappers.listing;

import com.gemstore.backend.dtos.listing.lookup.LookupDTO;
import com.gemstore.backend.entities.listing.lookup.*;
import org.mapstruct.*;

import java.util.List;

/**
 * LookupMapper converts between lookup entities and LookupDTO. 
 *
 * Design notes:
 * - All lookup entities share the same DTO structure for simplicity.
 * - Some lookups have additional fields (category, rank) that are conditionally mapped.
 * - Used by ListingMapper to map nested lookup references.
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface LookupMapper {

    /* ===================== GemstoneType ===================== */

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "rank", ignore = true)
    LookupDTO toDTO(GemstoneType entity);

    List<LookupDTO> toGemstoneTypeDTOList(List<GemstoneType> entities);

    /* ===================== Color ===================== */

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "rank", ignore = true)
    LookupDTO toDTO(Color entity);

    List<LookupDTO> toColorDTOList(List<Color> entities);

    /* ===================== ColorQuality ===================== */

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "rank", source = "rank")
    LookupDTO toDTO(ColorQuality entity);

    List<LookupDTO> toColorQualityDTOList(List<ColorQuality> entities);

    /* ===================== ClarityGrade ===================== */

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "rank", source = "rank")
    LookupDTO toDTO(ClarityGrade entity);

    List<LookupDTO> toClarityGradeDTOList(List<ClarityGrade> entities);

    /* ===================== Cut ===================== */

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "rank", ignore = true)
    LookupDTO toDTO(Cut entity);

    List<LookupDTO> toCutDTOList(List<Cut> entities);

    /* ===================== Origin ===================== */

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "rank", ignore = true)
    LookupDTO toDTO(Origin entity);

    List<LookupDTO> toOriginDTOList(List<Origin> entities);

    /* ===================== Treatment ===================== */

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "rank", ignore = true)
    LookupDTO toDTO(Treatment entity);

    List<LookupDTO> toTreatmentDTOList(List<Treatment> entities);
}