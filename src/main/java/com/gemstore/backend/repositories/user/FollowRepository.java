package com.gemstore.backend.repositories.user;

import com.gemstore.backend.entities.user.Follow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data. jpa.repository. JpaRepository;
import org.springframework. data.jpa. repository.Modifying;
import org. springframework.data.jpa.repository.Query;
import org. springframework.data.repository.query.Param;
import org. springframework.stereotype.Repository;

import java. util.List;
import java. util.Optional;
import java.util. Set;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {

    // ==================== EXISTS CHECKS ====================

    // Check if user A follows user B (with specific status)
    boolean existsByFollowerIdAndFollowingIdAndStatus(Long followerId, Long followingId, String status);

    // Check if user A follows user B (any status)
    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    // ==================== FIND ====================

    // Find specific follow relationship
    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    // ==================== PAGINATED QUERIES ====================

    // Get followers of a user (people who follow them) - filtered by status
    // Used for:  active followers list, pending requests list
    Page<Follow> findByFollowingIdAndStatus(Long followingId, String status, Pageable pageable);

    // Get following of a user (people they follow) - filtered by status
    Page<Follow> findByFollowerIdAndStatus(Long followerId, String status, Pageable pageable);

    // ==================== COUNT QUERIES ====================

    // Count followers by status (active or pending)
    long countByFollowingIdAndStatus(Long followingId, String status);

    // Count following by status
    long countByFollowerIdAndStatus(Long followerId, String status);

    // ==================== DELETE ====================

    // Delete follow relationship
    @Modifying
    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);

    // ==================== BATCH QUERIES ====================

    // Batch check: Get which users from a list the current user is following
    @Query("SELECT f. following. id FROM Follow f WHERE f.follower. id = :userId AND f.following.id IN :userIds AND f.status = 'ACTIVE'")
    Set<Long> findFollowingIds(@Param("userId") Long userId, @Param("userIds") List<Long> userIds);
}