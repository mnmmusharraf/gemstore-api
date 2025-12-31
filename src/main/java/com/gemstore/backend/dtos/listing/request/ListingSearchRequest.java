package com.gemstore.backend. dtos.listing.request;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingSearchRequest {

    // Text search
    private String query;

    // Filters
    private Integer gemstoneTypeId;
    private Integer colorId;
    private Integer colorQualityId;
    private Integer clarityId;
    private Integer cutId;
    private Integer originId;
    private Integer treatmentId;

    // Price Range
    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    // Carat Range
    private BigDecimal minCarat;
    private BigDecimal maxCarat;

    // Sorting
    private String sortBy = "createdAt";    // createdAt, price, caratWeight
    private String sortDirection = "DESC";  // ASC, DESC

    // Pagination
    private Integer page = 0;
    private Integer size = 20;
}