package com.socialpulse.app.report.application.service;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.ReportCode;
import com.socialpulse.app.report.application.dto.request.UpdateReportStatusRequest;
import com.socialpulse.app.report.application.usecase.UpdateReportStatusUseCase;
import com.socialpulse.app.report.domain.enums.ReportStatus;
import com.socialpulse.app.report.domain.model.Report;
import com.socialpulse.app.report.domain.repository.ReportRepository;

public class UpdateReportStatusService implements UpdateReportStatusUseCase {
    private final ReportRepository reportRepository;

    public UpdateReportStatusService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @Override
    public Report updateStatus(Long reportId, UpdateReportStatusRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new AppException(ReportCode.REPORT_NOT_FOUND));

        if (request.getStatus() == ReportStatus.PENDING) {
            throw new AppException(ReportCode.REPORT_STATUS_UPDATE_NOT_ALLOWED);
        }

        if (request.getStatus() == ReportStatus.RESOLVED) {
            report.markResolved();
        } else if (request.getStatus() == ReportStatus.REJECTED) {
            report.markRejected();
        }

        return reportRepository.save(report);
    }
}
