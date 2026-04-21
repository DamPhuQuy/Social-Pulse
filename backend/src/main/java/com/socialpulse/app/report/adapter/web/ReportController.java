package com.socialpulse.app.report.adapter.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.socialpulse.app.common.dto.response.ApiResponse;
import com.socialpulse.app.report.application.dto.mapper.ReportMapper;
import com.socialpulse.app.report.application.dto.request.CreateReportRequest;
import com.socialpulse.app.report.application.dto.response.ReportResponse;
import com.socialpulse.app.report.application.usecase.CreateReportUseCase;
import com.socialpulse.app.report.domain.model.Report;
import com.socialpulse.app.security.user.CustomUserDetails;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final CreateReportUseCase createReportUseCase;
    private final ReportMapper reportMapper;

    public ReportController(CreateReportUseCase createReportUseCase,
                            ReportMapper reportMapper) {
        this.createReportUseCase = createReportUseCase;
        this.reportMapper = reportMapper;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReportResponse>> submitReport(
        @RequestBody CreateReportRequest request,
        @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        Long currentUserId = currentUser.getId();
        Report savedReport = createReportUseCase.createReport(currentUserId, request);
        ReportResponse response = reportMapper.toResponse(savedReport);
        return ResponseEntity.ok(ApiResponse.<ReportResponse>builder().data(response).build());
    }
}

