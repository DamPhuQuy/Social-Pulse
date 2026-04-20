package com.socialpulse.app.report.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.socialpulse.app.report.infrastructure.persistence.entity.ReportEntity;

public interface JpaReportRepository extends JpaRepository<ReportEntity, Long> {

}
