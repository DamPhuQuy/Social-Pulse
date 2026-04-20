package com.socialpulse.app.report.application.port.out;

import com.socialpulse.app.report.domain.model.Report;

public interface ReportRepositoryPort {
    Report save(Report report);
}
