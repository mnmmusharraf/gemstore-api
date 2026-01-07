package com.gemstore.backend.services. user;

import com.gemstore.backend.dtos.common.PageResponse;
import com.gemstore.backend.dtos.user. UserSummaryResponse;
import com.gemstore.backend.entities.user.Follow;
import com.gemstore.backend.entities.user.User;
import com.gemstore.backend.exceptions.BadRequestException;
import com.gemstore.backend.exceptions.ResourceNotFoundException;
import com.gemstore.backend.mappers.user.FollowMapper;
import com.gemstore.backend.repositories.user.FollowRepository;
import com.gemstore.backend.repositories.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class FollowService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_PENDING = "PENDING";

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final FollowMapper followMapper;  // 👈 ADD THIS

    /**
     * Follow a user
     */
    @Transactional
    public FollowResult follow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new BadRequestException("You cannot follow yourself");
        }

        if (followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new BadRequestException("You are already following this user");
        }

        User follower = userRepository. findById(followerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found:  " + followerId));

        User following = userRepository. findById(followingId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found:  " + followingId));

        String status = following.isPrivateProfile() ? STATUS_PENDING : STATUS_ACTIVE;

        Follow follow = Follow. builder()
                .follower(follower)
                .following(following)
                .status(status)
                .build();

        followRepository.save(follow);

        log.info("User {} {} user {}", followerId,
                status. equals(STATUS_PENDING) ? "requested to follow" : "followed",
                followingId);

        return new FollowResult(true, status. equals(STATUS_PENDING));
    }

    /**
     * Unfollow a user
     */
    @Transactional
    public void unfollow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new BadRequestException("You cannot unfollow yourself");
        }

        Follow follow = followRepository.findByFollowerIdAndFollowingId(followerId, followingId)
                .orElseThrow(() -> new BadRequestException("You are not following this user"));

        followRepository.delete(follow);

        log.info("User {} unfollowed user {}", followerId, followingId);
    }

    /**
     * Toggle follow status
     */
    @Transactional
    public FollowResult toggleFollow(Long followerId, Long followingId) {
        if (isFollowing(followerId, followingId)) {
            unfollow(followerId, followingId);
            return new FollowResult(false, false);
        } else {
            return follow(followerId, followingId);
        }
    }

    /**
     * Accept a follow request (for private accounts)
     */
    @Transactional
    public void acceptFollowRequest(Long userId, Long followerId) {
        Follow follow = followRepository. findByFollowerIdAndFollowingId(followerId, userId)
                .orElseThrow(() -> new BadRequestException("Follow request not found"));

        if (! follow.getStatus().equals(STATUS_PENDING)) {
            throw new BadRequestException("This is not a pending request");
        }

        follow.setStatus(STATUS_ACTIVE);
        followRepository.save(follow);

        // Manually update counts since trigger only fires on INSERT/DELETE
        User follower = follow.getFollower();
        User following = follow.getFollowing();
        follower.setFollowingCount(follower. getFollowingCount() + 1);
        following.setFollowersCount(following.getFollowersCount() + 1);
        userRepository.save(follower);
        userRepository.save(following);

        log.info("User {} accepted follow request from user {}", userId, followerId);
    }

    /**
     * Reject/Delete a follow request
     */
    @Transactional
    public void rejectFollowRequest(Long userId, Long followerId) {
        Follow follow = followRepository. findByFollowerIdAndFollowingId(followerId, userId)
                .orElseThrow(() -> new BadRequestException("Follow request not found"));

        followRepository.delete(follow);

        log.info("User {} rejected follow request from user {}", userId, followerId);
    }

    /**
     * Check if user A follows user B (active only)
     */
    @Transactional(readOnly = true)
    public boolean isFollowing(Long followerId, Long followingId) {
        return followRepository.existsByFollowerIdAndFollowingIdAndStatus(
                followerId, followingId, STATUS_ACTIVE);
    }

    /**
     * Check follow status (returns ACTIVE, PENDING, or NONE)
     */
    @Transactional(readOnly = true)
    public String getFollowStatus(Long followerId, Long followingId) {
        return followRepository.findByFollowerIdAndFollowingId(followerId, followingId)
                .map(Follow::getStatus)
                .orElse("NONE");
    }

    /**
     * Get followers of a user
     */
    @Transactional(readOnly = true)
    public PageResponse<UserSummaryResponse> getFollowers(Long userId, Long currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort. by(Sort.Direction.DESC, "createdAt"));
        Page<Follow> follows = followRepository.findByFollowingIdAndStatus(userId, STATUS_ACTIVE, pageable);

        return toPageResponse(follows, currentUserId, true);
    }

    /**
     * Get users that a user is following
     */
    @Transactional(readOnly = true)
    public PageResponse<UserSummaryResponse> getFollowing(Long userId, Long currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Follow> follows = followRepository.findByFollowerIdAndStatus(userId, STATUS_ACTIVE, pageable);

        return toPageResponse(follows, currentUserId, false);
    }

    /**
     * Get pending follow requests (for private accounts)
     */
    @Transactional(readOnly = true)
    public PageResponse<UserSummaryResponse> getPendingRequests(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Follow> follows = followRepository.findByFollowingIdAndStatus(userId, STATUS_PENDING, pageable);

        return toPageResponse(follows, userId, true);
    }

    /**
     * Get pending requests count
     */
    @Transactional(readOnly = true)
    public long getPendingRequestsCount(Long userId) {
        return followRepository.countByFollowingIdAndStatus(userId, STATUS_PENDING);
    }

    /**
     * Batch check:  Get which users the current user is following
     */
    @Transactional(readOnly = true)
    public Set<Long> getFollowingIds(Long userId, List<Long> userIds) {
        return followRepository.findFollowingIds(userId, userIds);
    }

    /**
     * Convert to PageResponse using mapper
     */
    private PageResponse<UserSummaryResponse> toPageResponse(Page<Follow> follows, Long currentUserId, boolean isFollowers) {
        List<UserSummaryResponse> content = follows.getContent().stream()
                .map(follow -> {
                    // Use mapper to convert
                    UserSummaryResponse response = isFollowers
                            ? followMapper.followerToUserSummary(follow)
                            : followMapper.followingToUserSummary(follow);

                    // Set isFollowing dynamically
                    User user = isFollowers ? follow. getFollower() : follow.getFollowing();
                    if (currentUserId != null && ! currentUserId.equals(user.getId())) {
                        response.setIsFollowing(isFollowing(currentUserId, user.getId()));
                    } else {
                        response.setIsFollowing(false);
                    }

                    return response;
                })
                .toList();

        return PageResponse. <UserSummaryResponse>builder()
                .content(content)
                .page(follows.getNumber())
                .size(follows.getSize())
                .totalElements(follows.getTotalElements())
                .totalPages(follows.getTotalPages())
                .first(follows.isFirst())
                .last(follows.isLast())
                .hasNext(follows.hasNext())
                .hasPrevious(follows.hasPrevious())
                .build();
    }

    /**
     * Result object for follow operation
     */
    public record FollowResult(boolean isFollowing, boolean isPending) {}
}