package com.socialpulse.app.report.application.usecase;

import com.socialpulse.app.report.application.dto.request.UpdateReportStatusRequest;
import com.socialpulse.app.report.domain.model.Report;

public interface UpdateReportStatusUseCase {
    Report updateStatus(Long reportId, UpdateReportStatusRequest request);
}
