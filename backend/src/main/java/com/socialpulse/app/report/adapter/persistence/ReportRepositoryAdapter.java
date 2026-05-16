package com.socialpulse.app.report.adapter.persistence;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.socialpulse.app.report.domain.enums.ReportStatus;
import com.socialpulse.app.report.domain.model.Report;
import com.socialpulse.app.report.domain.repository.ReportRepository;
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
    public Optional<Report> findById(Long reportId) {
        return jpaReportRepository.findById(reportId)
                .map(reportPersistenceMapper::toDomain);
    }

    @Override
    public Report save(Report report) {
        return reportPersistenceMapper.toDomain(
            jpaReportRepository.save(reportPersistenceMapper.toEntity(report))
        );
    }

    @Override
    public Page<Report> findAll(int page, int size) {
        return jpaReportRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(reportPersistenceMapper::toDomain);
    }

    @Override
    public Page<Report> findByStatus(ReportStatus status, int page, int size) {
        return jpaReportRepository.findByStatusOrderByCreatedAtDesc(status, PageRequest.of(page, size))
                .map(reportPersistenceMapper::toDomain);
    }
}

