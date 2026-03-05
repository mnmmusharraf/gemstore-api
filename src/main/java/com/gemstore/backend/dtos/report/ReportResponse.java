package com.gemstore.backend.dtos.report;


import com.gemstore.backend.entities.report.ReportAction;
import com.gemstore.backend.entities.report.ReportReason;
import com.gemstore.backend.entities.report.ReportStatus;
import com.gemstore.backend.entities.report.ReportType;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ReportResponse {
    private Long id;
    private ReportType reportType;
    private ReportReason reason;
    private String description;
    private ReportStatus status;
    private LocalDateTime createdAt;

    // Reporter info
    private Long reporterId;
    private String reporterUsername;

    // Reported entity info
    private Long reportedListingId;
    private String reportedListingTitle;
    private Long reportedUserId;
    private String reportedUsername;
    private Long reportedMessageId;

    // Admin info (only for admin responses)
    private String adminNotes;
    private ReportAction actionTaken;
    private LocalDateTime reviewedAt;
    private String reviewedByUsername;
}
