package com.gemstore.backend.dtos.listing.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingImageResponse {

    private Long id;
    private String imageUrl;
    private Boolean isPrimary;
    private Integer displayOrder;
}