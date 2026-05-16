package com.socialpulse.app.report.infrastructure.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.socialpulse.app.report.domain.enums.ReportStatus;
import com.socialpulse.app.report.infrastructure.persistence.entity.ReportEntity;

public interface JpaReportRepository extends JpaRepository<ReportEntity, Long> {
    Page<ReportEntity> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);
}
