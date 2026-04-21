package com.socialpulse.app.report.application.service;

import com.socialpulse.app.report.application.dto.mapper.ReportMapper;
import com.socialpulse.app.report.application.dto.request.CreateReportRequest;
import com.socialpulse.app.report.application.usecase.CreateReportUseCase;
import com.socialpulse.app.report.domain.repository.ReportRepository;
import com.socialpulse.app.report.domain.model.Report;

public class CreateReportService implements CreateReportUseCase {

    private final ReportRepository reportRepositoryPort;
    private final ReportMapper reportMapper;

    public CreateReportService(ReportRepository reportRepositoryPort, ReportMapper reportMapper) {
        this.reportRepositoryPort = reportRepositoryPort;
        this.reportMapper = reportMapper;
    }

    @Override
    public Report createReport(Long reporterId, CreateReportRequest request) {
        Report report = reportMapper.toReport(request, reporterId);

        report.markPending();
        return reportRepositoryPort.save(report);
    }
}


