package com.socialpulse.app.report.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.socialpulse.app.report.dto.CreateReportRequest;
import com.socialpulse.app.report.entity.Report;
import com.socialpulse.app.report.repository.ReportRepository;

@Service
public class ReportService {

    @Autowired
    private ReportRepository reportRepository;

    public Report createReport(Long reporterId, CreateReportRequest request) {
        // Có thể thêm logic kiểm tra targetId (bài viết/user) có tồn tại thật không ở đây
        
        Report report = Report.builder()
                .reporterId(reporterId)
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .reason(request.getReason())
                .build();

        return reportRepository.save(report);
    }
}