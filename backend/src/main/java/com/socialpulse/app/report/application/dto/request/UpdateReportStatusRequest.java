package com.socialpulse.app.report.application.dto.request;

import com.socialpulse.app.report.domain.enums.ReportStatus;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReportStatusRequest {
    @NotNull(message = "Report status is required")
    private ReportStatus status;
}
