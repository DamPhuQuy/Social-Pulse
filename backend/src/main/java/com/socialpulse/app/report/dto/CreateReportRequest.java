package com.socialpulse.app.report.dto;

import com.socialpulse.app.report.enums.ReportTargetType;

import lombok.Data;

@Data
public class CreateReportRequest {
    private ReportTargetType targetType;
    private Long targetId;
    private String reason;
}