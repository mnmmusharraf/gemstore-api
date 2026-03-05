package com.gemstore.backend.dtos.report;


import com.gemstore.backend.entities.report.ReportReason;
import com.gemstore.backend.entities.report.ReportType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateReportRequest {

    @NotNull(message = "Report type is required")
    private ReportType reportType;

    private Long listingId;
    private Long userId;
    private Long messageId;

    @NotNull(message = "Reason is required")
    private ReportReason reason;

    private String description;  // Optional additional details
}
