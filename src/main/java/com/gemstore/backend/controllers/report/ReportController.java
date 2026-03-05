package com.gemstore.backend.controllers.report;

import com.gemstore.backend.dtos.report.CreateReportRequest;
import com.gemstore.backend.dtos.report.ReportResponse;
import com.gemstore.backend.dtos.report.UpdateReportRequest;
import com.gemstore.backend.entities.report.ReportStatus;
import com.gemstore.backend.entities.report.ReportType;
import com.gemstore.backend.security.CustomUserDetails;
import com.gemstore.backend.services.report.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // ==================== USER ENDPOINTS ====================

    /**
     * Create a new report
     * POST /api/v1/reports
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createReport(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody CreateReportRequest request
    ) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "You must be logged in to report"
            ));
        }

        ReportResponse report = reportService.createReport(user.getId(), request);

        return ResponseEntity.status(201).body(Map.of(
                "success", true,
                "message", "Report submitted successfully. Thank you for helping keep our community safe.",
                "data", report
        ));
    }

    /**
     * Get current user's submitted reports
     * GET /api/v1/reports/my-reports
     */
    @GetMapping("/my-reports")
    public ResponseEntity<Map<String, Object>> getMyReports(
            @AuthenticationPrincipal CustomUserDetails user,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "You must be logged in"
            ));
        }

        Page<ReportResponse> reports = reportService.getUserReports(user.getId(), pageable);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", reports
        ));
    }

    /**
     * Check if current user has reported a specific listing
     * GET /api/v1/reports/check/listing/{listingId}
     */
    @GetMapping("/check/listing/{listingId}")
    public ResponseEntity<Map<String, Object>> hasReportedListing(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long listingId
    ) {
        if (user == null) {
            return ResponseEntity.ok(Map.of(
                    "hasReported", false
            ));
        }

        boolean hasReported = reportService.hasUserReportedListing(user.getId(), listingId);

        return ResponseEntity.ok(Map.of(
                "hasReported", hasReported
        ));
    }

    /**
     * Get a specific report (user can only see their own)
     * GET /api/v1/reports/{reportId}
     */
    @GetMapping("/{reportId}")
    public ResponseEntity<Map<String, Object>> getReport(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long reportId
    ) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "You must be logged in"
            ));
        }

        ReportResponse report = reportService.getReport(reportId, false);

        // Users can only view their own reports
        if (!report.getReporterId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "error", "You can only view your own reports"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", report
        ));
    }

    // ==================== ADMIN ENDPOINTS ====================

    /**
     * Get all reports (admin only)
     * GET /api/v1/reports/admin
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) ReportType type,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ReportResponse> reports = reportService.getAllReports(pageable, status, type);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", reports
        ));
    }

    /**
     * Get pending reports count (admin dashboard)
     * GET /api/v1/reports/admin/pending-count
     */
    @GetMapping("/admin/pending-count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getPendingReportsCount() {
        long count = reportService.getPendingReportsCount();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "count", count
        ));
    }

    /**
     * Get a specific report with admin details
     * GET /api/v1/reports/admin/{reportId}
     */
    @GetMapping("/admin/{reportId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getReportAsAdmin(
            @PathVariable Long reportId
    ) {
        ReportResponse report = reportService.getReport(reportId, true);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", report
        ));
    }

    /**
     * Update report status and take action (admin only)
     * PUT /api/v1/reports/admin/{reportId}
     */
    @PutMapping("/admin/{reportId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateReport(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long reportId,
            @Valid @RequestBody UpdateReportRequest request
    ) {
        ReportResponse report = reportService.updateReport(reportId, user.getId(), request);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Report updated successfully",
                "data", report
        ));
    }

    /**
     * Get reports by status (admin only)
     * GET /api/v1/reports/admin/status/{status}
     */
    @GetMapping("/admin/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getReportsByStatus(
            @PathVariable ReportStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ReportResponse> reports = reportService.getAllReports(pageable, status, null);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", reports
        ));
    }

    /**
     * Get reports by type (admin only)
     * GET /api/v1/reports/admin/type/{type}
     */
    @GetMapping("/admin/type/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getReportsByType(
            @PathVariable ReportType type,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ReportResponse> reports = reportService.getAllReports(pageable, null, type);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", reports
        ));
    }
}