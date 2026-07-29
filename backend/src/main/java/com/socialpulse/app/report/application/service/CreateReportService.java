package com.socialpulse.app.report.application.service;
import org.springframework.stereotype.Service;

import com.socialpulse.app.report.application.dto.mapper.ReportMapper;
import com.socialpulse.app.report.application.dto.request.CreateReportRequest;
import com.socialpulse.app.report.application.usecase.CreateReportUseCase;
import com.socialpulse.app.report.domain.repository.ReportRepository;
import com.socialpulse.app.report.domain.model.Report;

@Service
public class CreateReportService implements CreateReportUseCase {

    private final ReportRepository reportRepositoryPort;
    private final ReportTargetValidator reportTargetValidator;
    private final ReportMapper reportMapper;

    public CreateReportService(
            ReportRepository reportRepositoryPort,
            ReportTargetValidator reportTargetValidator,
            ReportMapper reportMapper) {
        this.reportRepositoryPort = reportRepositoryPort;
        this.reportTargetValidator = reportTargetValidator;
        this.reportMapper = reportMapper;
    }

    @Override
    public Report createReport(Long reporterId, CreateReportRequest request) {
        reportTargetValidator.validate(request.getTargetType(), request.getTargetId());
        Report report = reportMapper.toReport(request, reporterId);

        report.markPending();
        return reportRepositoryPort.save(report);
    }
}

