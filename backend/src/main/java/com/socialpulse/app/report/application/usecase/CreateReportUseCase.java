package com.socialpulse.app.report.application.usecase;

import com.socialpulse.app.report.application.dto.request.CreateReportRequest;
import com.socialpulse.app.report.domain.model.Report;

public interface CreateReportUseCase {
    Report createReport(Long reporterId, CreateReportRequest request);
}

