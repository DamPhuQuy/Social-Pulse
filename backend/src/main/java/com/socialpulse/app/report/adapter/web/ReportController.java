package com.socialpulse.app.report.adapter.web;

import com.socialpulse.app.security.permission.RequiresPermission;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import com.socialpulse.app.common.dto.response.PageResponse;
import com.socialpulse.app.common.dto.response.ApiResponse;
import com.socialpulse.app.report.application.dto.mapper.ReportMapper;
import com.socialpulse.app.report.application.dto.request.CreateReportRequest;
import com.socialpulse.app.report.application.dto.request.ReviewReportRequest;
import com.socialpulse.app.report.application.dto.request.UpdateReportStatusRequest;
import com.socialpulse.app.report.application.dto.response.ReportResponse;
import com.socialpulse.app.report.application.service.ReportResponseEnricher;
import com.socialpulse.app.report.application.usecase.CreateReportUseCase;
import com.socialpulse.app.report.application.usecase.GetReportDetailUseCase;
import com.socialpulse.app.report.application.usecase.GetReportsUseCase;
import com.socialpulse.app.report.application.usecase.ReviewReportUseCase;
import com.socialpulse.app.report.application.usecase.UpdateReportStatusUseCase;
import com.socialpulse.app.report.domain.enums.ReportStatus;
import com.socialpulse.app.report.domain.model.Report;
import com.socialpulse.app.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Report API", description = "API for submitting reports on posts or comments")
@Validated
public class ReportController {

    private final CreateReportUseCase createReportUseCase;
    private final GetReportsUseCase getReportsUseCase;
    private final GetReportDetailUseCase getReportDetailUseCase;
    private final UpdateReportStatusUseCase updateReportStatusUseCase;
    private final ReviewReportUseCase reviewReportUseCase;
    private final ReportMapper reportMapper;
    private final ReportResponseEnricher reportResponseEnricher;

    public ReportController(CreateReportUseCase createReportUseCase,
                            GetReportsUseCase getReportsUseCase,
                            GetReportDetailUseCase getReportDetailUseCase,
                            UpdateReportStatusUseCase updateReportStatusUseCase,
                            ReviewReportUseCase reviewReportUseCase,
                            ReportMapper reportMapper,
                            ReportResponseEnricher reportResponseEnricher) {
        this.createReportUseCase = createReportUseCase;
        this.getReportsUseCase = getReportsUseCase;
        this.getReportDetailUseCase = getReportDetailUseCase;
        this.updateReportStatusUseCase = updateReportStatusUseCase;
        this.reviewReportUseCase = reviewReportUseCase;
        this.reportMapper = reportMapper;
        this.reportResponseEnricher = reportResponseEnricher;
    }

    @PostMapping
    @RequiresPermission.ReportCreate
    @Operation(summary = "Submit a report", description = "User submits a report on a post, comment, or user")
    public ResponseEntity<ApiResponse<ReportResponse>> submitReport(
        @RequestBody @Valid CreateReportRequest request,
        @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        Long currentUserId = currentUser.getId();
        Report savedReport = createReportUseCase.createReport(currentUserId, request);
        ReportResponse response = reportMapper.toResponse(savedReport);
        response = reportResponseEnricher.enrich(response);
        return ResponseEntity.ok(ApiResponse.<ReportResponse>builder().data(response).build());
    }

    @GetMapping
    @RequiresPermission.ReportManage
    @Operation(summary = "Get all reports", description = "Admin retrieves paginated list of reports with target content")
    public ResponseEntity<ApiResponse<PageResponse<ReportResponse>>> getReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") @Max(100) int size) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<ReportResponse>>builder()
                .data(getReportsUseCase.getReports(status, page, size))
                .build());
    }

    @GetMapping("/{reportId}")
    @RequiresPermission.ReportManage
    @Operation(summary = "Get report detail",
               description = "Admin retrieves a single report with the full target content (post/comment/user)")
    public ResponseEntity<ApiResponse<ReportResponse>> getReportDetail(
            @PathVariable Long reportId) {
        ReportResponse response = getReportDetailUseCase.getReportDetail(reportId);
        return ResponseEntity.ok(ApiResponse.<ReportResponse>builder().data(response).build());
    }

    @PostMapping("/{reportId}/review")
    @RequiresPermission.ReportManage
    @Operation(summary = "Review and moderate a report",
               description = "Admin performs a moderation action: REJECT, DELETE_CONTENT, BAN_USER, or DELETE_CONTENT_AND_BAN_USER")
    public ResponseEntity<ApiResponse<ReportResponse>> reviewReport(
            @PathVariable Long reportId,
            @RequestBody @Valid ReviewReportRequest request) {
        ReportResponse response = reviewReportUseCase.reviewReport(reportId, request);
        return ResponseEntity.ok(ApiResponse.<ReportResponse>builder()
                .data(response)
                .message("Report reviewed successfully")
                .build());
    }

    @PatchMapping("/{reportId}/status")
    @RequiresPermission.ReportManage
    @Operation(summary = "Update report status", description = "Admin manually updates the status of a report")
    public ResponseEntity<ApiResponse<ReportResponse>> updateReportStatus(
            @PathVariable Long reportId,
            @RequestBody @Valid UpdateReportStatusRequest request) {
        Report updatedReport = updateReportStatusUseCase.updateStatus(reportId, request);
        ReportResponse response = reportMapper.toResponse(updatedReport);
        response = reportResponseEnricher.enrich(response);
        return ResponseEntity.ok(ApiResponse.<ReportResponse>builder()
                .data(response)
                .build());
    }
}
