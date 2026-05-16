package com.socialpulse.app.report.adapter.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.socialpulse.app.common.dto.response.PageResponse;
import com.socialpulse.app.common.dto.response.ApiResponse;
import com.socialpulse.app.report.application.dto.mapper.ReportMapper;
import com.socialpulse.app.report.application.dto.request.CreateReportRequest;
import com.socialpulse.app.report.application.dto.request.UpdateReportStatusRequest;
import com.socialpulse.app.report.application.dto.response.ReportResponse;
import com.socialpulse.app.report.application.usecase.CreateReportUseCase;
import com.socialpulse.app.report.application.usecase.GetReportsUseCase;
import com.socialpulse.app.report.application.usecase.UpdateReportStatusUseCase;
import com.socialpulse.app.report.domain.enums.ReportStatus;
import com.socialpulse.app.report.domain.model.Report;
import com.socialpulse.app.security.user.CustomUserDetails;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Report API", description = "API for submitting reports on posts or comments")
public class ReportController {

    private final CreateReportUseCase createReportUseCase;
    private final GetReportsUseCase getReportsUseCase;
    private final UpdateReportStatusUseCase updateReportStatusUseCase;
    private final ReportMapper reportMapper;

    public ReportController(CreateReportUseCase createReportUseCase,
                            GetReportsUseCase getReportsUseCase,
                            UpdateReportStatusUseCase updateReportStatusUseCase,
                            ReportMapper reportMapper) {
        this.createReportUseCase = createReportUseCase;
        this.getReportsUseCase = getReportsUseCase;
        this.updateReportStatusUseCase = updateReportStatusUseCase;
        this.reportMapper = reportMapper;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('report:create')")
    public ResponseEntity<ApiResponse<ReportResponse>> submitReport(
        @RequestBody @Valid CreateReportRequest request,
        @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        Long currentUserId = currentUser.getId();
        Report savedReport = createReportUseCase.createReport(currentUserId, request);
        ReportResponse response = reportMapper.toResponse(savedReport);
        return ResponseEntity.ok(ApiResponse.<ReportResponse>builder().data(response).build());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('report:manage')")
    public ResponseEntity<ApiResponse<PageResponse<ReportResponse>>> getReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<ReportResponse>>builder()
                .data(getReportsUseCase.getReports(status, page, size))
                .build());
    }

    @PatchMapping("/{reportId}/status")
    @PreAuthorize("hasAuthority('report:manage')")
    public ResponseEntity<ApiResponse<ReportResponse>> updateReportStatus(
            @PathVariable Long reportId,
            @RequestBody @Valid UpdateReportStatusRequest request) {
        Report updatedReport = updateReportStatusUseCase.updateStatus(reportId, request);
        return ResponseEntity.ok(ApiResponse.<ReportResponse>builder()
                .data(reportMapper.toResponse(updatedReport))
                .build());
    }
}
