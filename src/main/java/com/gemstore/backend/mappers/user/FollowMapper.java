package com.gemstore.backend.mappers.user;

import com.gemstore.backend.dtos.user.UserSummaryResponse;
import com.gemstore.backend.entities.user.Follow;
import com.gemstore.backend.entities.user.User;
import org.mapstruct.*;

import java.util.List;

/**
 * FollowMapper converts between Follow/User entities and DTOs.
 *
 * Design notes:
 * - UserSummaryResponse is a lightweight DTO for displaying users in lists
 *   (followers, following, search results, etc.)
 * - isFollowing field is set dynamically in service layer (not mapped here)
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy. IGNORE
)
public interface FollowMapper {

    /* ===================== User -> UserSummaryResponse ===================== */

    /**
     * Maps User entity to a lightweight summary DTO.
     * Note: isFollowing is NOT mapped here - it's set in service based on context.
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "username", source = "username")
    @Mapping(target = "displayName", source = "displayName")
    @Mapping(target = "avatarUrl", source = "avatarUrl")
    @Mapping(target = "isFollowing", ignore = true) // Set in service layer
    UserSummaryResponse toUserSummary(User user);

    /**
     * Maps list of User entities to summary DTOs.
     */
    List<UserSummaryResponse> toUserSummaryList(List<User> users);

    /* ===================== Follow -> UserSummaryResponse ===================== */

    /**
     * Extract follower from Follow entity and map to summary.
     * Used when getting "followers" list.
     */
    @Mapping(target = "id", source = "follower.id")
    @Mapping(target = "username", source = "follower.username")
    @Mapping(target = "displayName", source = "follower.displayName")
    @Mapping(target = "avatarUrl", source = "follower.avatarUrl")
    @Mapping(target = "isFollowing", ignore = true) // Set in service layer
    UserSummaryResponse followerToUserSummary(Follow follow);

    /**
     * Extract following from Follow entity and map to summary.
     * Used when getting "following" list.
     */
    @Mapping(target = "id", source = "following.id")
    @Mapping(target = "username", source = "following.username")
    @Mapping(target = "displayName", source = "following.displayName")
    @Mapping(target = "avatarUrl", source = "following.avatarUrl")
    @Mapping(target = "isFollowing", ignore = true) // Set in service layer
    UserSummaryResponse followingToUserSummary(Follow follow);

    /**
     * Maps list of Follow entities to follower summaries.
     */
    default List<UserSummaryResponse> toFollowerSummaryList(List<Follow> follows) {
        if (follows == null) return null;
        return follows.stream()
                .map(this::followerToUserSummary)
                .toList();
    }

    /**
     * Maps list of Follow entities to following summaries.
     */
    default List<UserSummaryResponse> toFollowingSummaryList(List<Follow> follows) {
        if (follows == null) return null;
        return follows.stream()
                .map(this:: followingToUserSummary)
                .toList();
    }

    /* ===================== After-Mapping Hooks ===================== */

    /**
     * Fallback for displayName if null.
     */
    @AfterMapping
    default void fillDisplayName(@MappingTarget UserSummaryResponse response, User user) {
        if (response.getDisplayName() == null && user != null) {
            if (user.getUsername() != null) {
                response.setDisplayName(user. getUsername());
            }
        }
    }
}