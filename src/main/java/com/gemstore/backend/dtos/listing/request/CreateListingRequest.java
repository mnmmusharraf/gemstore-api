package com.gemstore. backend.dtos.listing.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateListingRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 255, message = "Title must be between 5 and 255 characters")
    private String title;

    @Size(max = 5000, message = "Description cannot exceed 5000 characters")
    private String description;

    // Required Fields
    @NotNull(message = "Gemstone type is required")
    private Integer gemstoneTypeId;

    @NotNull(message = "Carat weight is required")
    @DecimalMin(value = "0.01", message = "Carat weight must be at least 0.01")
    @DecimalMax(value = "10000", message = "Carat weight cannot exceed 10000")
    private BigDecimal caratWeight;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "1", message = "Price must be at least 1")
    private BigDecimal price;

    private String currency = "LKR";

    // Optional Fields (4 Cs)
    private Integer colorId;
    private Integer colorQualityId;
    private Integer clarityId;
    private Integer cutId;
    private Integer originId;
    private Integer treatmentId;

    // Dimensions (Optional)
    @DecimalMin(value = "0.1", message = "Length must be at least 0.1mm")
    private BigDecimal lengthMm;

    @DecimalMin(value = "0.1", message = "Width must be at least 0.1mm")
    private BigDecimal widthMm;

    @DecimalMin(value = "0.1", message = "Depth must be at least 0.1mm")
    private BigDecimal depthMm;

    // Certificate (Optional)
    private Boolean isCertified = false;

    @Size(max = 255, message = "Certificate info cannot exceed 255 characters")
    private String certificateInfo;

    // Images
    @Size(max = 10, message = "Maximum 10 images allowed")
    private List<String> imageUrls;

    private Integer primaryImageIndex = 0;
}