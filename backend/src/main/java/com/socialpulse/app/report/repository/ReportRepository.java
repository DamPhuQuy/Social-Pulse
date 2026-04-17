package com.socialpulse.app.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.socialpulse.app.report.entity.Report;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    // Sau này bạn có thể viết thêm hàm tìm report theo status cho Admin ở đây
    // List<Report> findByStatus(ReportStatus status);
}