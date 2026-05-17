package com.socialpulse.app.report.application.service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.socialpulse.app.common.dto.response.PageResponse;
import com.socialpulse.app.report.application.dto.mapper.ReportMapper;
import com.socialpulse.app.report.application.dto.response.ReportResponse;
import com.socialpulse.app.report.application.usecase.GetReportsUseCase;
import com.socialpulse.app.report.domain.enums.ReportStatus;
import com.socialpulse.app.report.domain.model.Report;
import com.socialpulse.app.report.domain.repository.ReportRepository;

public class GetReportsService implements GetReportsUseCase {
    private final ReportRepository reportRepository;
    private final ReportMapper reportMapper;
    private final ReportResponseEnricher reportResponseEnricher;

    public GetReportsService(ReportRepository reportRepository,
                             ReportMapper reportMapper,
                             ReportResponseEnricher reportResponseEnricher) {
        this.reportRepository = reportRepository;
        this.reportMapper = reportMapper;
        this.reportResponseEnricher = reportResponseEnricher;
    }

    @Override
    public PageResponse<ReportResponse> getReports(ReportStatus status, int page, int size) {
        Page<Report> reportPage = status == null
                ? reportRepository.findAll(page, size)
                : reportRepository.findByStatus(status, page, size);

        List<ReportResponse> items = reportPage.getContent().stream()
                .map(reportMapper::toResponse)
                .toList();

        // Enrich each report with the actual target content
        items = reportResponseEnricher.enrich(items);

        return PageResponse.<ReportResponse>builder()
                .items(items)
                .page(reportPage.getNumber())
                .size(reportPage.getSize())
                .totalElements(reportPage.getTotalElements())
                .totalPages(reportPage.getTotalPages())
                .hasNext(reportPage.hasNext())
                .build();
    }
}

