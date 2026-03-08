package com.gemstore.backend.services.user;

import com.gemstore.backend.entities.user.Follow;
import com.gemstore.backend.entities.user.User;
import com.gemstore.backend.exceptions.BadRequestException;
import com.gemstore.backend.exceptions.ResourceNotFoundException;
import com.gemstore.backend.mappers.user.FollowMapper;
import com.gemstore.backend.repositories.user.FollowRepository;
import com.gemstore.backend.repositories.user.UserRepository;
import com.gemstore.backend.services.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FollowService - Unit Tests")
class FollowServiceTest {

    @Mock private FollowRepository followRepository;
    @Mock private UserRepository userRepository;
    @Mock private FollowMapper followMapper;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private FollowService followService;

    private User follower;
    private User following;

    @BeforeEach
    void setUp() {
        follower = new User();
        follower.setId(1L);
        follower.setUsername("follower");
        follower.setFollowingCount(0);
        follower.setFollowersCount(0);

        following = new User();
        following.setId(2L);
        following.setUsername("following");
        following.setFollowingCount(0);
        following.setFollowersCount(0);
        following.setPrivateProfile(false);
    }

    @Nested
    @DisplayName("follow()")
    class FollowTests {

        @Test
        @DisplayName("TC-FLW-001: Should follow public user")
        void shouldFollowPublicUser() {
            when(followRepository.findByFollowerIdAndFollowingId(1L, 2L)).thenReturn(Optional.empty());
            when(userRepository.findById(1L)).thenReturn(Optional.of(follower));
            when(userRepository.findById(2L)).thenReturn(Optional.of(following));

            FollowService.FollowResult result = followService.follow(1L, 2L);

            assertThat(result.isFollowing()).isTrue();
            assertThat(result.isPending()).isFalse();
            assertThat(follower.getFollowingCount()).isEqualTo(1);
            assertThat(following.getFollowersCount()).isEqualTo(1);
            verify(followRepository).save(any(Follow.class));
            verify(notificationService).notifyFollow(following, follower);
        }

        @Test
        @DisplayName("TC-FLW-002: Should send pending request for private user")
        void shouldSendPendingForPrivate() {
            following.setPrivateProfile(true);

            when(followRepository.findByFollowerIdAndFollowingId(1L, 2L)).thenReturn(Optional.empty());
            when(userRepository.findById(1L)).thenReturn(Optional.of(follower));
            when(userRepository.findById(2L)).thenReturn(Optional.of(following));

            FollowService.FollowResult result = followService.follow(1L, 2L);

            assertThat(result.isFollowing()).isTrue();
            assertThat(result.isPending()).isTrue();
            assertThat(follower.getFollowingCount()).isEqualTo(0); // not incremented yet
            verify(notificationService).notifyFollowRequest(following, follower);
        }

        @Test
        @DisplayName("TC-FLW-003: Should reject self-follow")
        void shouldRejectSelfFollow() {
            assertThatThrownBy(() -> followService.follow(1L, 1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("yourself");
        }

        @Test
        @DisplayName("TC-FLW-004: Should reject duplicate follow")
        void shouldRejectDuplicate() {
            Follow existing = Follow.builder().follower(follower).following(following).status("ACTIVE").build();
            when(followRepository.findByFollowerIdAndFollowingId(1L, 2L)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> followService.follow(1L, 2L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already following");
        }

        @Test
        @DisplayName("TC-FLW-005: Should reject duplicate pending request")
        void shouldRejectDuplicatePending() {
            Follow existing = Follow.builder().follower(follower).following(following).status("PENDING").build();
            when(followRepository.findByFollowerIdAndFollowingId(1L, 2L)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> followService.follow(1L, 2L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("pending");
        }

        @Test
        @DisplayName("TC-FLW-006: Should throw when user not found")
        void shouldThrowWhenNotFound() {
            when(followRepository.findByFollowerIdAndFollowingId(1L, 99L)).thenReturn(Optional.empty());
            when(userRepository.findById(1L)).thenReturn(Optional.of(follower));
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> followService.follow(1L, 99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("unfollow()")
    class UnfollowTests {

        @Test
        @DisplayName("TC-FLW-007: Should unfollow and decrement counts")
        void shouldUnfollow() {
            follower.setFollowingCount(1);
            following.setFollowersCount(1);

            Follow follow = Follow.builder().follower(follower).following(following).status("ACTIVE").build();
            when(followRepository.findByFollowerIdAndFollowingId(1L, 2L)).thenReturn(Optional.of(follow));

            followService.unfollow(1L, 2L);

            verify(followRepository).delete(follow);
            assertThat(follower.getFollowingCount()).isEqualTo(0);
            assertThat(following.getFollowersCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("TC-FLW-008: Should reject self-unfollow")
        void shouldRejectSelfUnfollow() {
            assertThatThrownBy(() -> followService.unfollow(1L, 1L))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("TC-FLW-009: Should throw when not following")
        void shouldThrowWhenNotFollowing() {
            when(followRepository.findByFollowerIdAndFollowingId(1L, 2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> followService.unfollow(1L, 2L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("not following");
        }

        @Test
        @DisplayName("TC-FLW-010: Should not decrement counts for pending cancel")
        void shouldNotDecrementForPending() {
            Follow follow = Follow.builder().follower(follower).following(following).status("PENDING").build();
            when(followRepository.findByFollowerIdAndFollowingId(1L, 2L)).thenReturn(Optional.of(follow));

            followService.unfollow(1L, 2L);

            assertThat(follower.getFollowingCount()).isEqualTo(0);
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("isFollowing()")
    class IsFollowing {

        @Test
        @DisplayName("TC-FLW-011: Should return true when following")
        void shouldReturnTrue() {
            when(followRepository.existsByFollowerIdAndFollowingIdAndStatus(1L, 2L, "ACTIVE")).thenReturn(true);

            assertThat(followService.isFollowing(1L, 2L)).isTrue();
        }

        @Test
        @DisplayName("TC-FLW-012: Should return false when not following")
        void shouldReturnFalse() {
            when(followRepository.existsByFollowerIdAndFollowingIdAndStatus(1L, 2L, "ACTIVE")).thenReturn(false);

            assertThat(followService.isFollowing(1L, 2L)).isFalse();
        }
    }

    @Nested
    @DisplayName("getFollowStatus()")
    class GetFollowStatus {

        @Test
        @DisplayName("TC-FLW-013: Should return ACTIVE")
        void shouldReturnActive() {
            Follow follow = Follow.builder().status("ACTIVE").build();
            when(followRepository.findByFollowerIdAndFollowingId(1L, 2L)).thenReturn(Optional.of(follow));

            assertThat(followService.getFollowStatus(1L, 2L)).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("TC-FLW-014: Should return NONE when not following")
        void shouldReturnNone() {
            when(followRepository.findByFollowerIdAndFollowingId(1L, 2L)).thenReturn(Optional.empty());

            assertThat(followService.getFollowStatus(1L, 2L)).isEqualTo("NONE");
        }
    }

    @Nested
    @DisplayName("getPendingRequestsCount()")
    class PendingCount {

        @Test
        @DisplayName("TC-FLW-015: Should return pending count")
        void shouldReturnCount() {
            when(followRepository.countByFollowingIdAndStatus(1L, "PENDING")).thenReturn(3L);

            assertThat(followService.getPendingRequestsCount(1L)).isEqualTo(3L);
        }
    }
}