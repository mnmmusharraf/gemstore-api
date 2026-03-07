package com.gemstore.backend.dtos.report;


import com.gemstore.backend.entities.report.ReportAction;
import com.gemstore.backend.entities.report.ReportStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateReportRequest {

    @NotNull(message = "Status is required")
    private ReportStatus status;

    private ReportAction actionTaken;

    private String adminNotes;
}
