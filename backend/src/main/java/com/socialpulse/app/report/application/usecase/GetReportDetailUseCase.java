package com.socialpulse.app.report.application.usecase;

import com.socialpulse.app.report.application.dto.response.ReportResponse;

public interface GetReportDetailUseCase {
    ReportResponse getReportDetail(Long reportId);
}
