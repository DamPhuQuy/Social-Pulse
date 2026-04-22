package com.socialpulse.app.report.domain.repository;

import com.socialpulse.app.report.domain.model.Report;

public interface ReportRepository {
    Report save(Report report);
}

