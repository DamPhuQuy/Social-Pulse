package com.socialpulse.app.report.adapter.out;

import com.socialpulse.app.report.application.port.out.ReportRepositoryPort;
import com.socialpulse.app.report.domain.model.Report;
import com.socialpulse.app.report.infrastructure.persistence.mapper.ReportDomainToEntity;
import com.socialpulse.app.report.infrastructure.persistence.mapper.ReportEntityToDomain;
import com.socialpulse.app.report.infrastructure.persistence.repository.JpaReportRepository;

public class ReportRepositoryAdapter implements ReportRepositoryPort {

    private final JpaReportRepository jpaReportRepository;
    private final ReportEntityToDomain reportEntityToDomain;
    private final ReportDomainToEntity reportDomainToEntity;

    public ReportRepositoryAdapter(JpaReportRepository jpaReportRepository,
                                   ReportEntityToDomain reportEntityToDomain,
                                   ReportDomainToEntity reportDomainToEntity) {
        this.jpaReportRepository = jpaReportRepository;
        this.reportEntityToDomain = reportEntityToDomain;
        this.reportDomainToEntity = reportDomainToEntity;
    }

    @Override
    public Report save(Report report) {
        return reportEntityToDomain.toDomain(
                jpaReportRepository.save(reportDomainToEntity.toEntity(report))
        );
    }
}
