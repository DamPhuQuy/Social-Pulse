package com.socialpulse.app.report.adapter.persistence;

import com.socialpulse.app.report.domain.repository.ReportRepository;
import com.socialpulse.app.report.domain.model.Report;
import com.socialpulse.app.report.infrastructure.persistence.mapper.ReportPersistenceMapper;
import com.socialpulse.app.report.infrastructure.persistence.repository.JpaReportRepository;

public class ReportRepositoryAdapter implements ReportRepository {

    private final JpaReportRepository jpaReportRepository;
    private final ReportPersistenceMapper reportPersistenceMapper;

    public ReportRepositoryAdapter(JpaReportRepository jpaReportRepository,
                                   ReportPersistenceMapper reportPersistenceMapper) {
        this.jpaReportRepository = jpaReportRepository;
        this.reportPersistenceMapper = reportPersistenceMapper;
    }

    @Override
    public Report save(Report report) {
        return reportPersistenceMapper.toDomain(
            jpaReportRepository.save(reportPersistenceMapper.toEntity(report))
        );
    }
}


