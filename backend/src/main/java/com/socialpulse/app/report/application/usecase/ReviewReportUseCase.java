package com.socialpulse.app.report.application.usecase;

import com.socialpulse.app.report.application.dto.request.ReviewReportRequest;
import com.socialpulse.app.report.application.dto.response.ReportResponse;

public interface ReviewReportUseCase {
    ReportResponse reviewReport(Long reportId, ReviewReportRequest request);
}
