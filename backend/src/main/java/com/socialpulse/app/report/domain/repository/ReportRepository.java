package com.socialpulse.app.report.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;

import com.socialpulse.app.report.domain.enums.ReportStatus;
import com.socialpulse.app.report.domain.model.Report;

public interface ReportRepository {
    Optional<Report> findById(Long reportId);

    Report save(Report report);

    Page<Report> findAll(int page, int size);

    Page<Report> findByStatus(ReportStatus status, int page, int size);
}
