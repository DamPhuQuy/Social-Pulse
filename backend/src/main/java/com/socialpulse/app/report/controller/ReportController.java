package com.socialpulse.app.report.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.socialpulse.app.report.dto.CreateReportRequest;
import com.socialpulse.app.report.entity.Report;
import com.socialpulse.app.report.service.ReportService;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @PostMapping
    public ResponseEntity<?> submitReport(@RequestBody CreateReportRequest request) {
        // Trong thực tế, reporterId sẽ được lấy từ Token (SecurityContext) của người dùng đang đăng nhập.
        // Tạm thời hardcode là 1L để bạn dễ test API qua Postman
        Long currentUserId = 1L; 

        try {
            Report savedReport = reportService.createReport(currentUserId, request);
            return ResponseEntity.ok(savedReport);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi gửi báo cáo: " + e.getMessage());
        }
    }
}