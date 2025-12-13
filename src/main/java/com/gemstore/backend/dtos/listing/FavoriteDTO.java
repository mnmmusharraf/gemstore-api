package com.gemstore.backend.dtos.listing;

import com.gemstore.backend. dtos.listing.response.ListingCardResponse;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteDTO {

    private Long id;
    private Long userId;
    private ListingCardResponse listing;
    private LocalDateTime createdAt;
}