package com.gemstore.backend.services.report;

import com.gemstore.backend.dtos.report.CreateReportRequest;
import com.gemstore.backend.dtos.report.ReportResponse;
import com.gemstore.backend.dtos.report.UpdateReportRequest;
import com.gemstore.backend.entities.listing.Listing;
import com.gemstore.backend.entities.listing.ListingStatus;
import com.gemstore.backend.entities.message.Message;
import com.gemstore.backend.entities.report.Report;
import com.gemstore.backend.entities.report.ReportAction;
import com.gemstore.backend.entities.report.ReportStatus;
import com.gemstore.backend.entities.report.ReportType;
import com.gemstore.backend.entities.user.User;
import com.gemstore.backend.exceptions.BadRequestException;
import com.gemstore.backend.exceptions.ResourceNotFoundException;
import com.gemstore.backend.repositories.listing.ListingRepository;
import com.gemstore.backend.repositories.message.MessageRepository;
import com.gemstore.backend.repositories.report.ReportRepository;
import com.gemstore.backend.repositories.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final MessageRepository messageRepository;

    // ✅ User status constants (matching your User entity)
    private static final String USER_STATUS_ACTIVE = "ACTIVE";
    private static final String USER_STATUS_SUSPENDED = "SUSPENDED";
    private static final String USER_STATUS_BANNED = "BANNED";

    /**
     * Create a new report
     */
    @Transactional
    public ReportResponse createReport(Long reporterId, CreateReportRequest request) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Validate and check for duplicates
        validateReportRequest(reporterId, request);

        Report report = Report.builder()
                .reporter(reporter)
                .reportType(request.getReportType())
                .reason(request.getReason())
                .description(request.getDescription())
                .status(ReportStatus.PENDING)
                .build();

        // Set the reported entity based on type
        switch (request.getReportType()) {
            case LISTING -> {
                Listing listing = listingRepository.findById(request.getListingId())
                        .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));

                if (listing.getSeller().getId().equals(reporterId)) {
                    throw new BadRequestException("You cannot report your own listing");
                }

                report.setReportedListing(listing);
            }
            case USER -> {
                User reportedUser = userRepository.findById(request.getUserId())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                if (reportedUser.getId().equals(reporterId)) {
                    throw new BadRequestException("You cannot report yourself");
                }

                report.setReportedUser(reportedUser);
            }
            case MESSAGE -> {
                Message message = messageRepository.findById(request.getMessageId())
                        .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

                if (message.getSender().getId().equals(reporterId)) {
                    throw new BadRequestException("You cannot report your own message");
                }

                report.setReportedMessage(message);
            }
        }

        Report savedReport = reportRepository.save(report);
        log.info("Report created: {} by user {} for {} {}",
                savedReport.getId(), reporterId, request.getReportType(), getReportedEntityId(request));

        // Auto-flag if multiple reports exist
        checkAndAutoFlag(savedReport);

        return mapToResponse(savedReport, false);
    }

    /**
     * Get report by ID
     */
    @Transactional(readOnly = true)
    public ReportResponse getReport(Long reportId, boolean isAdmin) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        return mapToResponse(report, isAdmin);
    }

    /**
     * Get all reports (admin only)
     */
    @Transactional(readOnly = true)
    public Page<ReportResponse> getAllReports(Pageable pageable, ReportStatus status, ReportType type) {
        Page<Report> reports;

        if (status != null) {
            reports = reportRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else if (type != null) {
            reports = reportRepository.findByReportTypeOrderByCreatedAtDesc(type, pageable);
        } else {
            reports = reportRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        return reports.map(r -> mapToResponse(r, true));
    }

    /**
     * Get pending reports count (admin dashboard)
     */
    @Transactional(readOnly = true)
    public long getPendingReportsCount() {
        return reportRepository.countByStatus(ReportStatus.PENDING);
    }

    /**
     * Update report status (admin only)
     */
    @Transactional
    public ReportResponse updateReport(Long reportId, Long adminId, UpdateReportRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        report.setStatus(request.getStatus());
        report.setAdminNotes(request.getAdminNotes());
        report.setActionTaken(request.getActionTaken());
        report.setReviewedBy(admin);
        report.setReviewedAt(LocalDateTime.now());

        // Execute action if specified
        if (request.getActionTaken() != null) {
            executeAction(report, request.getActionTaken());
        }

        Report updatedReport = reportRepository.save(report);
        log.info("Report {} updated by admin {} - Status: {}, Action: {}",
                reportId, adminId, request.getStatus(), request.getActionTaken());

        return mapToResponse(updatedReport, true);
    }

    /**
     * Get user's submitted reports
     */
    @Transactional(readOnly = true)
    public Page<ReportResponse> getUserReports(Long userId, Pageable pageable) {
        return reportRepository.findByReporterIdOrderByCreatedAtDesc(userId, pageable)
                .map(r -> mapToResponse(r, false));
    }

    /**
     * Check if user has already reported this listing
     */
    @Transactional(readOnly = true)
    public boolean hasUserReportedListing(Long userId, Long listingId) {
        return reportRepository.existsByReporterIdAndReportedListingId(userId, listingId);
    }

    // ========== PRIVATE HELPER METHODS ==========

    private void validateReportRequest(Long reporterId, CreateReportRequest request) {
        switch (request.getReportType()) {
            case LISTING -> {
                if (request.getListingId() == null) {
                    throw new BadRequestException("Listing ID is required for listing reports");
                }
                if (reportRepository.existsByReporterIdAndReportedListingId(reporterId, request.getListingId())) {
                    throw new BadRequestException("You have already reported this listing");
                }
            }
            case USER -> {
                if (request.getUserId() == null) {
                    throw new BadRequestException("User ID is required for user reports");
                }
                if (reportRepository.existsByReporterIdAndReportedUserId(reporterId, request.getUserId())) {
                    throw new BadRequestException("You have already reported this user");
                }
            }
            case MESSAGE -> {
                if (request.getMessageId() == null) {
                    throw new BadRequestException("Message ID is required for message reports");
                }
                if (reportRepository.existsByReporterIdAndReportedMessageId(reporterId, request.getMessageId())) {
                    throw new BadRequestException("You have already reported this message");
                }
            }
        }
    }

    private void checkAndAutoFlag(Report report) {
        if (report.getReportedListing() != null) {
            long reportCount = reportRepository.countByReportedListingId(report.getReportedListing().getId());
            if (reportCount >= 3) {
                log.warn("Listing {} has {} reports - auto-flagging for review",
                        report.getReportedListing().getId(), reportCount);
            }
        }
    }

    // ✅ Updated to use String status
    private void executeAction(Report report, ReportAction action) {
        switch (action) {
            case LISTING_REMOVED, LISTING_SUSPENDED -> {
                if (report.getReportedListing() != null) {
                    Listing listing = report.getReportedListing();
                    listing.setStatus(ListingStatus.SUSPENDED);
                    listingRepository.save(listing);
                    log.info("Listing {} suspended due to report {}", listing.getId(), report.getId());
                }
            }
            case USER_SUSPENDED -> {
                if (report.getReportedUser() != null) {
                    User user = report.getReportedUser();
                    user.setStatus(USER_STATUS_SUSPENDED);  // ✅ Using String
                    userRepository.save(user);
                    log.info("User {} suspended due to report {}", user.getId(), report.getId());
                }
            }
            case USER_BANNED -> {
                if (report.getReportedUser() != null) {
                    User user = report.getReportedUser();
                    user.setStatus(USER_STATUS_BANNED);  // ✅ Using String
                    userRepository.save(user);
                    log.info("User {} banned due to report {}", user.getId(), report.getId());
                }
            }
            case MESSAGE_DELETED -> {
                if (report.getReportedMessage() != null) {
                    Message message = report.getReportedMessage();
                    message.setIsDeleted(true);  // ✅ Make sure Message has this field
                    messageRepository.save(message);
                    log.info("Message {} deleted due to report {}", message.getId(), report.getId());
                }
            }
            default -> {
                // NO_ACTION, WARNING_ISSUED - just log
                log.info("Report {} resolved with action: {}", report.getId(), action);
            }
        }
    }

    private Long getReportedEntityId(CreateReportRequest request) {
        return switch (request.getReportType()) {
            case LISTING -> request.getListingId();
            case USER -> request.getUserId();
            case MESSAGE -> request.getMessageId();
        };
    }

    private ReportResponse mapToResponse(Report report, boolean includeAdminFields) {
        ReportResponse.ReportResponseBuilder builder = ReportResponse.builder()
                .id(report.getId())
                .reportType(report.getReportType())
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .reporterId(report.getReporter().getId())
                .reporterUsername(report.getReporter().getUsername());

        // Set reported entity info
        if (report.getReportedListing() != null) {
            builder.reportedListingId(report.getReportedListing().getId())
                    .reportedListingTitle(report.getReportedListing().getTitle());
        }
        if (report.getReportedUser() != null) {
            builder.reportedUserId(report.getReportedUser().getId())
                    .reportedUsername(report.getReportedUser().getUsername());
        }
        if (report.getReportedMessage() != null) {
            builder.reportedMessageId(report.getReportedMessage().getId());
        }

        // Include admin fields only for admin responses
        if (includeAdminFields) {
            builder.adminNotes(report.getAdminNotes())
                    .actionTaken(report.getActionTaken())
                    .reviewedAt(report.getReviewedAt());
            if (report.getReviewedBy() != null) {
                builder.reviewedByUsername(report.getReviewedBy().getUsername());
            }
        }

        return builder.build();
    }
}