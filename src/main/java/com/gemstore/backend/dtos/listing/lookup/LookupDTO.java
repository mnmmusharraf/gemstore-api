package com.gemstore. backend.dtos.listing.lookup;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LookupDTO {

    private Integer id;
    private String name;

    // Optional fields (only some lookups have these)
    private String category;
    private Integer rank;
}