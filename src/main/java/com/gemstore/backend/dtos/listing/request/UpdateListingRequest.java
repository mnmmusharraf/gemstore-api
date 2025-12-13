package com.gemstore.backend.dtos.listing.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java. math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateListingRequest {

    @Size(min = 5, max = 255, message = "Title must be between 5 and 255 characters")
    private String title;

    @Size(max = 5000, message = "Description cannot exceed 5000 characters")
    private String description;

    // Gemstone Specs
    private Integer gemstoneTypeId;

    @DecimalMin(value = "0.01", message = "Carat weight must be at least 0.01")
    @DecimalMax(value = "10000", message = "Carat weight cannot exceed 10000")
    private BigDecimal caratWeight;

    @DecimalMin(value = "1", message = "Price must be at least 1")
    private BigDecimal price;

    private String currency;

    // Optional Fields
    private Integer colorId;
    private Integer colorQualityId;
    private Integer clarityId;
    private Integer cutId;
    private Integer originId;
    private Integer treatmentId;

    // Dimensions
    private BigDecimal lengthMm;
    private BigDecimal widthMm;
    private BigDecimal depthMm;

    // Certificate
    private Boolean isCertified;
    private String certificateInfo;

    // Images
    private List<String> imageUrls;
    private Integer primaryImageIndex;
}