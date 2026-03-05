package com.gemstore.backend.repositories.report;


import com.gemstore.backend.entities.report.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    // Check for duplicate reports
    boolean existsByReporterIdAndReportedListingId(Long reporterId, Long listingId);
    boolean existsByReporterIdAndReportedUserId(Long reporterId, Long userId);
    boolean existsByReporterIdAndReportedMessageId(Long reporterId, Long messageId);

    // Get reports by status
    Page<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    // Get all reports (admin)
    Page<Report> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Get reports by type
    Page<Report> findByReportTypeOrderByCreatedAtDesc(ReportType type, Pageable pageable);

    // Get reports for a specific listing
    List<Report> findByReportedListingIdOrderByCreatedAtDesc(Long listingId);

    // Get reports for a specific user
    List<Report> findByReportedUserIdOrderByCreatedAtDesc(Long userId);

    // Count pending reports (for admin dashboard)
    long countByStatus(ReportStatus status);

    // Get user's submitted reports
    Page<Report> findByReporterIdOrderByCreatedAtDesc(Long reporterId, Pageable pageable);

    // Check if user has reported this listing
    Optional<Report> findByReporterIdAndReportedListingId(Long reporterId, Long listingId);

    // Count reports against a user (for trust score)
    @Query("SELECT COUNT(r) FROM Report r WHERE r.reportedUser.id = :userId AND r.status = 'RESOLVED' AND r.actionTaken != 'NO_ACTION'")
    long countValidReportsAgainstUser(@Param("userId") Long userId);

    // Count reports against a listing
    long countByReportedListingId(Long listingId);
}
