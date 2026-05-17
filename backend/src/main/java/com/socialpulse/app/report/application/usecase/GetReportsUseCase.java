package com.socialpulse.app.report.application.usecase;

import com.socialpulse.app.common.dto.response.PageResponse;
import com.socialpulse.app.report.application.dto.response.ReportResponse;
import com.socialpulse.app.report.domain.enums.ReportStatus;

public interface GetReportsUseCase {
    PageResponse<ReportResponse> getReports(ReportStatus status, int page, int size);
}
