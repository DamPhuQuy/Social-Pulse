package com.socialpulse.app.report.application.service;
import org.springframework.stereotype.Service;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.ReportCode;
import com.socialpulse.app.report.application.dto.mapper.ReportMapper;
import com.socialpulse.app.report.application.dto.response.ReportResponse;
import com.socialpulse.app.report.application.usecase.GetReportDetailUseCase;
import com.socialpulse.app.report.domain.model.Report;
import com.socialpulse.app.report.domain.repository.ReportRepository;

@Service
public class GetReportDetailService implements GetReportDetailUseCase {

    private final ReportRepository reportRepository;
    private final ReportMapper reportMapper;
    private final ReportResponseEnricher reportResponseEnricher;

    public GetReportDetailService(ReportRepository reportRepository,
                                  ReportMapper reportMapper,
                                  ReportResponseEnricher reportResponseEnricher) {
        this.reportRepository = reportRepository;
        this.reportMapper = reportMapper;
        this.reportResponseEnricher = reportResponseEnricher;
    }

    @Override
    public ReportResponse getReportDetail(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new AppException(ReportCode.REPORT_NOT_FOUND));

        ReportResponse response = reportMapper.toResponse(report);
        return reportResponseEnricher.enrich(response);
    }
}
