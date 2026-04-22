package com.socialpulse.app.report.application.dto.response;

import java.time.LocalDateTime;

import com.socialpulse.app.report.domain.enums.ReportStatus;
import com.socialpulse.app.report.domain.enums.ReportTargetType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {
    private Long id;
    private Long reporterId;
    private ReportTargetType targetType;
    private Long targetId;
    private String reason;
    private ReportStatus status;
    private LocalDateTime createdAt;
}
