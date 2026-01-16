package com.gemstore.backend.mappers.notification;

import com.gemstore.backend.dtos.notification.NotificationResponse;
import com.gemstore.backend.entities.listing.Listing;
import com.gemstore.backend.entities.notification.Notification;
import com.gemstore.backend.entities.user.User;
import org.mapstruct.*;
import org.springframework.util.StringUtils;

/**
 * NotificationMapper converts Notification entity to NotificationResponse DTO.
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface NotificationMapper {

    /* ========================= Notification -> NotificationResponse ========================= */

    @Mapping(target = "type", source = "type")
    @Mapping(target = "actor", expression = "java(notification.getActor() != null ? toActorDto(notification.getActor()) : null)")
    @Mapping(target = "listing", expression = "java(notification.getListing() != null ? toListingDto(notification.getListing()) : null)")
    NotificationResponse toResponse(Notification notification);

    /* ========================= Supporting mappers ========================= */

    default NotificationResponse.ActorDto toActorDto(User user) {
        if (user == null) return null;
        return NotificationResponse.ActorDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(StringUtils.hasText(user.getDisplayName()) ? user.getDisplayName() : user.getUsername())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    default NotificationResponse.ListingDto toListingDto(Listing listing) {
        if (listing == null) return null;
        return NotificationResponse.ListingDto.builder()
                .id(listing.getId())
                .title(listing.getTitle())
                .primaryImageUrl(listing.getPrimaryImageUrl())
                .build();
    }
}